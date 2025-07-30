package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.List;


public class DBFormalAbstractMeasurementFunction implements Serializable {
    public enum MeasurementType {
        TIME, SPACE
    }

    private List<AbstractLoad> loads;

    public List<AbstractLoad> getLoads() {
        return loads;
    }

    public void setLoads(List<AbstractLoad> loads) {
        this.loads = loads;
    }
}



