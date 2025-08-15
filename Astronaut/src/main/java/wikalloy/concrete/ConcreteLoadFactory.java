package wikalloy.concrete;

import edu.mit.csail.sdg.translator.A4Solution;
import wikalloy.objmodel.ObjAssoc;
import wikalloy.objmodel.ObjClass;
import wikalloy.objmodel.ObjField;
import wikalloy.objmodel.ObjectModel;
import wikalloy.generic.AbstractInst;
import wikalloy.generic.AbstractLoad;
import wikalloy.kodkod.KodkodAtom;
import wikalloy.kodkod.KodkodInstance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcreteLoadFactory extends KodkodInstance {

    private final Map<Object, Object> assocmap = new HashMap<>();
    private final Map<Object, Set<ObjField>> fieldmap;
    private final ObjectModel oodm;
    private final Map<Object, Object> tablemap = new HashMap<>();

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

    public ConcreteImpl create(Collection<AbstractLoad> loads) {
        return new ConcreteImpl(this.fieldmap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ConcreteImpl.ConcreteField::new)
                                .collect(Collectors.toSet()))),
                loads.stream()
                        .map(this::create)
                        .collect(Collectors.toList()));
    }

    public ConcreteLoad create(AbstractLoad al) {
        var cl = new ConcreteLoad(al.getUUID());
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
                        .map(AbstractInst::entrySet)
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
                        .map(ObjClass::getAllKeys)
                        .flatMap(Collection::stream)
                        .map(KodkodAtom::getAtom)
                        .collect(Collectors.toSet());
                var ae = Stream.of(assoc.src(), assoc.dst())
                        .map(AbstractInst::entrySet)
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
            // get the filters (if there are any) -- we get them early, because
            // we may need to attach them to the joined tables instead
            var af = new HashSet<>(aq.getFilters());
            var cf = new HashSet<ConcreteLoad.ConcreteFilter>();
            // join based on the association (if there is one)
            var assoc = aq.getAssociation();
            if (assoc != null) {
                var srcClass = oodm.getClass(assoc.src().getAtom());
                var dstClass = oodm.getClass(assoc.dst().getAtom());
                var tgtClass = assoc.src().getAtom().equals(aq.getAtom()) ? srcClass : dstClass;
                var othClass = assoc.src().getAtom().equals(aq.getAtom()) ? dstClass : srcClass;
                var otherTable = this.getClassTable(othClass.getAtom());
                var okeys = othClass.getKeys().stream().map(KodkodAtom::getAtom).collect(Collectors.toSet());
                if (isAStrat(assoc, "FKE")) {
                    // if this is a "foreign key" association, we need to join against the table based on the "src" key
                    var usings = new HashMap<>();
                    for (var k : srcClass.getKeys()) {
                        usings.put(k.getAtom(), null);
                    }
                    joins.putIfAbsent(otherTable, usings);
                    // if there are filters that are related to the keys of the other table,
                    // we need to build those filters so they use the other table projection
                    af.removeIf(f -> {
                        if (okeys.contains(f.getAtom())) {
                            cf.add(new ConcreteLoad.ConcreteFilter(otherTable, f.getAtom(), f.getOperator(), f.getValue()));
                            return true;
                        } else {
                            return false;
                        }
                    });
                }
                if (isAStrat(assoc, "OAT")) {
                    // if there's a join table for this association, we need to join that
                    // table based on the keys in the target class
                    var at = this.getAssocTable(assoc.getAtom());
                    joins.putIfAbsent(at, tgtClass.getKeys().stream()
                            .map(KodkodAtom::getAtom)
                            .collect(Collectors.toMap(Function.identity(), e -> at)));
                    // if there are filters that are related to the keys of the other table,
                    // we need to build those filters so they use the join table projection
                    af.removeIf(f -> {
                        if (okeys.contains(f.getAtom())) {
                            cf.add(new ConcreteLoad.ConcreteFilter(at, f.getAtom(), f.getOperator(), f.getValue()));
                            return true;
                        } else {
                            return false;
                        }
                    });
                }
            }
            // load any remaining filters
            if (af.isEmpty()) {
                cf.addAll(oodm.getClass(aq.getAtom()).getAllKeys().stream()
                        .map(k -> new ConcreteLoad.ConcreteFilter(mainTable, k.getAtom(), "<>", -1))
                        .collect(Collectors.toSet()));
            } else {
                cf.addAll(af.stream()
                        .map(f -> new ConcreteLoad.ConcreteFilter(mainTable, f.getAtom(), f.getOperator(), f.getValue()))
                        .collect(Collectors.toSet()));
            }
            // add the select statement to the concrete load
            cl.addSelect(mainTable, projection, joins.entrySet(), cf);
        }
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

    private Map<Object, Set<ObjField>> getAssocFields(ObjAssoc objAssoc) {
        if (this.isAStrat(objAssoc, "OAT")) {
            // there is a table just for this assoc that has the
            // keys from both source and dest in it
            return Map.of(this.getAssocTable(objAssoc.getAtom()),
                    Stream.of(objAssoc.dst(), objAssoc.src())
                            .map(ObjClass::getAllKeys)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet()));
        } else {
            // the dst table must include the src keys
            return Map.of(this.getClassTable(objAssoc.dst().getAtom()),
                    objAssoc.src().getAllKeys().stream()
                            .map(f -> new ObjField(f.getAtom(), f.getDataType(), false))
                            .collect(Collectors.toSet()));
        }
    }

    private Object getAssocTable(Object assoc) {
        return this.assocmap.computeIfAbsent(assoc, x -> {
            var atom = this.getAtom("x/" + assoc);
            return this.join("orm/orm.map", atom, 2)
                    .map(t -> t.atom(1))
                    .findFirst().orElseThrow();
        });
    }

    private Set<ObjField> getClassFields(ObjClass objCls) {
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

    private Map<Object, Object> getProjection(ObjClass objCls) {
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
        return tablemap.computeIfAbsent(type, x -> {
            var atom = this.getAtom("x/" + x);
            return this.join("orm/orm.main", atom, 1)
                    .map(t -> t.atom(2))
                    .findFirst().orElseThrow();
        });
    }

    private boolean isAStrat(KodkodAtom kkObj, String strat) {
        return inX(kkObj, "association_strategies/AStrat.%ss".formatted(strat));
    }

    private boolean isIStrat(KodkodAtom kkObj, String strat) {
        return inX(kkObj, "inheritance_strategies/IStrat.%ss".formatted(strat));
    }
}
