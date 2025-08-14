package wikalloy.generic;

import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.util.Objects;

public final class AbstractAssoc extends KodkodAtom implements Serializable {
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
