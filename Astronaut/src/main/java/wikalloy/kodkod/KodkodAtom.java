package wikalloy.kodkod;

public abstract class KodkodAtom {
    private final Object atom;

    protected KodkodAtom(Object atom) {
        this.atom = atom;
    }

    public Object getAtom() {
        return this.atom;
    }
}
