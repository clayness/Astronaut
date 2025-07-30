package edu.virginia.cs.Framework.Types;

public class ConcreteQuery {
    private SpecializedQuery sq = null;

    public SpecializedQuery getSq() {
        return sq;
    }

    public void setSq(SpecializedQuery sq) {
        this.sq = sq;
    }
}
