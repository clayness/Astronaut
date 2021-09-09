package edu.virginia.cs.Framework.Types;

import java.util.HashMap;

public class SpecializedQuery {
    private HashMap<String, HashMap<Integer, String>> insertStmtsInOneObject = new HashMap<>();
    private HashMap<String, HashMap<Integer, String>> selectStmtsInOneObject = new HashMap<>();

    public HashMap<String, HashMap<Integer, String>> getInsertStmtsInOneObject() {
        return insertStmtsInOneObject;
    }

    public void setInsertStmtsInOneObject(
            HashMap<String, HashMap<Integer, String>> insertStmtsInOneObject) {
        this.insertStmtsInOneObject = insertStmtsInOneObject;
    }

    public HashMap<String, HashMap<Integer, String>> getSelectStmtsInOneObject() {
        return selectStmtsInOneObject;
    }

    public void setSelectStmtsInOneObject(
            HashMap<String, HashMap<Integer, String>> selectStmtsInOneObject) {
        this.selectStmtsInOneObject = selectStmtsInOneObject;
    }
}
