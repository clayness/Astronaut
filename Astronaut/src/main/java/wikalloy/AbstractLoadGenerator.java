package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AbstractLoadGenerator extends AbstractKodkodGenerator {
    private static final int DEFAULT_NUM_INSTANCES = 10;

    private final Map<Object, AtomicInteger> keys = new HashMap<>();
    private final Random randy = new Random();
    private final ObjectModel oodm;

    public AbstractLoadGenerator(A4Solution solution, ObjectModel oodm) {
        super(solution);
        this.oodm = oodm;
    }

    public static void main(String[] args) {
        // get the arguments
        var modelPath = Path.of(args[0]);
        var numInstances = args.length < 2 ? 10 : Integer.parseInt(args[1]);
        // get the object model
        var it = new AlloySolutionIterator(modelPath);
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No solution found!");
        }
        var sol = it.next();
        var gen = new AbstractLoadGenerator(sol, new ObjectModelGenerator(sol).getObjectModel());
        var instances = gen.generateLoad(numInstances);
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(AbstractLoad.class, new AbstractLoad.Adapter())
                .registerTypeAdapter(AbstractLoad.AbstractInst.class, new AbstractLoad.AbstractInst.Adapter())
                .create();
        System.out.println(gson.toJson(instances));
        System.exit(0);
    }

    public AbstractLoad generateLoad() {
        return this.generateLoad(DEFAULT_NUM_INSTANCES);
    }

    public AbstractLoad generateLoad(int numInstances) {
        var abl = new AbstractLoad();
        for (var m : oodm.getClasses()) {
            for (int i = 0; i <= randy.nextInt(numInstances); i++) {
                var instance = abl.newInstance(m.getName());
                // get the fields from this class and all its parents
                var fields = new HashSet<>(m.getFields());
                var p = m.getParent();
                while (p != null) {
                    fields.addAll(p.getFields());
                    p = p.getParent();
                }
                for (var f : fields) {
                    if (f.isKey()) {
                        instance.set(f.name(), String.valueOf(this.getKeyValue(f.name())));
                    } else {
                        instance.set(f.name(), this.getStrValue());
                    }
                }
            }
        }
        for (var m : oodm.getAssociations()) {
            var srcs = new ArrayList<AbstractLoad.AbstractInst>();
            var dsts = new ArrayList<AbstractLoad.AbstractInst>();
            abl.getInstances().forEach(i -> {
                var type = i.getType();
                if (isAssignableTo(type, m.src().getName())) {
                    srcs.add(i);
                }
                if (isAssignableTo(type, m.dst().getName())) {
                    dsts.add(i);
                }
            });
            switch (m.mlt()) {
                case ONE_TO_ONE: {
                    var s = new ArrayList<>(srcs);
                    dsts.forEach(d -> abl.newAssociation(m.name(), pick(s), d));
                    break;
                }
                case ONE_TO_MANY: {
                    var s = new ArrayList<>(srcs);
                    dsts.forEach(d -> {
                        for (int i = 0; i < randy.nextInt(s.size()); i++) {
                            abl.newAssociation(m.name(), pick(s), d);
                        }
                    });
                    break;
                }
                case MANY_TO_MANY: {
                    for (int i = 0; i < randy.nextInt(dsts.size()); i++) {
                        var d = pick(dsts);
                        for (int j = 0; j < randy.nextInt(srcs.size()); j++) {
                            abl.newAssociation(m.name(), rand(srcs), d);
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid multiplicity: " + m.mlt());
            }
        }
        return abl;
    }

    private <T> T pick(List<T> items) {
        return items.remove(randy.nextInt(items.size()));
    }

    private <T> T rand(List<T> items) {
        return items.get(randy.nextInt(items.size()));
    }

    private int getKeyValue(Object key) {
        var v = keys.computeIfAbsent(key, k -> new AtomicInteger());
        return v.incrementAndGet();
    }

    private String getStrValue() {
        return UUID.randomUUID().toString();
    }

    private boolean isAssignableTo(Object from, Object to) {
        // if there is a superclass of the "from" that matches the "to",
        // then we can make this assignment
        if (from.equals(to)) {
            return true;
        } else {
            var p = oodm.getClass(from).getParent();
            if (p != null) {
                return this.isAssignableTo(p.getName(), to);
            } else {
                return false;
            }
        }
    }
}