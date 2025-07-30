package edu.virginia.cs.Framework.Types;

import java.io.Serializable;

public class DBFormalAbstractMeasurementFunctionSet implements Serializable {
    private DBFormalAbstractMeasurementFunction tmf;

    public DBFormalAbstractMeasurementFunctionSet(DBFormalAbstractMeasurementFunction tmf) {
        this.setTmf(tmf);
    }

    public DBFormalAbstractMeasurementFunction getTmf() {
        return tmf;
    }

    public void setTmf(DBFormalAbstractMeasurementFunction tmf) {
        this.tmf = tmf;
    }

}
