package wikalloy.concrete;

import edu.mit.csail.sdg.translator.A4Solution;
import wikalloy.ObjectModel;
import wikalloy.generic.AbstractLoad;
import wikalloy.kodkod.KodkodAtom;
import wikalloy.kodkod.KodkodInstance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcreteLoadFactory extends KodkodInstance {

    private final Map<Object, Set<ObjectModel.ObjField>> fieldmap;
    private final ObjectModel oodm;

    public ConcreteLoadFactory(A4Solution solution, ObjectModel oodm) {
        super(solution);
        // store the data model
        this.oodm = oodm;
        // extract the mapping of classes to fields
        var clsmap = oodm.getClasses().stream()
                .collect(Collectors.toMap(KodkodAtom::getAtom, this::getClassFields));
        // merge all the fields with the same table
        fieldmap = clsmap.entrySet().stream()
                .collect(Collectors.groupingBy(e -> this.getClassTable(e.getKey()),
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

    public ConcreteLoad create(AbstractLoad al) {
        var cl = new ConcreteLoad();
        createInserts(al, cl);
        createSelects(al, cl);
        return cl;
    }

    private void createInserts(AbstractLoad al, ConcreteLoad cl) {
        // for each abstract load, we need to convert to a concrete set of insert statements
        // 1. start with the associations. if this is a merge table, we'll be inserting values
        //    into the fields for both src and dst into the same table
        var insts = new HashSet<>(al.getInstances());
        var assocs = new HashSet<>(al.getAssociations());
        for (var assoc : assocs) {
            if (isAStrat(oodm.getAssociation(assoc.getAtom()), "MT")) {
                // if this assoc is a "merge table" association, the src and dst instances
                // are both in the same table
                var t = getClassTable(assoc.src().getAtom());
                var e = Stream.of(assoc.src(), assoc.dst())
                        .map(AbstractLoad.AbstractInst::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                addConcreteRow(cl, t, List.of(assoc.src().getAtom(), assoc.dst().getAtom()), e);
            }
            if (isAStrat(oodm.getAssociation(assoc.getAtom()), "FKE")) {
                // if this is a "foreign key" association, the src and dst need to go in
                // their main tables, but the dst gets the keys from the src, too

                // add a row for the source table
                var st = getClassTable(assoc.src().getAtom());
                var se = assoc.src().entrySet();
                addConcreteRow(cl, st, List.of(assoc.src().getAtom()), se);

                // add a row for the dest table, including the keys from the source type
                var dt = getClassTable(assoc.dst().getAtom());
                var de = new HashSet<>(assoc.dst().entrySet());
                var sk = oodm.getClass(assoc.src().getAtom()).getAllKeys().stream()
                        .map(KodkodAtom::getAtom)
                        .collect(Collectors.toSet());
                de.addAll(se.stream().filter(e -> sk.contains(e.getKey())).collect(Collectors.toSet()));
                addConcreteRow(cl, dt, List.of(assoc.dst().getAtom()), de);
            }
            if (isAStrat(oodm.getAssociation(assoc.getAtom()), "OAT")) {
                // add a row for the source table
                var st = getClassTable(assoc.src().getAtom());
                var se = assoc.src().entrySet();
                addConcreteRow(cl, st, List.of(assoc.src().getAtom()), se);

                // add a row for the source table
                var dt = getClassTable(assoc.dst().getAtom());
                var de = assoc.dst().entrySet();
                addConcreteRow(cl, dt, List.of(assoc.dst().getAtom()), de);

                // add the association
                var at = getAssocTable(assoc.getAtom());
                var ak = Stream.of(assoc.src(), assoc.dst())
                        .map(KodkodAtom::getAtom)
                        .map(oodm::getClass)
                        .map(ObjectModel.ObjClass::getAllKeys)
                        .flatMap(Collection::stream)
                        .map(KodkodAtom::getAtom)
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
            var it = getClassTable(inst.getAtom());
            var ie = inst.entrySet();
            addConcreteRow(cl, it, List.of(inst.getAtom()), ie);
        }
    }

    private void createSelects(AbstractLoad al, ConcreteLoad cl) {
        // get the queries from the abstract load and create a select query based on it
        // for example, if the query is a "find by id" query (i.e., ElectronicProject.get(X)), we need
        // to synthesize a select query that will join all tables involved in the inheritance
        // hierarchy for that type, project to get all fields for the selected type, and filter
        // the query based on the passed id. E.g,
        //     select `Table$13`.`FProductId`,
        //            `Table$13`.`FSize`,
        //            `Table$9`.`FProductName`,
        //            `Table$9`.`FDescription`,
        //            `Table$9`.`FPrice`
        //     from `Table$13`
        //         inner join `Table$9$` on `Table$13`.`FProductId` = `Table$9`.`FProductId`
        //     where `Table$13`.`FProductId` = 'X'
        for (var aq : al.getQueries()) {
            // get the type of the object to get the projection and the
            // basic joins for those projections
            var objClass = oodm.getClass(aq.getAtom());
            var mainTable = this.getClassTable(aq.getAtom());
            // list of "joins": the keys are the table, and the values are the set of
            // key fields used in the "using" clause. for this, we assume the joins are
            // chained so that each key field is joined against the previously joined table
            var joins = new HashMap<Object, Map<Object, Object>>();
            var p = objClass.getParent();
            while (p != null) {
                var pt = this.getClassTable(p.getAtom());
                if (!pt.equals(mainTable)) {
                    var usings = new HashMap<>();
                    for (var k : p.getKeys()) {
                        usings.put(k.getAtom(), null);
                    }
                    joins.putIfAbsent(pt, usings);
                }
                p = p.getParent();
            }
            // get the projection
            var projection = this.getProjection(objClass);
            // join based on the association (if there is one)
            var assoc = aq.getAssociation();
            if (assoc != null) {
                if (isAStrat(assoc, "FKE")) {
                    // if this is a "foreign key" association, we need to join against the table based on the "src" key
                    var other = oodm.getClass((assoc.src().getAtom().equals(aq.getAtom()) ? assoc.dst() : assoc.src()).getAtom());
                    var usings = new HashMap<>();
                    for (var k : oodm.getClass(assoc.src().getAtom()).getKeys()) {
                        usings.put(k.getAtom(), null);
                    }
                    joins.putIfAbsent(this.getClassTable(other.getAtom()), usings);
                }
                if (isAStrat(assoc, "OAT")) {
                    // if there's a join table for this association, we need to join that table based on all the keys
                    var at = this.getAssocTable(assoc.getAtom());
                    joins.putIfAbsent(at,
                            Stream.concat(oodm.getClass(assoc.src().getAtom()).getKeys().stream(), oodm.getClass(assoc.dst().getAtom()).getKeys().stream())
                                    .map(KodkodAtom::getAtom)
                                    .collect(Collectors.toMap(Function.identity(), e -> at)));
                }
            }
            // get the filters (if there are any)
            var af = aq.getFilters();
            Collection<ConcreteLoad.ConcreteFilter> cf;
            if (af.isEmpty()) {
                cf = oodm.getClass(aq.getAtom()).getAllKeys().stream()
                        .map(k -> new ConcreteLoad.ConcreteFilter(k.getAtom(), "<>", -1))
                        .collect(Collectors.toSet());
            } else {
                cf = af.stream()
                        .map(f -> new ConcreteLoad.ConcreteFilter(f.getAtom(), f.getOperator(), f.getValue()))
                        .collect(Collectors.toSet());
            }
            // add the select statement to the concrete load
            cl.addSelect(mainTable, projection, joins.entrySet(), cf);
        }
    }

    public Collection<String> getCreateQueries() {
        return fieldmap.entrySet().stream()
                .map(grp -> createTableQuery(grp.getKey(), grp.getValue()))
                .toList();
    }

    private void addConcreteRow(ConcreteLoad cl, Object table, Collection<Object> types, Collection<Map.Entry<Object, Object>> entries) {
        for (var type : types) {
            var oc = oodm.getClass(type);
            if (isIStrat(oc, "CR")) {
                // if this is a "class relation" inheritance type, then there is a different
                // table containing the fields from the parent, so we'll need to insert that
                var pc = oc.getParent().getAtom();
                var pt = getClassTable(pc);
                addConcreteRow(cl, pt, List.of(pc), entries);
            }
        }
        // get the fields for this table
        var tf = fieldmap.get(table).stream()
                .map(KodkodAtom::getAtom)
                .collect(Collectors.toSet());
        var cols = new ArrayList<>();
        var vals = new ArrayList<>();
        entries.stream().filter(f -> tf.contains(f.getKey()))
                .forEach(f -> {
                    cols.add(f.getKey());
                    vals.add(f.getValue());
                });
        cl.addInsert(table, cols, vals);
    }

    private String createTableQuery(Object table, Collection<ObjectModel.ObjField> columns) {
        return "CREATE TABLE `%s` (%n   %s,%n   PRIMARY KEY (%s)%n);".formatted(table,
                columns.stream().map(f -> "`%s` %s%s".formatted(
                                f.getAtom(), this.getSqlType(f.getDataType()), f.isKey() ? " NOT NULL DEFAULT -1" : ""))
                        .collect(Collectors.joining(",\n   ")),
                columns.stream().filter(ObjectModel.ObjField::isKey)
                        .map(f -> "`" + f.getAtom() + "`")
                        .collect(Collectors.joining(", ")));
    }

    private Map<Object, Set<ObjectModel.ObjField>> getAssocFields(ObjectModel.ObjAssoc objAssoc) {
        if (this.isAStrat(objAssoc, "OAT")) {
            // there is a table just for this assoc that has the
            // keys from both source and dest in it
            return Map.of(this.getAssocTable(objAssoc.getAtom()),
                    Stream.of(objAssoc.dst(), objAssoc.src())
                            .map(ObjectModel.ObjClass::getAllKeys)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet()));
        } else {
            // the dst table must include the src keys
            return Map.of(this.getClassTable(objAssoc.dst().getAtom()),
                    objAssoc.src().getAllKeys().stream()
                            .map(f -> new ObjectModel.ObjField(f.getAtom(), f.getDataType(), false))
                            .collect(Collectors.toSet()));
        }
    }

    private Object getAssocTable(Object assoc) {
        var atom = this.getAtom("x/" + assoc);
        return this.join("orm/orm.map", atom, 1)
                .map(t -> t.atom(0))
                .findFirst().orElseThrow();
    }

    private Set<ObjectModel.ObjField> getClassFields(ObjectModel.ObjClass objCls) {
        // get the fields from this class, since those certainly go
        var fields = new HashSet<>(objCls.getFields());
        // get any fields from the parent
        if (objCls.getParent() != null) {
            if (this.isIStrat(objCls, "SRI") || this.isIStrat(objCls, "CCR")) {
                // if this is an SRI or CCR inheritance situation, we need to add
                // all the fields from the parent, too
                fields.addAll(this.getClassFields(objCls.getParent()));
            } else {
                // otherwise, add only the keys from the parent
                fields.addAll(objCls.getParent().getAllKeys());
            }
        }
        return fields;
    }

    private Map<Object, Object> getProjection(ObjectModel.ObjClass objCls) {
        var proj = new HashMap<>();
        for (var f : this.getClassFields(objCls)) {
            proj.put(f.getAtom(), this.getClassTable(objCls.getAtom()));
        }
        if (this.isIStrat(objCls, "CR")) {
            // if this is a "single relation" or a "concrete relation" situation, then all the
            // fields we need will be on the main table for the class
            var parentProj = this.getProjection(objCls.getParent());
            for (var kvp : parentProj.entrySet()) {
                proj.putIfAbsent(kvp.getKey(), kvp.getValue());
            }
            return proj;
        }
        return proj;
    }

    private Object getClassTable(Object type) {
        var atom = this.getAtom("x/" + type);
        return this.join("orm/orm.main", atom, 0)
                .map(t -> t.atom(1))
                .findFirst().orElseThrow();
    }

    private String getSqlType(Object type) {
        //@formatter:off
        return switch (type.toString()) {
            case "oodm/TBool$0"   -> "BOOLEAN";
            case "oodm/TInt$0"    -> "INTEGER";
            case "oodm/TFloat$0"  -> "FLOAT";
            case "oodm/TString$0" -> "VARCHAR(63)";
            case "oodm/TDate$0"   -> "DATETIME";
            case "oodm/TBlob$0"   -> "BLOB";
            default -> throw new IllegalArgumentException("Could not determine field type for atom: " + type);
        };
        //@formatter:on
    }

    private boolean isAStrat(KodkodAtom kkObj, String strat) {
        return inX(kkObj, "association_strategies/AStrat.%ss".formatted(strat));
    }

    private boolean isIStrat(KodkodAtom kkObj, String strat) {
        return inX(kkObj, "inheritance_strategies/IStrat.%ss".formatted(strat));
    }
}
