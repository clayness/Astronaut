package wikalloy.generic;

import com.google.gson.*;
import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbstractInst extends KodkodAtom implements Serializable {
    private final Map<Object, Object> values = new HashMap<>();

    AbstractInst(Object type) {
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
