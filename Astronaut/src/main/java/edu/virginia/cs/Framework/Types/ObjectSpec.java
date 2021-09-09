package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;

public class ObjectSpec {
    private String specPath = "";

    private ArrayList<String> ids = new ArrayList<>();

    public ArrayList<String> getIds() {
        return ids;
    }

    public void setIds(ArrayList<String> ids) {
        this.ids = ids;
    }

    public String getSpecPath() {
        return this.specPath;
    }

    public void setSpecPath(String path) {
        this.specPath = path;
    }
}
