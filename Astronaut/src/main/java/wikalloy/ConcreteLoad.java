package wikalloy;

import edu.mit.csail.sdg.translator.A4Solution;
import picocli.CommandLine;
import wikalloy.kodkod.KodkodAtom;
import wikalloy.kodkod.KodkodInstance;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConcreteLoad {

    private final Set<ConcreteRow> rows = new HashSet<>();

    public void addRow(Object table, Collection<Object> columns, Collection<Object> values) {
        this.rows.add(new ConcreteRow(table, columns, values));
    }

    public Collection<String> getInsertQueries() {
        return rows.stream().map(r -> "INSERT INTO `%s` (%s) VALUES (%s);".formatted(r.table,
                        r.columns.stream().map(v -> "`" + v + "`").collect(Collectors.joining(",")),
                        r.values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","))))
                .toList();
    }

    public record ConcreteRow(Object table, Collection<Object> columns, Collection<Object> values) {
        /* no-op */
    }

    public static class Runner implements Callable<Integer> {

        @CommandLine.Parameters(index = "0", description = "The path to the OODM Alloy specification file.")
        private Path oodmPath;

        @CommandLine.Option(names = {"-l", "--num-loads"}, description = "The number of loads to generate.")
        private int numLoads = 10;

        @CommandLine.Option(names = {"-i", "--num-instances"}, description = "The maximum number of instances per abstract class.")
        private int numInstances = 100;

        @CommandLine.Option(names = {"-m", "--num-models"}, description = "The maximum number of models to generate.")
        private int numModels = Integer.MAX_VALUE;

        @CommandLine.Option(names = {"-o", "--output"}, description = "The path to the root output directory.")
        private Path outputPath = Paths.get("", "out");

        public static void main(String[] args) {
            System.exit(new CommandLine(new Runner()).execute(args));
        }

        @Override
        public Integer call() throws Exception {
            // create the output path
            var runOutput = outputPath.resolve(Instant.now().toString().replaceAll("\\D", ""));
            runOutput.toFile().mkdirs();
            // get the object model
            var it = new AlloySolutionIterator(oodmPath);
            if (!it.hasNext()) {
                throw new IllegalArgumentException("No solution found!");
            }
            var kkdm = it.next();
            var oodm = new ObjectModel.Factory(kkdm).create();
            // create a new abstract load generator from the object model
            var alg = new AbstractLoad.Factory(kkdm, oodm);
            var als = IntStream.range(0, numLoads).mapToObj(i -> alg.create(numInstances)).toArray(AbstractLoad[]::new);
            // get the instances from the "map" model
            var mapPath = oodmPath.resolveSibling("map." + oodmPath.getFileName().toString());
            var mt = new AlloySolutionIterator(mapPath);
            for (int j = 0; mt.hasNext() && j < numModels; j++) {
                var sol1 = mt.next();
                try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d.xml".formatted(j)).toAbsolutePath().toFile())) {
                    sol1.writeXML(pw, null, null);
                }
                var clg = new ConcreteLoad.Factory(sol1, oodm);
                try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d_CREATE.sql".formatted(j)).toAbsolutePath().toFile())) {
                    pw.println("/*-------------------------------------------------------------*/");
                    pw.printf("/*-------------------- OBJECT MODEL #%05d --------------------*/%n", j);
                    pw.println(String.join("\n", clg.getCreateQueries()));
                }
                for (int i = 0; i < numLoads; i++) {
                    var cl = clg.create(als[i]);
                    try (var pw = new PrintWriter(runOutput.resolve("MODL_%05d_LOAD_%05d_INSERT.sql".formatted(j, i)).toAbsolutePath().toFile())) {
                        pw.println("/*-------------------    INSERT  LOAD    ----------------------*/");
                        pw.println(String.join("\n", cl.getInsertQueries()));
                    }
                }
            }
            return 0;
        }
    }

    public static class Factory extends KodkodInstance {

        private final ObjectModel oodm;
        private final Map<Object, Set<ObjectModel.ObjField>> fieldmap;

        public Factory(A4Solution solution, ObjectModel oodm) {
            super(solution);
            // store the data model
            this.oodm = oodm;
            // extract the mapping of classes to fields
            var clsmap = oodm.getClasses().stream()
                    .collect(Collectors.toMap(KodkodAtom::getAtom, this::getClassFields));
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

        public ConcreteLoad create(AbstractLoad abstractLoad) {
            var cl = new ConcreteLoad();
            // for each abstract load, we need to convert to a concrete set of insert statements
            // 1. start with the associations. if this is a merge table, we'll be inserting values
            //    into the fields for both src and dst into the same table
            var insts = new HashSet<>(abstractLoad.getInstances());
            var assocs = new HashSet<>(abstractLoad.getAssociations());
            for (var assoc : assocs) {
                if (isAStrat(oodm.getAssociation(assoc.getAtom()), "MT")) {
                    // if this assoc is a "merge table" association, the src and dst instances
                    // are both in the same table
                    var t = getClassTable(getAtom("x/" + assoc.src().getAtom()));
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
                    var st = getClassTable(getAtom("x/" + assoc.src().getAtom()));
                    var se = assoc.src().entrySet();
                    addConcreteRow(cl, st, List.of(assoc.src().getAtom()), se);

                    // add a row for the dest table, including the keys from the source type
                    var dt = getClassTable(getAtom("x/" + assoc.dst().getAtom()));
                    var de = new HashSet<>(assoc.dst().entrySet());
                    var sk = getAllKeys(oodm.getClass(assoc.src().getAtom())).stream()
                            .map(KodkodAtom::getAtom)
                            .collect(Collectors.toSet());
                    de.addAll(se.stream().filter(e -> sk.contains(e.getKey())).collect(Collectors.toSet()));
                    addConcreteRow(cl, dt, List.of(assoc.dst().getAtom()), de);
                }
                if (isAStrat(oodm.getAssociation(assoc.getAtom()), "OAT")) {
                    // add a row for the source table
                    var st = getClassTable(getAtom("x/" + assoc.src().getAtom()));
                    var se = assoc.src().entrySet();
                    addConcreteRow(cl, st, List.of(assoc.src().getAtom()), se);

                    // add a row for the source table
                    var dt = getClassTable(getAtom("x/" + assoc.dst().getAtom()));
                    var de = assoc.dst().entrySet();
                    addConcreteRow(cl, dt, List.of(assoc.dst().getAtom()), de);

                    // add the association
                    var at = getAssocTable(getAtom("x/" + assoc.getAtom()));
                    var ak = Stream.of(assoc.src(), assoc.dst())
                            .map(abstractInst -> abstractInst.getAtom())
                            .map(oodm::getClass)
                            .map(this::getAllKeys)
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
                var it = getClassTable(getAtom("x/" + inst.getAtom()));
                var ie = inst.entrySet();
                addConcreteRow(cl, it, List.of(inst.getAtom()), ie);
            }
            return cl;
        }

        private Collection<String> getCreateQueries() {
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
                    var pt = getClassTable(this.getAtom("x/" + pc));
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
            cl.addRow(table, cols, vals);
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

        private Collection<ObjectModel.ObjField> getAllKeys(ObjectModel.ObjClass objCls) {
            // get the keys for this class and all its parents
            var k = objCls.getKeys();
            var p = objCls.getParent();
            if (p != null) {
                k.addAll(getAllKeys(p));
            }
            return k;
        }

        private Map<Object, Set<ObjectModel.ObjField>> getAssocFields(ObjectModel.ObjAssoc objAssoc) {
            if (this.isAStrat(objAssoc, "OAT")) {
                // there is a table just for this assoc that has the
                // keys from both source and dest in it
                return Map.of(this.getAssocTable(this.getAtom("x/" + objAssoc.getAtom())),
                        Stream.of(objAssoc.dst(), objAssoc.src())
                                .map(this::getAllKeys)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toSet()));
            } else {
                // the dst table must include the src keys
                return Map.of(this.getClassTable(this.getAtom("x/" + objAssoc.dst().getAtom())),
                        getAllKeys(objAssoc.src()).stream()
                                .map(f -> new ObjectModel.ObjField(f.getAtom(), f.getDataType(), false))
                                .collect(Collectors.toSet()));
            }
        }

        private Object getAssocTable(Object assoc) {
            return this.join("orm/orm.map", assoc, 1)
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
                    fields.addAll(getAllKeys(objCls.getParent()));
                }
            }
            return fields;
        }

        private Object getClassTable(Object type) {
            return this.join("orm/orm.main", type, 0)
                    .map(t -> t.atom(1))
                    .findFirst().orElseThrow();
        }

        private String getSqlType(Object type) {
            //@formatter:off
            return switch (type.toString()) {
                case "oodm/TBool"   -> "BOOLEAN";
                case "oodm/TInt"    -> "INTEGER";
                case "oodm/TFloat"  -> "FLOAT";
                case "oodm/TString" -> "VARCHAR(63)";
                case "oodm/TDate"   -> "DATETIME";
                case "oodm/TBlob"   -> "BLOB";
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
}
