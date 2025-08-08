package wikalloy;

import com.google.gson.*;
import edu.mit.csail.sdg.translator.A4Solution;
import wikalloy.kodkod.KodkodAtom;
import wikalloy.kodkod.KodkodInstance;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class AbstractLoad implements Serializable {

    private final Set<AbstractInst> instances = new HashSet<>();
    private final Set<AbstractAssoc> associations = new HashSet<>();

    public Collection<AbstractAssoc> getAssociations() {
        return associations;
    }

    public Collection<AbstractInst> getInstances() {
        return instances;
    }

    public void newAssociation(Object name, AbstractInst src, AbstractInst dst) {
        this.associations.add(new AbstractAssoc(name, src, dst));
    }

    public AbstractInst newInstance(Object type) {
        var i = new AbstractInst(type);
        this.instances.add(i);
        return i;
    }

    public static final class AbstractAssoc extends KodkodAtom {
        private final AbstractInst dst;
        private final AbstractInst src;

        public AbstractAssoc(Object name, AbstractInst src, AbstractInst dst) {
            super(name);
            this.src = src;
            this.dst = dst;
        }

        public AbstractInst dst() {
            return dst;
        }

        public AbstractInst src() {
            return src;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractAssoc that = (AbstractAssoc) o;
            return this.getAtom().equals(that.getAtom()) && src.equals(that.src) && dst.equals(that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getAtom(), src, dst);
        }
    }

    public static class Adapter implements JsonSerializer<AbstractLoad> {
        @Override
        public JsonElement serialize(AbstractLoad src, Type typeOfSrc, JsonSerializationContext context) {
            var jsonLoad = new JsonObject();
            var jsonInstances = new JsonArray();
            for (var instance : src.instances) {
                jsonInstances.add(context.serialize(instance));
            }
            jsonLoad.add("instances", jsonInstances);
            var jsonAssociations = new JsonArray();
            for (var association : src.associations) {
                jsonAssociations.add(context.serialize(association));
            }
            jsonLoad.add("associations", jsonAssociations);
            return jsonLoad;
        }
    }

    public static class AbstractInst extends KodkodAtom implements Serializable {
        private final Map<Object, Object> values = new HashMap<>();

        private AbstractInst(Object type) {
            super(type);
        }

        public Collection<Map.Entry<Object, Object>> entrySet() {
            return values.entrySet();
        }

        public Object get(Object key) {
            return values.get(key);
        }

        public void set(Object key, Object value) {
            values.put(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractInst that = (AbstractInst) o;
            return this.getAtom().equals(that.getAtom()) && values.equals(that.values);
        }

        public static class Adapter implements JsonSerializer<AbstractInst> {
            @Override
            public JsonElement serialize(AbstractInst src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonObject = new JsonObject();
                for (var entry : src.values.entrySet()) {
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(context.serialize(entry.getValue()));
                    jsonObject.add(entry.getKey().toString(), context.serialize(jsonArray));
                }
                jsonObject.addProperty("type", src.getAtom().toString());
                return jsonObject;
            }
        }
    }

    public static class Factory extends KodkodInstance {

        private final Map<Object, AtomicInteger> keys = new HashMap<>();
        private final ObjectModel oodm;

        public Factory(A4Solution solution, ObjectModel oodm) {
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
            var gen = new Factory(sol, new ObjectModel.Factory(sol).create());
            var instances = gen.create(numInstances);
            var gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(AbstractLoad.class, new AbstractLoad.Adapter())
                    .registerTypeAdapter(AbstractLoad.AbstractInst.class, new AbstractLoad.AbstractInst.Adapter())
                    .create();
            System.out.println(gson.toJson(instances));
            System.exit(0);
        }

        public AbstractLoad create(int numInstances) {
            var abl = new AbstractLoad();
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
                var srcs = new ArrayList<AbstractLoad.AbstractInst>();
                var dsts = new ArrayList<AbstractLoad.AbstractInst>();
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
            return abl;
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
}
