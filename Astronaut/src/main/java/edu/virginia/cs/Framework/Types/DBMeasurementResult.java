package edu.virginia.cs.Framework.Types;

import java.io.Serializable;

public class DBMeasurementResult implements Serializable {
    public final DBTimeMeasurementResult tmr;
    public final DBSpaceMeasurementResult smr;
    public DBImplementation impl;

    public DBMeasurementResult(DBTimeMeasurementResult tmr, DBSpaceMeasurementResult smr) {
        this.tmr = tmr;
        this.smr = smr;
    }

    public DBTimeMeasurementResult getTmr() {
        return tmr;
    }

    public DBSpaceMeasurementResult getSmr() {
        return smr;
    }

    public void setImpl(DBImplementation impl) {
        this.impl = impl;
    }
}
