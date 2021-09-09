package edu.virginia.cs.Synthesizer;

import java.io.Serializable;
import java.util.ArrayList;


public class Sig implements Serializable {
    int category = -1;  // 0: Class , 1: Association
    String sigName = "";
    final ArrayList<String> attrSet = new ArrayList<>();
    String id = "";
    String parent = "";
    boolean hasParent = false;
    String src = "";
    String dst = "";
    String src_mul = "";
    String dst_mul = "";

    public int getCategory() {
        return category;
    }

    public String getSigName() {
        return sigName;
    }

    public ArrayList<String> getAttrSet() {
        return attrSet;
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public boolean isHasParent() {
        return hasParent;
    }

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

}
