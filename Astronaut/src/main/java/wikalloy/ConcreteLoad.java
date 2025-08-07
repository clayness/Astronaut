package wikalloy;

import edu.mit.csail.sdg.translator.A4Solution;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcreteLoad extends AbstractKodkodGenerator {

    public static void main(String[] args) {
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
        while (mt.hasNext()) {
            var sol1 = mt.next();
            var clg = new ConcreteLoad.Factory(sol1, oodm);
            System.out.printf("--- OBJECT MODEL #%05d ---%n", (++i));
            System.out.println(clg.getDDL());
        }
    }

    public static class Factory extends AbstractKodkodGenerator {

        private final ObjectModel oodm;
        private final String ddl;

        public String getDDL() {
            return ddl;
        }

        public Factory(A4Solution solution, ObjectModel oodm) {
            super(solution);
            // store the data model
            this.oodm = oodm;
            // extract the mapping of classes to fields
            var clsmap = oodm.getClasses().stream()
                    .collect(Collectors.toMap(ObjectModel.ObjClass::getName, this::getClassFields));
            // merge all the fields with the same table
            var grps = clsmap.entrySet().stream()
                    .collect(Collectors.groupingBy(e -> this.getClassTable(this.getAtom("x/" + e.getKey())),
                            Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toSet())));
            // get the tables for the associations, too
            for (var objAssoc : oodm.getAssociations()) {
                for (var am : this.getAssocFields(objAssoc).entrySet()) {
                    grps.merge(am.getKey(), am.getValue(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
                }
            }
            StringBuilder sb = new StringBuilder();
            for (var grp : grps.entrySet()) {
                sb.append(createTableQuery(grp.getKey(), grp.getValue()));
                sb.append("\n");
            }
            this.ddl = sb.toString();
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
            return "INT";
        }

        private String createInsertQuery(Object table, Collection<ObjectModel.ObjField> columns, Collection<Object> values) {
            return "INSERT INTO `%s` (%s) VALUES (%s);".formatted(table,
                    columns.stream().map(v -> "`" + v.name() + "`").collect(Collectors.joining(",")),
                    values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(",")));
        }

        public ConcreteLoad create(A4Solution sol, AbstractLoad abstractLoad) {
            return null;
        }

        private Map<Object, Set<ObjectModel.ObjField>> getAssocFields(ObjectModel.ObjAssoc objAssoc) {
            if (this.isOAT(objAssoc)) {
                // there is a table just for this assoc that has the
                // keys from both source and dest in it
                return Map.of(this.getAssocTable(this.getAtom("x/" + objAssoc.name())),
                        Stream.of(objAssoc.dst(), objAssoc.src()).flatMap(c -> c.getKeys().stream())
                                .collect(Collectors.toSet()));
            } else {
                // the dst table must include the src keys
                return Map.of(this.getClassTable(this.getAtom("x/" + objAssoc.dst().getName())), objAssoc.src().getKeys());
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
                    fields.addAll(objCls.getParent().getKeys());
                }
            }
            return fields;
        }

        private Object getAssocTable(Object assoc) {
            return this.join("orm/orm.map", assoc, 1)
                    .map(t -> t.atom(1))
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
    }

    public ConcreteLoad(A4Solution solution, ObjectModel objectModel) {
        super(solution);
    }
}
