package wikalloy;

import com.google.gson.*;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;

public class AbstractLoad implements Serializable {

    private final List<Instance> instances = new ArrayList<>();
    private final List<InstanceAssociation> associations = new ArrayList<>();

    public Instance newInstance(Object type) {
        var i = new Instance(type);
        this.instances.add(i);
        return i;
    }

    public void newAssociation(Object name, Instance src, Instance dst) {
        this.associations.add(new InstanceAssociation(name, src, dst));
    }

    public Iterable<Instance> getInstances() {
        return instances;
    }

    public record InstanceAssociation(Object name, Instance src, Instance dst) {
        /* no-op, record class */
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

    public static class Instance implements Serializable {
        private final Map<Object, List<Object>> values = new HashMap<>();
        private final Object type;

        private Instance(Object type) {
            this.type = type;
        }

        public Object getType() {
            return this.type;
        }

        public void set(Object key, Object value) {
            values.computeIfAbsent(key, (k) -> new ArrayList<>()).add(value);
        }

        public List<Object> get(Object key) {
            return values.get(key);
        }

        public Collection<Map.Entry<Object, List<Object>>> entrySet() {
            return values.entrySet();
        }

        public static class Adapter implements JsonSerializer<Instance> {
            @Override
            public JsonElement serialize(Instance src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jsonObject = new JsonObject();
                for (var entry : src.values.entrySet()) {
                    JsonArray jsonArray = new JsonArray();
                    for (var value : entry.getValue()) {
                        jsonArray.add(context.serialize(value));
                    }
                    jsonObject.add(entry.getKey().toString(), context.serialize(jsonArray));
                }
                jsonObject.addProperty("type", src.type.toString());
                return jsonObject;
            }
        }
    }
}
