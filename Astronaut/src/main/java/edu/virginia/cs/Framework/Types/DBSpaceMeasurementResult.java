package edu.virginia.cs.Framework.Types;

import java.io.Serializable;

public class DBSpaceMeasurementResult implements Serializable {
    private final double dbSpace;

    public DBSpaceMeasurementResult(double sc) {
        this.dbSpace = sc;
    }

    public double getDbSpace() {
        return dbSpace;
    }

}
