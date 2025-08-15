package wikalloy.generic;

import edu.mit.csail.sdg.translator.A4Solution;
import wikalloy.objmodel.ObjectModel;
import wikalloy.kodkod.KodkodInstance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AbstractLoadFactory extends KodkodInstance {

    private final Map<Object, AtomicInteger> keys = new HashMap<>();
    private final ObjectModel oodm;

    public AbstractLoadFactory(A4Solution solution, ObjectModel oodm) {
        super(solution);
        this.oodm = oodm;
    }

    public AbstractLoad create(int numInstances, int numQueries) {
        var abl = new AbstractLoad();
        createInstances(numInstances, abl);
        createQueries(numQueries, abl);
        return abl;
    }

    private void createInstances(int numInstances, AbstractLoad abl) {
        for (var m : oodm.getClasses()) {
            for (int i = 0; i <= ThreadLocalRandom.current().nextInt(numInstances); i++) {
                var instance = abl.newInstance(m.getAtom());
                // get the fields from this class and all its parents
                var fields = new HashSet<>(m.getFields());
                var p = m.getParent();
                while (p != null) {
                    fields.addAll(p.getFields());
                    p = p.getParent();
                }
                for (var f : fields) {
                    Object atom = f.getAtom();
                    if (f.isKey()) {
                        instance.set(atom, String.valueOf(this.getKeyValue(atom)));
                    } else {
                        instance.set(atom, this.getValue(f.getDataType()));
                    }
                }
            }
        }
        for (var m : oodm.getAssociations()) {
            var srcs = new ArrayList<AbstractInst>();
            var dsts = new ArrayList<AbstractInst>();
            abl.getInstances().forEach(i -> {
                var type = i.getAtom();
                if (isAssignableTo(type, m.src().getAtom())) {
                    srcs.add(i);
                }
                if (isAssignableTo(type, m.dst().getAtom())) {
                    dsts.add(i);
                }
            });
            switch (m.mlt()) {
                case ONE_TO_ONE: {
                    var s = new ArrayList<>(srcs);
                    dsts.forEach(d -> abl.newAssociation(m.getAtom(), pick(s), d));
                    break;
                }
                case ONE_TO_MANY: {
                    var s = new ArrayList<>(srcs);
                    dsts.forEach(d -> {
                        for (int i = 0; i < ThreadLocalRandom.current().nextInt(s.size()); i++) {
                            abl.newAssociation(m.getAtom(), pick(s), d);
                        }
                    });
                    break;
                }
                case MANY_TO_MANY: {
                    for (int i = 0; i < ThreadLocalRandom.current().nextInt(dsts.size()); i++) {
                        var d = pick(dsts);
                        for (int j = 0; j < ThreadLocalRandom.current().nextInt(srcs.size()); j++) {
                            abl.newAssociation(m.getAtom(), rand(srcs), d);
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid multiplicity: " + m.mlt());
            }
        }
    }

    private void createQueries(int numQueries, AbstractLoad al) {
        for (int i = 0; i <= ThreadLocalRandom.current().nextInt(numQueries); i++) {
            switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0: {
                    // pick a random type from the object model to get all of them (e.g., Product.all)
                    al.newQuery(rand(oodm.getClasses().stream().toList()).getAtom(), null, List.of());
                    break;
                }
                case 1: {
                    // pick a random instance from the abstract load to fetch specifically that one (e.g., PhysicalProduct.find(X))
                    var inst = rand(al.getInstances().stream().toList());
                    var keys = oodm.getClass(inst.getAtom()).getAllKeys().stream()
                            .map(k -> new AbstractFilter(k.getAtom(), "=", inst.get(k.getAtom())))
                            .collect(Collectors.toSet());
                    al.newQuery(inst.getAtom(), null, keys);
                    break;
                }
                case 2: {
                    // pick a random association, then pick either the source or destination instance.
                    // select the other side of the association based on the shared keys (e.g., ShippingCart.find(X).products)
                    var assocs = al.getAssociations();
                    if (!assocs.isEmpty()) {
                        var assoc = rand(assocs.stream().toList());
                        var ends = new ArrayList<>(List.of(assoc.src(), assoc.dst()));
                        var inst = pick(ends);
                        var keys = oodm.getClass(inst.getAtom()).getAllKeys().stream()
                                .map(k -> new AbstractFilter(k.getAtom(), "=", inst.get(k.getAtom())))
                                .collect(Collectors.toSet());
                        var othr = ends.get(0);
                        al.newQuery(othr.getAtom(), assoc, keys);
                    }
                    break;
                }
            }
        }
    }

    private int getKeyValue(Object key) {
        var v = keys.computeIfAbsent(key, k -> new AtomicInteger());
        return v.incrementAndGet();
    }

    private Object getValue(Object type) {
        //@formatter:off
        return switch (type.toString()) {
            case "oodm/TBool$0"   -> ThreadLocalRandom.current().nextBoolean() ? 1 : 0;
            case "oodm/TInt$0"    -> ThreadLocalRandom.current().nextInt(1024);
            case "oodm/TFloat$0"  -> ThreadLocalRandom.current().nextFloat();
            case "oodm/TString$0" -> UUID.randomUUID().toString();
            case "oodm/TDate$0"   -> LocalDate.ofEpochDay(ThreadLocalRandom.current()
                    .nextInt(-3650, 3650)).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "oodm/TBlob$0"   -> UUID.randomUUID().toString().getBytes();
            default -> throw new IllegalArgumentException("Unexpected data type: " + type);
        };
        //@formatter:on
    }

    private boolean isAssignableTo(Object from, Object to) {
        // if there is a superclass of the "from" that matches the "to",
        // then we can make this assignment
        if (from.equals(to)) {
            return true;
        } else {
            var p = oodm.getClass(from).getParent();
            if (p != null) {
                return this.isAssignableTo(p.getAtom(), to);
            } else {
                return false;
            }
        }
    }

    private <T> T pick(List<T> items) {
        return items.remove(ThreadLocalRandom.current().nextInt(items.size()));
    }

    private <T> T rand(List<T> items) {
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }
}
