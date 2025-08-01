package wikalloy;

import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.ast.LeafExpression;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaGenerator {

    private final Instance instance;

    public SchemaGenerator(A4Solution sol) {
        assert sol.satisfiable();
        this.instance = sol.debugExtractKInstance();
    }

    public static void main(String[] args) {
        var solIter = new AlloySolutionIterator(Path.of(args[0]));
        for (int i = 0; solIter.hasNext(); i++) {
            System.out.printf("---- DDL FOR SOLUTION %05d ----%n", i);
            var gs = new SchemaGenerator(solIter.next());
            System.out.println(gs.generateSchema());
            System.out.println();
        }
    }

    public String createTable(Set<Object> mapping) {
        var name = new HashSet<String>();
        var cols = new HashMap<String, String>();
        var keys = new HashSet<String>();
        for (var o : mapping) {
            name.add(this.getRelationName(o));
            var k = this.getKeys(o);
            var c = this.getColumns(o);
            var a = this.getAssociationKeys(o);
            var f = this.getForeignKeys(o);
            var p = this.getParentFields(o);
            // add the keys for any tables
            keys.addAll(this.getKeys(o));
            // add the keys for the associations
            keys.addAll(a.keySet());
            // add the association keys as columns
            c.putAll(a);
            // add the foreign keys as columns
            c.putAll(f);
            // add the parent fields as columns
            c.putAll(p);
            // make sure the keys are all in the columns set
            // and they are all not null
            for (var kvp : c.entrySet()) {
                if (k.contains(kvp.getKey())) {
                    cols.put(kvp.getKey(), "%s NOT NULL".formatted(kvp.getValue()));
                } else {
                    cols.put(kvp.getKey(), kvp.getValue());
                }
            }
        }
        return "CREATE TABLE tbl_%s (%n    %s,%n    PRIMARY KEY (%s)%n  );".formatted(
                String.join("_", name),
                cols.entrySet().stream()
                        .map(kvp -> "%s %s".formatted(kvp.getKey(), kvp.getValue()))
                        .collect(Collectors.joining(",\n    ")),
                String.join(",", keys));
    }

    public String generateSchema() {
        var ddl = new StringBuilder();
        // the association strategies are in:
        //   - "association_strategies/AStrat.MTs"  for "Merge Table"            (src & dst in same table, union key)
        //   - "association_strategies/AStrat.FKEs" for "Foreign Key Embedding"  (src & dst in different tables, key of src in dst table)
        //   - "association_strategies/AStrat.OATs" for "Own Association Table"  (src & dst in different tables, table of keys only for assoc)
        ddl.append("-- MTs : %s%n".formatted(this.getAssociationMappings("MTs")));
        ddl.append("-- FKEs: %s%n".formatted(this.getAssociationMappings("FKEs")));
        ddl.append("-- OATs: %s%n".formatted(this.getAssociationMappings("OATs")));
        // the inheritance strategies are in :
        //   - "inheritance_strategies/IStrat.SRIs" for "Single Relation Inheritance" (parent & child in same table, union key)
        //   - "inheritance_strategies/IStrat.CRs"  for "Class Relation"              (parent & child in different tables, parent key in child)
        //   - "inheritance_strategies/IStrat.CCRs" for "Concrete Class Relation"     (parent & child in different tables, all parent fields in child)
        ddl.append("-- SRIs: %s%n".formatted(this.getInheritanceMappings("SRIs")));
        ddl.append("-- CRs : %s%n".formatted(this.getInheritanceMappings("CRs")));
        ddl.append("-- CCRs: %s%n".formatted(this.getInheritanceMappings("CCRs")));
        // get the table creation script
        ddl.append(this.instance.tuples("orm/orm.map").stream()
                .collect(Collectors.groupingBy(t -> t.atom(0),
                        Collectors.mapping(t -> t.atom(1), Collectors.toSet())))
                .values().stream().map(this::createTable)
                .collect(Collectors.joining(System.lineSeparator())));
        return ddl.toString();
    }

    private Map<String, String> getAssociationKeys(Object association) {
        return getDataTypes(getKeysForClasses(Stream.of("oodm/Association.src", "oodm/Association.dst")
                .flatMap(this::getTuples)
                .filter(x -> x.atom(0) == association)
                .map(x -> x.atom(1))
                .collect(Collectors.toSet())), true);
    }

    private String getAssociationMappings(String t) {
        return this.getTuples("association_strategies/AStrat.%s".formatted(t))
                .map(x -> x.atom(0))
                .map(this::getRelationName)
                .collect(Collectors.joining(","));
    }

    private Map<String, String> getColumns(final Object o) {
        return this.join("oodm/Class.fields", o, 0)
                .collect(Collectors.toMap(
                        x -> this.getRelationName(x.atom(1)),
                        x -> this.getTypeDef(x.atom(2))));
    }

    private Map<String, String> getDataTypes(Set<Object> fields, boolean keys) {
        return fields.stream()
                .flatMap(f -> join("oodm/Class.fields", f, 1))
                .collect(Collectors.toMap(
                        x -> this.getRelationName(x.atom(1)),
                        x -> "%s%s".formatted(this.getTypeDef(x.atom(2)), keys ? " NOT NULL" : "")));
    }

    private Map<String, String> getForeignKeys(Object cls) {
        // if this class is the "dst" of an FKE association, we need to
        // add columns for all the keys of the "src" of that same association
        return getDataTypes(getKeysForClasses(this.join("oodm/Association.dst", cls, 1)
                .filter(dst -> this.in("association_strategies/AStrat.FKEs", this.getTuple(dst.atom(0))))
                .map(dst -> dst.atom(0))
                .flatMap(assoc -> this.join("oodm/Association.src", assoc, 0))
                .map(src -> src.atom(1))
                .collect(Collectors.toSet())), true);
    }

    private String getInheritanceMappings(String t) {
        return this.getTuples("inheritance_strategies/IStrat.%s".formatted(t))
                .map(x -> x.atom(0))
                .map(this::getRelationName)
                .collect(Collectors.joining(","));
    }

    private Set<String> getKeys(final Object o) {
        return this.join("oodm/Class.key", o, 0)
                .map(x -> x.atom(1))
                .map(this::getRelationName)
                .collect(Collectors.toSet());
    }

    private Set<Object> getKeysForClasses(Set<Object> cls) {
        return cls.stream()
                .flatMap(cl -> join("oodm/Class.key", cl, 0))
                .map(cl -> cl.atom(1))
                .collect(Collectors.toSet());
    }

    private Map<String, String> getParentFields(Object cls) {
        // if this class is in a "CCR" inheritance relation, we need to
        // add all the columns from the parent to this table
        if (this.in("inheritance_strategies/IStrat.CCRs", getTuple(cls))) {
            // get the parent
            return this.join("oodm/Class.parent", cls, 0)
                    .flatMap(p -> this.join("oodm/Class.fields", p, 1))
                    .collect(Collectors.toMap(
                            x -> this.getRelationName(x.atom(1)),
                            x -> this.getTypeDef(x.atom(2))));
        } else {
            return Map.of();
        }
    }

    private String getRelationName(Object o) {
        var t = getTuple(o);
        return Path.of(this.instance.relations().stream()
                        .filter(r -> r.arity() == 1)
                        .filter(r -> this.instance.tuples(r).contains(t))
                        .map(LeafExpression::name)
                        .findFirst()
                        .orElseThrow())
                .getFileName().toString();
    }

    private Tuple getTuple(Object atom) {
        return this.instance.universe().factory().tuple(atom);
    }

    private Stream<Tuple> getTuples(String relationName) {
        var r = this.instance.findRelationByName(relationName);
        return Optional.ofNullable(this.instance.tuples(r))
                .orElse(this.instance.universe().factory().noneOf(r.arity()))
                .stream();
    }

    private String getTypeDef(Object o) {
        var t = getTuple(o);
        if (this.in("oodm/TBool", t)) {
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
            throw new IllegalArgumentException("Could not determine field type for atom: " + o);
        }
    }

    private boolean in(String relation, Tuple t) {
        var r = this.instance.findRelationByName(relation);
        if (r == null) {
            return false;
        } else {
            return Optional.ofNullable(this.instance.tuples(r))
                    .orElse(this.instance.universe().factory().noneOf(r.arity()))
                    .contains(t);
        }
    }

    private Stream<Tuple> join(String relationName, Object atom, int i) {
        return getTuples(relationName).filter(t -> t.atom(i) == atom);
    }
}
