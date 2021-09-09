package edu.virginia.cs.Framework.Types;

public class FormalAbstractLoadSet {
    private AbstractLoad insLoad;
    private AbstractLoad selLoad;

    public FormalAbstractLoadSet(AbstractLoad insLoad, AbstractLoad selLoad) {
        this.setInsLoad(insLoad);
        this.setSelLoad(selLoad);
    }

    public AbstractLoad getInsLoad() {
        return insLoad;
    }

    public void setInsLoad(AbstractLoad insLoad) {
        this.insLoad = insLoad;
    }

    public AbstractLoad getSelLoad() {
        return selLoad;
    }

    public void setSelLoad(AbstractLoad selLoad) {
        this.selLoad = selLoad;
    }

}
