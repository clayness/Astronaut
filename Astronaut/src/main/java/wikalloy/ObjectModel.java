package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;
import wikalloy.kodkod.KodkodAtom;
import wikalloy.kodkod.KodkodInstance;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectModel {
    private final Map<Object, ObjClass> classes = new HashMap<>();
    private final Map<Object, ObjAssoc> associations = new HashMap<>();

    public void addAssociation(Object name, Object src, Object dst, ObjMult mlt) {
        this.associations.put(name, new ObjAssoc(name, this.getClass(src), this.getClass(dst), mlt));
    }

    public ObjClass addClass(Object name, ObjClass parent) {
        var cls = new ObjClass(name, parent);
        this.classes.put(cls.getAtom(), cls);
        return cls;
    }
    
    public ObjAssoc getAssociation(Object name) {
        return this.associations.get(name);
    }

    public Collection<ObjAssoc> getAssociations() {
        return associations.values();
    }

    public ObjClass getClass(Object name) {
        return this.classes.get(name);
    }

    public Collection<ObjClass> getClasses() {
        return classes.values();
    }

    public boolean hasClass(Object name) {
        return this.classes.containsKey(name);
    }

    public enum ObjMult {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    }

    public static class ObjClass extends KodkodAtom {
        private final Map<Object, ObjField> fields = new HashMap<>();
        private final ObjClass parent;

        public ObjClass(Object name, ObjClass parent) {
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

        public ObjClass getParent() {
            return parent;
        }

        public Set<ObjField> getKeys() {
            return this.fields.values().stream()
                    .filter(ObjField::isKey)
                    .collect(Collectors.toSet());
        }
    }

    public static final class ObjAssoc extends KodkodAtom {
        private final ObjClass dst;
        private final ObjMult mlt;
        private final ObjClass src;

        public ObjAssoc(Object atom, ObjClass src, ObjClass dst, ObjMult mlt) {
            super(atom);
            this.src = src;
            this.dst = dst;
            this.mlt = mlt;
        }

        public ObjClass dst() {
            return dst;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ObjAssoc) obj;
            return Objects.equals(this.getAtom(), that.getAtom()) &&
                    Objects.equals(this.src, that.src) &&
                    Objects.equals(this.dst, that.dst) &&
                    Objects.equals(this.mlt, that.mlt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getAtom(), src, dst, mlt);
        }

        public ObjMult mlt() {
            return mlt;
        }

        public ObjClass src() {
            return src;
        }
    }

    public static final class ObjField extends KodkodAtom {
        private final Object dataType;
        private final boolean isKey;

        public ObjField(Object name, Object dataType, boolean isKey) {
            super(name);
            this.dataType = dataType;
            this.isKey = isKey;
        }

        /* no-op, record class */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            // ignores the "is key" field
            ObjField objField = (ObjField) o;
            return this.getAtom().equals(objField.getAtom()) && dataType.equals(objField.dataType);
        }


        public Object getDataType() {
            return dataType;
        }

        @Override
        public int hashCode() {
            // ignores the "is key" field
            return Objects.hash(this.getAtom(), dataType);
        }

        public boolean isKey() {
            return isKey;
        }
    }

    public static class Factory extends KodkodInstance {

        public Factory(A4Solution solution) {
            super(solution);
        }

        public static void main(String[] args) {
            var modelPath = Path.of(args[0]);
            var it = new AlloySolutionIterator(modelPath);
            if (!it.hasNext()) {
                throw new IllegalArgumentException("No solution found!");
            }
            var oodm = new Factory(it.next());
            var om = oodm.create();
            var gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            System.out.println(gson.toJson(om));
            System.exit(0);
        }

        public ObjectModel create() {
            final var oodm = new ObjectModel();
            this.extractClasses(oodm);
            this.extractAssociations(oodm);
            return oodm;
        }

        private ObjectModel.ObjClass addClass(ObjectModel oodm, Object kkCls) {
            // make sure we haven't done this one before
            if (oodm.hasClass(kkCls)) {
                return oodm.getClass(kkCls);
            }
            // check to see if this class has a parent
            ObjectModel.ObjClass p = null;
            var kkp = this.join("oodm/Class.parent", kkCls, 0)
                    .map(t -> t.atom(1))
                    .findFirst()
                    .orElse(null);
            if (kkp != null) {
                p = this.addClass(oodm, kkp);
            }
            // create the class and add the fields
            var objCls = oodm.addClass(kkCls, p);
            this.addFields(objCls, kkCls);
            // return the class
            return objCls;
        }

        private void addFields(final ObjectModel.ObjClass objCls, final Object kkCls) {
            this.join("oodm/Class.fields", kkCls, 0)
                    .distinct()
                    .map(f -> new ObjectModel.ObjField(f.atom(1), f.atom(2), this.isKey(kkCls, f.atom(1))))
                    .forEach(objCls::addField);
        }

        private void extractAssociations(ObjectModel oodm) {
            this.getTuples("oodm/Association.src")
                    .forEach(t -> {
                        // get the src class name
                        var src = t.atom(1);
                        // get the dst class that goes with it
                        var dst = this.join("oodm/Association.dst", t.atom(0), 0)
                                .map(d -> d.atom(1))
                                .findFirst()
                                .orElseThrow();
                        // get the multiplicity that goes with it
                        var mlt = this.join("oodm/Association.mlt", t.atom(0), 0)
                                .map(m -> switch (m.atom(1).toString()) {
                                    case "oodm/OtoO$0" -> ObjectModel.ObjMult.ONE_TO_ONE;
                                    case "oodm/OtoM$0" -> ObjectModel.ObjMult.ONE_TO_MANY;
                                    case "oodm/MtoM$0" -> ObjectModel.ObjMult.MANY_TO_MANY;
                                    default -> throw new IllegalArgumentException("Invalid multiplicity: " + m.atom(1));
                                })
                                .findFirst()
                                .orElseThrow();
                        oodm.addAssociation(t.atom(0), src, dst, mlt);
                    });
        }

        private void extractClasses(ObjectModel oodm) {
            this.getTuples("oodm/Class.key")
                    .map(k -> k.atom(0))
                    .forEach(kkCls -> this.addClass(oodm, kkCls));
        }

        private boolean isKey(Object kkClass, Object kkField) {
            return this.getTuples("oodm/Class.key")
                    .anyMatch(k -> k.atom(0) == kkClass && k.atom(1) == kkField);
        }
    }
}
