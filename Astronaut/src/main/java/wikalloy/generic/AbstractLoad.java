package wikalloy.generic;

import com.google.gson.*;
import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;

public class AbstractLoad implements Serializable {

    private final Set<AbstractInst> instances = new HashSet<>();
    private final Set<AbstractAssoc> associations = new HashSet<>();
    private final Set<AbstractQuery> queries = new HashSet<>();

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

    public Collection<AbstractQuery> getQueries() {
        return queries;
    }

    public void newQuery(Object type, AbstractAssoc association, Collection<AbstractFilter> filters) {
        this.queries.add(new AbstractQuery(type, filters, association));
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
}
