package edu.virginia.cs.Framework.Types;

import java.io.Serializable;

public class DBTimeMeasurementResult implements Serializable {

    private final double insertTime;
    private final double selectTime;

    public DBTimeMeasurementResult(double iTime, double sTime) {
        this.insertTime = iTime;
        this.selectTime = sTime;
    }

    public double getInsertTime() {
        return insertTime;
    }

    public double getSelectTime() {
        return selectTime;
    }

}
