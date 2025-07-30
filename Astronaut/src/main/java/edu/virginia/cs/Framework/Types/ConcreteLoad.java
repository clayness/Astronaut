package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;
import java.util.List;

public class ConcreteLoad {
    private String insertPath = "";
    private String selectPath = "";

    private List<ConcreteQuery> querySet;

    public ConcreteLoad() {
        this.querySet = new ArrayList<>();
    }

    public List<ConcreteQuery> getQuerySet() {
        return this.querySet;
    }

    public void setQuerySet(List<ConcreteQuery> querySet) {
        this.querySet = querySet;
    }

    public String getInsertPath() {
        return insertPath;
    }

    public void setInsertPath(String insertPath) {
        this.insertPath = insertPath;
    }

    public String getSelectPath() {
        return selectPath;
    }

    public void setSelectPath(String selectPath) {
        this.selectPath = selectPath;
    }

}
