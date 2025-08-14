package wikalloy;

import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.util.Objects;

public final class ObjAssoc extends KodkodAtom implements Serializable {
    private final ObjectClass dst;
    private final ObjectModel.ObjMult mlt;
    private final ObjectClass src;

    public ObjAssoc(Object atom, ObjectClass src, ObjectClass dst, ObjectModel.ObjMult mlt) {
        super(atom);
        this.src = src;
        this.dst = dst;
        this.mlt = mlt;
    }

    public ObjectClass dst() {
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

    public ObjectModel.ObjMult mlt() {
        return mlt;
    }

    public ObjectClass src() {
        return src;
    }
}
