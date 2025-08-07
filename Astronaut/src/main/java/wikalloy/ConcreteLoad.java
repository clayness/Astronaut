package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcreteLoad {

    private final Set<ConcreteRow> rows = new HashSet<>();

    public Collection<String> getInsertQueries() {
        return rows.stream().map(r -> "INSERT INTO `%s` (%s) VALUES (%s);".formatted(r.table,
                        r.columns.stream().map(v -> "`" + v + "`").collect(Collectors.joining(",")),
                        r.values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","))))
                .toList();
    }

    public static void main(String[] args) {
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        // get the arguments
        var oodmPath = Path.of(args[0]);
        var numInstances = args.length < 2 ? 10 : Integer.parseInt(args[1]);
        // get the object model
        var it = new AlloySolutionIterator(oodmPath);
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No solution found!");
        }
        var kkdm = it.next();
        var oodm = new ObjectModelGenerator(kkdm).getObjectModel();
        // create a new abstract load generator from the object model
        var gen = new AbstractLoadGenerator(kkdm, oodm);
        var instances = gen.generateLoad(numInstances);
        // get the instances from the "map" model
        var mapPath = oodmPath.resolveSibling("map." + oodmPath.getFileName().toString());
        var mt = new AlloySolutionIterator(mapPath);
        int i = 0;
        while (mt.hasNext() && i < 10) {
            var sol1 = mt.next();
            var clg = new ConcreteLoad.Factory(sol1, oodm);
            System.out.println("-------------------------------------------------------------");
            System.out.printf("-------------------- OBJECT MODEL #%05d --------------------%n", (++i));
            System.out.println(clg.getDDL());
            var cl = clg.create(instances);
            System.out.println("-------------------    INSERT  LOAD    ----------------------");
            System.out.println(String.join("\n", cl.getInsertQueries()));
            System.out.println();
        }
    }

    public void addRow(Object table, Collection<Object> columns, Collection<Object> values) {
        this.rows.add(new ConcreteRow(table, columns, values));
    }

    public record ConcreteRow(Object table, Collection<Object> columns, Collection<Object> values) {
        /* no-op */
    }

    public static class Factory extends AbstractKodkodGenerator {

        private final ObjectModel oodm;
        private final Map<Object, Set<ObjectModel.ObjField>> fieldmap;

        public Factory(A4Solution solution, ObjectModel oodm) {
            super(solution);
            // store the data model
            this.oodm = oodm;
            // extract the mapping of classes to fields
            var clsmap = oodm.getClasses().stream()
                    .collect(Collectors.toMap(ObjectModel.ObjClass::getName, this::getClassFields));
            // merge all the fields with the same table
            fieldmap = clsmap.entrySet().stream()
                    .collect(Collectors.groupingBy(e -> this.getClassTable(this.getAtom("x/" + e.getKey())),
                            Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toSet())));
            // get the tables for the associations, too
            for (var objAssoc : oodm.getAssociations()) {
                for (var am : this.getAssocFields(objAssoc).entrySet()) {
                    fieldmap.merge(am.getKey(), am.getValue(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
                }
            }
        }

        private String getDDL() {
            StringBuilder sb = new StringBuilder();
            for (var grp : fieldmap.entrySet()) {
                sb.append(createTableQuery(grp.getKey(), grp.getValue()));
                sb.append("\n");
            }
            return sb.toString();
        }

        private String createTableQuery(Object table, Collection<ObjectModel.ObjField> columns) {
            return "CREATE TABLE `%s` (%n   %s,%n   PRIMARY KEY (%s)%n);".formatted(table,
                    columns.stream().map(f -> "`%s` %s%s".formatted(
                                    f.name(), this.getSqlType(f.type()), f.isKey() ? " NOT NULL DEFAULT -1" : ""))
                            .collect(Collectors.joining(",\n   ")),
                    columns.stream().filter(ObjectModel.ObjField::isKey)
                            .map(f -> "`" + f.name() + "`")
                            .collect(Collectors.joining(", ")));
        }

        private String getSqlType(Object type) {
            var t = getTuple(type);
            if /*--*/ (this.in("oodm/TBool", t)) {
                return "BOOLEAN";
            } else if (this.in("oodm/TInt", t)) {
                return "INTEGER";
            } else if (this.in("oodm/TFloat", t)) {
                return "FLOAT";
            } else if (this.in("oodm/TString", t)) {
                return "VARCHAR(63)";
            } else if (this.in("oodm/TDate", t)) {
                return "DATETIME";
            } else if (this.in("oodm/TBlob", t)) {
                return "BLOB";
            } else {
                throw new IllegalArgumentException("Could not determine field type for atom: " + type);
            }
        }

        private String createInsertQuery(Object table, Collection<ObjectModel.ObjField> columns, Collection<Object> values) {
            return "INSERT INTO `%s` (%s) VALUES (%s);".formatted(table,
                    columns.stream().map(v -> "`" + v.name() + "`").collect(Collectors.joining(",")),
                    values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(",")));
        }

        public ConcreteLoad create(AbstractLoad abstractLoad) {
            var cl = new ConcreteLoad();
            // for each abstract load, we need to convert to a concrete set of insert statements
            // 1. start with the associations. if this is a merge table, we'll be inserting values
            //    into the fields for both src and dst into the same table
            var insts = new HashSet<>(abstractLoad.getInstances());
            var assocs = new HashSet<>(abstractLoad.getAssociations());
            for (var assoc : assocs) {
                if (isMTA(oodm.getAssociation(assoc.name()))) {
                    // if this assoc is a "merge table" association, the src and dst instances
                    // are both in the same table
                    var t = getClassTable(getAtom("x/" + assoc.src().getType()));
                    var e = Stream.of(assoc.src(), assoc.dst())
                            .map(AbstractLoad.AbstractInst::entrySet)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                    addConcreteRow(cl, t, List.of(assoc.src().getType(), assoc.dst().getType()), e);
                }
                if (isFKE(oodm.getAssociation(assoc.name()))) {
                    // if this is a "foreign key" association, the src and dst need to go in
                    // their main tables, but the dst gets the keys from the src, too

                    // add a row for the source table
                    var st = getClassTable(getAtom("x/" + assoc.src().getType()));
                    var se = assoc.src().entrySet();
                    addConcreteRow(cl, st, List.of(assoc.src().getType()), se);

                    // add a row for the dest table, including the keys from the source type
                    var dt = getClassTable(getAtom("x/" + assoc.dst().getType()));
                    var de = new HashSet<>(assoc.dst().entrySet());
                    var sk = getAllKeys(oodm.getClass(assoc.src().getType())).stream()
                            .map(ObjectModel.ObjField::name)
                            .collect(Collectors.toSet());
                    de.addAll(se.stream().filter(e -> sk.contains(e.getKey())).collect(Collectors.toSet()));
                    addConcreteRow(cl, dt, List.of(assoc.dst().getType()), de);
                }
                if (isOAT(oodm.getAssociation(assoc.name()))) {
                    // add a row for the source table
                    var st = getClassTable(getAtom("x/" + assoc.src().getType()));
                    var se = assoc.src().entrySet();
                    addConcreteRow(cl, st, List.of(assoc.src().getType()), se);

                    // add a row for the source table
                    var dt = getClassTable(getAtom("x/" + assoc.dst().getType()));
                    var de = assoc.dst().entrySet();
                    addConcreteRow(cl, dt, List.of(assoc.dst().getType()), de);

                    // add the association
                    var at = getAssocTable(getAtom("x/" + assoc.name()));
                    var ak = Stream.of(assoc.src(), assoc.dst())
                            .map(AbstractLoad.AbstractInst::getType)
                            .map(oodm::getClass)
                            .map(this::getAllKeys)
                            .flatMap(Collection::stream)
                            .map(ObjectModel.ObjField::name)
                            .collect(Collectors.toSet());
                    var ae = Stream.of(assoc.src(), assoc.dst())
                            .map(AbstractLoad.AbstractInst::entrySet)
                            .flatMap(Collection::stream)
                            .filter(e -> ak.contains(e.getKey()))
                            .collect(Collectors.toSet());
                    addConcreteRow(cl, at, List.of(), ae);
                }
                // remove the src / dst so they don't get added independently
                insts.removeAll(List.of(assoc.src(), assoc.dst()));
            }
            // add the leftover instances
            for (var inst : insts) {
                var it = getClassTable(getAtom("x/" + inst.getType()));
                var ie = inst.entrySet();
                addConcreteRow(cl, it, List.of(inst.getType()), ie);
            }
            return cl;
        }

        private Collection<ObjectModel.ObjField> getAllKeys(ObjectModel.ObjClass objCls) {
            // get the keys for this class and all its parents
            var k = objCls.getKeys();
            var p = objCls.getParent();
            if (p != null) {
                k.addAll(getAllKeys(p));
            }
            return k;
        }

        private void addConcreteRow(ConcreteLoad cl, Object table, Collection<Object> types, Collection<Map.Entry<Object, Object>> entries) {
            for (var type : types) {
                var oc = oodm.getClass(type);
                if (isCRI(oc)) {
                    // if this is a "class relation" inheritance type, then there is a different
                    // table containing the fields from the parent, so we'll need to insert that
                    var pc = oc.getParent().getName();
                    var pt = getClassTable(this.getAtom("x/" + pc));
                    addConcreteRow(cl, pt, List.of(pc), entries);
                }
            }
            // get the fields for this table
            var tf = fieldmap.get(table).stream()
                    .map(ObjectModel.ObjField::name)
                    .collect(Collectors.toSet());
            var cols = new ArrayList<>();
            var vals = new ArrayList<>();
            entries.stream().filter(f -> tf.contains(f.getKey()))
                    .forEach(f -> {
                        cols.add(f.getKey());
                        vals.add(f.getValue());
                    });
            cl.addRow(table, cols, vals);
        }

        private Map<Object, Set<ObjectModel.ObjField>> getAssocFields(ObjectModel.ObjAssoc objAssoc) {
            if (this.isOAT(objAssoc)) {
                // there is a table just for this assoc that has the
                // keys from both source and dest in it
                return Map.of(this.getAssocTable(this.getAtom("x/" + objAssoc.name())),
                        Stream.of(objAssoc.dst(), objAssoc.src())
                                .map(this::getAllKeys)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toSet()));
            } else {
                // the dst table must include the src keys
                return Map.of(this.getClassTable(this.getAtom("x/" + objAssoc.dst().getName())),
                        getAllKeys(objAssoc.src()).stream()
                                .map(f -> new ObjectModel.ObjField(f.name(), f.type(), false))
                                .collect(Collectors.toSet()));
            }
        }

        private Set<ObjectModel.ObjField> getClassFields(ObjectModel.ObjClass objCls) {
            // get the fields from this class, since those certainly go
            var fields = new HashSet<>(objCls.getFields());
            // get any fields from the parent
            if (objCls.getParent() != null) {
                if (this.isSRI(objCls) || this.isCCR(objCls)) {
                    // if this is an SRI or CCR inheritance situation, we need to add
                    // all the fields from the parent, too
                    fields.addAll(this.getClassFields(objCls.getParent()));
                } else {
                    // otherwise, add only the keys from the parent
                    fields.addAll(getAllKeys(objCls.getParent()));
                }
            }
            return fields;
        }

        private Object getAssocTable(Object assoc) {
            return this.join("orm/orm.map", assoc, 1)
                    .map(t -> t.atom(0))
                    .findFirst().orElseThrow();
        }

        private Object getClassTable(Object type) {
            return this.join("orm/orm.main", type, 0)
                    .map(t -> t.atom(1))
                    .findFirst().orElseThrow();
        }

        private boolean isSRI(ObjectModel.ObjClass objCls) {
            return this.join("inheritance_strategies/IStrat.SRIs", this.getAtom("x/" + objCls.getName()), 0)
                    .findFirst().isPresent();
        }

        private boolean isCRI(ObjectModel.ObjClass objCls) {
            return this.join("inheritance_strategies/IStrat.CRs", this.getAtom("x/" + objCls.getName()), 0)
                    .findFirst().isPresent();
        }

        private boolean isCCR(ObjectModel.ObjClass objCls) {
            return this.join("inheritance_strategies/IStrat.CCRs", this.getAtom("x/" + objCls.getName()), 0)
                    .findFirst().isPresent();
        }

        private boolean isFKE(ObjectModel.ObjAssoc objAssoc) {
            return this.join("association_strategies/AStrat.FKEs", this.getAtom("x/" + objAssoc.name()), 0)
                    .findFirst().isPresent();
        }

        private boolean isOAT(ObjectModel.ObjAssoc objAssoc) {
            return this.join("association_strategies/AStrat.OATs", this.getAtom("x/" + objAssoc.name()), 0)
                    .findFirst().isPresent();
        }

        private boolean isMTA(ObjectModel.ObjAssoc objAssoc) {
            return this.join("association_strategies/AStrat.MTs", this.getAtom("x/" + objAssoc.name()), 0)
                    .findFirst().isPresent();
        }
    }
}
