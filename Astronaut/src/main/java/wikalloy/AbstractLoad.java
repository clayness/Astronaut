package wikalloy;

import com.google.gson.*;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;

public class AbstractLoad implements Serializable {

    private final Set<AbstractInst> instances = new HashSet<>();
    private final Set<AbstractAssoc> associations = new HashSet<>();

    public AbstractInst newInstance(Object type) {
        var i = new AbstractInst(type);
        this.instances.add(i);
        return i;
    }

    public void newAssociation(Object name, AbstractInst src, AbstractInst dst) {
        this.associations.add(new AbstractAssoc(name, src, dst));
    }

    public Collection<AbstractInst> getInstances() {
        return instances;
    }

    public Collection<AbstractAssoc> getAssociations() {
        return associations;
    }

    public record AbstractAssoc(Object name, AbstractInst src, AbstractInst dst) {
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractAssoc that = (AbstractAssoc) o;
            return name.equals(that.name) && src.equals(that.src) && dst.equals(that.dst);
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

    public static class AbstractInst implements Serializable {
        private final Map<Object, Object> values = new HashMap<>();
        private final Object type;

        private AbstractInst(Object type) {
            this.type = type;
        }

        public Object getType() {
            return this.type;
        }

        public void set(Object key, Object value) {
            values.put(key, value);
        }

        public Object get(Object key) {
            return values.get(key);
        }

        public Collection<Map.Entry<Object, Object>> entrySet() {
            return values.entrySet();
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
                jsonObject.addProperty("type", src.type.toString());
                return jsonObject;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AbstractInst that = (AbstractInst) o;
            return type.equals(that.type) && values.equals(that.values);
        }
    }
}
