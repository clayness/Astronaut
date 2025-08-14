package wikalloy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ObjectModel implements Serializable {
    private final Map<Object, ObjectClass> classes = new HashMap<>();
    private final Map<Object, ObjAssoc> associations = new HashMap<>();

    public void addAssociation(Object name, Object src, Object dst, ObjMult mlt) {
        this.associations.put(name, new ObjAssoc(name, this.getClass(src), this.getClass(dst), mlt));
    }

    public ObjectClass addClass(Object name, ObjectClass parent) {
        var cls = new ObjectClass(name, parent);
        this.classes.put(cls.getAtom(), cls);
        return cls;
    }

    public ObjAssoc getAssociation(Object name) {
        return this.associations.get(name);
    }

    public Collection<ObjAssoc> getAssociations() {
        return associations.values();
    }

    public ObjectClass getClass(Object name) {
        return this.classes.get(name);
    }

    public Collection<ObjectClass> getClasses() {
        return classes.values();
    }

    public boolean hasClass(Object name) {
        return this.classes.containsKey(name);
    }

    public enum ObjMult {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    }

}
