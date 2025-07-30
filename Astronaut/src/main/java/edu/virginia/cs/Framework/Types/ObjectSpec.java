package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.Synthesizer.Sig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectSpec {
    private String specPath = "";

    private List<String> ids = new ArrayList<>();
    private List<String> associations = null;
    private Map<String, String> typeList = null;
    private List<Sig> sigs = null;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public List<String> getAssociations() {
        return associations;
    }

    public void setAssociations(List<String> associations) {
        this.associations = associations;
    }

    public Map<String, String> getTypeList() {
        return typeList;
    }

    public void setTypeList(Map<String, String> typeList) {
        this.typeList = typeList;
    }

    public List<Sig> getSigs() {
        return sigs;
    }

    public void setSigs(List<Sig> sigs) {
        this.sigs = sigs;
    }

    public String getSpecPath() {
        return this.specPath;
    }

    public void setSpecPath(String path) {
        this.specPath = path;
    }
}
