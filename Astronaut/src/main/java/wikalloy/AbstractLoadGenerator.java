package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

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
                .registerTypeAdapter(AbstractLoad.Instance.class, new AbstractLoad.Instance.Adapter())
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
                for (var f : m) {
                    if (f.isKey()) {
                        instance.set(f.name(), String.valueOf(this.getKeyValue(f.name())));
                    } else {
                        instance.set(f.name(), this.getStrValue());
                    }
                }
            }
        }
        for (var m : oodm.getAssociations()) {
            var srcs = new ArrayList<AbstractLoad.Instance>();
            var dsts = new ArrayList<AbstractLoad.Instance>();
            StreamSupport.stream(abl.getInstances().spliterator(), false).forEach(i -> {
                var type = i.getType();
                if (type.equals(m.src().getName())) {
                    srcs.add(i);
                }
                if (type.equals(m.dst().getName())) {
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
}