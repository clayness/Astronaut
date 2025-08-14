package wikalloy;

import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectClass extends KodkodAtom implements Serializable {
    private final Map<Object, ObjField> fields = new HashMap<>();
    private final ObjectClass parent;

    public ObjectClass(Object name, ObjectClass parent) {
        super(name);
        this.parent = parent;
    }

    public void addField(ObjField field) {
        this.fields.put(field.getAtom(), field);
    }

    public Set<ObjField> getAllKeys() {
        // get the keys for this class and all its parents
        var k = this.getKeys();
        var p = this.getParent();
        if (p != null) {
            k.addAll(p.getAllKeys());
        }
        return k;
    }

    public ObjField getField(Object name) {
        return fields.get(name);
    }

    public Collection<ObjField> getFields() {
        return this.fields.values();
    }

    public ObjectClass getParent() {
        return parent;
    }

    public Set<ObjField> getKeys() {
        return this.fields.values().stream()
                .filter(ObjField::isKey)
                .collect(Collectors.toSet());
    }
}
