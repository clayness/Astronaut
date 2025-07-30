package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.List;


public class DBFormalAbstractMeasurementFunction implements Serializable {
    public enum MeasurementType {
        TIME, SPACE
    }

    private MeasurementType mType = null;
    private List<AbstractLoad> loads;

    public DBFormalAbstractMeasurementFunction(MeasurementType m) {
        this.setmType(m);
    }


    public MeasurementType getmType() {
        return mType;
    }


    public void setmType(MeasurementType mType) {
        this.mType = mType;
    }

    public List<AbstractLoad> getLoads() {
        return loads;
    }

    public void setLoads(List<AbstractLoad> loads) {
        this.loads = loads;
    }
}



