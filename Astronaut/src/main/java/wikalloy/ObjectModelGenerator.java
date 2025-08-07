package wikalloy;

import com.google.gson.GsonBuilder;
import edu.mit.csail.sdg.translator.A4Solution;

import java.nio.file.Path;

public class ObjectModelGenerator extends AbstractKodkodGenerator {

    public ObjectModelGenerator(A4Solution solution) {
        super(solution);
    }

    public static void main(String[] args) {
        var modelPath = Path.of(args[0]);
        var it = new AlloySolutionIterator(modelPath);
        if (!it.hasNext()) {
            throw new IllegalArgumentException("No solution found!");
        }
        var oodm = new ObjectModelGenerator(it.next());
        var om = oodm.getObjectModel();
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        System.out.println(gson.toJson(om));
        System.exit(0);
    }

    public ObjectModel getObjectModel() {
        final var oodm = new ObjectModel();
        this.extractClasses(oodm);
        this.extractAssociations(oodm);
        return oodm;
    }

    private void extractClasses(ObjectModel oodm) {
        this.getTuples("oodm/Class.key")
                .map(k -> k.atom(0))
                .forEach(kkCls -> this.create(oodm, kkCls));
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

    private ObjectModel.ObjClass create(ObjectModel oodm, Object kkCls) {
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
            p = this.create(oodm, kkp);
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

    private boolean isKey(Object kkClass, Object kkField) {
        return this.getTuples("oodm/Class.key")
                .anyMatch(k -> k.atom(0) == kkClass && k.atom(1) == kkField);
    }
}
