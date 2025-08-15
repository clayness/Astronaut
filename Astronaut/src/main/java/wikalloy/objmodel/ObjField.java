package wikalloy.objmodel;

import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.util.Objects;

public final class ObjField extends KodkodAtom implements Serializable {
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
