package edu.virginia.cs.Framework.Types;

import java.util.HashMap;
import java.util.Map;

public class SpecializedQuery {
    private Map<String, Map<Integer, String>> insertStmtsInOneObject = new HashMap<>();
    private Map<String, Map<Integer, String>> selectStmtsInOneObject = new HashMap<>();

    public Map<String, Map<Integer, String>> getInsertStmtsInOneObject() {
        return insertStmtsInOneObject;
    }

    public void setInsertStmtsInOneObject(Map<String, Map<Integer, String>> insertStmtsInOneObject) {
        this.insertStmtsInOneObject = insertStmtsInOneObject;
    }

    public Map<String, Map<Integer, String>> getSelectStmtsInOneObject() {
        return selectStmtsInOneObject;
    }

    public void setSelectStmtsInOneObject(Map<String, Map<Integer, String>> selectStmtsInOneObject) {
        this.selectStmtsInOneObject = selectStmtsInOneObject;
    }
}
