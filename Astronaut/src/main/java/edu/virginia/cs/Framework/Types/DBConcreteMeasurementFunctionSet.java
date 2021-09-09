package edu.virginia.cs.Framework.Types;

import java.io.Serializable;

public class DBConcreteMeasurementFunctionSet implements Serializable {
    private final DBConcreteSpaceMeasurementFunction csmf;
    private final DBConcreteTimeMeasurementFunction ctmf;

    public DBConcreteMeasurementFunctionSet(DBConcreteTimeMeasurementFunction ctf,
                                            DBConcreteSpaceMeasurementFunction stf) {
        this.ctmf = ctf;
        this.csmf = stf;
    }

    public DBConcreteSpaceMeasurementFunction getCsmf() {
        return csmf;
    }

    public DBConcreteTimeMeasurementFunction getCtmf() {
        return ctmf;
    }

}
