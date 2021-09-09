package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.ArrayList;


public class DBFormalAbstractMeasurementFunction implements Serializable {
    private ArrayList<AbstractLoad> loads;

    public ArrayList<AbstractLoad> getLoads() {
        return loads;
    }

    public void setLoads(ArrayList<AbstractLoad> loads) {
        this.loads = loads;
    }

    public enum MeasurementType {
        TIME, SPACE
    }
}



