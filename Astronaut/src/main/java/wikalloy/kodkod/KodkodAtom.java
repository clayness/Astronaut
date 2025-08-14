package wikalloy.kodkod;

import java.io.Serializable;

public abstract class KodkodAtom implements Serializable {
    private final Object atom;

    protected KodkodAtom(Object atom) {
        this.atom = atom;
    }

    public Object getAtom() {
        return this.atom;
    }
}
