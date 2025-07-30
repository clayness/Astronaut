package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tang Abstract load is a collection of abstract query
 */
public class AbstractLoad {
    // there are just two elements in querySey
    // the first one is insert abstract query
    // the second one is select abstract query
    private List<AbstractQuery> querySet = null;

    public AbstractLoad() {
        this.querySet = new ArrayList<AbstractQuery>();
    }

    public AbstractLoad(List<AbstractQuery> querySet) {
        this.querySet = querySet;
    }

    public List<AbstractQuery> getQuerySet() {
        return this.querySet;
    }

    public void setQuerySet(List<AbstractQuery> querySet) {
        this.querySet = querySet;
    }

    public AbstractQuery getInsertAbstractQuery() {
        AbstractQuery insertAQ = null;
        if (this.querySet.size() > 0) {
            insertAQ = this.querySet.get(0);
        }
        return insertAQ;
    }

    public AbstractQuery getSelectAbstractQuery() {
        AbstractQuery selectAQ = null;
        if (this.querySet.size() > 1) {
            selectAQ = this.querySet.get(1);
        }
        return selectAQ;
    }
}
