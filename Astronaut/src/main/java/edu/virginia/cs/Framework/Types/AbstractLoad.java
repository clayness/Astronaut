package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;

/**
 * @author tang Abstract load is a collection of abstract query
 */
public class AbstractLoad {
    // there are just two elements in querySey
    // the first one is insert abstract query
    // the second one is select abstract query
    private ArrayList<AbstractQuery> querySet;

    public AbstractLoad() {
        this.querySet = new ArrayList<>();
    }

    public ArrayList<AbstractQuery> getQuerySet() {
        return this.querySet;
    }

    public void setQuerySet(ArrayList<AbstractQuery> querySet) {
        this.querySet = querySet;
    }

}
