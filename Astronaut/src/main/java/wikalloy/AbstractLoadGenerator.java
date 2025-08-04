package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.instance.Tuple;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractLoadGenerator extends AbstractKodkodGenerator {
    private static final int DEFAULT_NUM_INSTANCES = 10;

    private final Map<Object, AtomicInteger> keys = new HashMap<>();
    private final Random randy = new Random();

    public AbstractLoadGenerator(A4Solution solution) {
        super(solution);
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
        var gen = new AbstractLoadGenerator(it.next());
        var instances = gen.generateLoad(numInstances);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(instances));
        System.exit(0);
    }

    public List<Map<String, String>> generateLoad() {
        return this.generateLoad(DEFAULT_NUM_INSTANCES);
    }

    public List<Map<String, String>> generateLoad(int numInstances) {
        var oom = this.getObjectModel();
        var instances = new ArrayList<Map<String, String>>();
        for (var m : oom) {
            for (int i = 0; i <= randy.nextInt(numInstances); i++) {
                // clone the "class" map
                var instance = new HashMap<>(m);
                for (var kvp : m.entrySet()) {
                    switch (kvp.getValue()) {
                        case "<KEY>" -> instance.put(kvp.getKey(), String.valueOf(this.getKeyValue(kvp.getKey())));
                        case "<VAL>" -> instance.put(kvp.getKey(), this.getStrValue());
                    }
                }
                instances.add(instance);
            }
        }
        return instances;
    }

    public List<Map<String, String>> getObjectModel() {
        return this.getTuples("oodm/Class.key")
                .map(k -> k.atom(0)).distinct()
                .map(c -> {
                    var m = this.getAllFields(c).collect(Collectors.toMap(Object::toString,
                            f -> this.isKey(f) ? "<KEY>" : "<VAL>"));
                    m.put("type", c.toString());
                    return m;
                }).toList();
    }

    private Stream<Object> getAllFields(Object cls) {
        return Stream.concat(Stream.of(cls), this.tc(this.join("oodm/Class.parent", cls, 0).collect(Collectors.toSet())).stream()
                        .map(t -> t.atom(1)))
                .distinct()
                .flatMap(t -> this.join("oodm/Class.fields", t, 0))
                .map(f -> f.atom(1));
    }

    private boolean isKey(Object field) {
        return this.join("oodm/Class.key", field, 1)
                .findAny()
                .isPresent();
    }

    private Set<Tuple> tc(Set<Tuple> ts) {
        // compute the transitive closure of the set of pairs
        var num = ts.size();
        do {
            ts.addAll(ts.stream()
                    .flatMap(t -> this.join("oodm/Class.parent", t.atom(1), 0))
                    .collect(Collectors.toSet()));
        } while (num < ts.size());
        return ts;
    }

    private int getKeyValue(Object key) {
        var v = keys.computeIfAbsent(key, k -> new AtomicInteger());
        return v.incrementAndGet();
    }

    private String getStrValue() {
        return UUID.randomUUID().toString();
    }
}
