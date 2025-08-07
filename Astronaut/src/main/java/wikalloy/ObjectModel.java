package wikalloy;

import java.util.*;
import java.util.stream.Collectors;

public class ObjectModel {
    private final Map<Object, ObjClass> classes = new HashMap<>();
    private final List<ObjAssoc> associations = new ArrayList<>();

    public ObjClass addClass(Object name, ObjClass parent) {
        var cls = new ObjClass(name, parent);
        this.classes.put(cls.getName(), cls);
        return cls;
    }

    public boolean hasClass(Object name) {
        return this.classes.containsKey(name);
    }

    public ObjClass getClass(Object name) {
        return this.classes.get(name);
    }

    public Collection<ObjClass> getClasses() {
        return classes.values();
    }

    public Collection<ObjAssoc> getAssociations() {
        return associations;
    }

    public void addAssociation(Object name, Object src, Object dst, ObjMult mlt) {
        this.associations.add(new ObjAssoc(name, this.getClass(src), this.getClass(dst), mlt));
    }

    public enum ObjMult {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    }

    public static class ObjClass implements Iterable<ObjField> {

        private final Object name;
        private final Map<Object, ObjField> fields = new HashMap<>();
        private final ObjClass parent;

        public ObjClass(Object name, ObjClass parent) {
            this.name = name;
            this.parent = parent;
        }

        public ObjClass getParent() {
            return parent;
        }

        public Object getName() {
            return name;
        }

        public Set<ObjField> getKeys() {
            return this.fields.values().stream()
                    .filter(ObjField::isKey)
                    .collect(Collectors.toSet());
        }

        public ObjField getField(Object name) {
            return fields.get(name);
        }

        public void addField(ObjField field) {
            this.fields.put(field.name(), field);
        }

        @Override
        public Iterator<ObjField> iterator() {
            return this.fields.values().iterator();
        }

        public Collection<ObjField> getFields() {
            return this.fields.values();
        }
    }

    public record ObjAssoc(Object name, ObjClass src, ObjClass dst, ObjMult mlt) {
        /* no-op, record class */
    }

    public record ObjField(Object name, Object type, boolean isKey) {
        /* no-op, record class */
    }
}
