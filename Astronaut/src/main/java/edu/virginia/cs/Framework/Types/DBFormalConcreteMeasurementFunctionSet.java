package edu.virginia.cs.Framework.Types;


public class DBFormalConcreteMeasurementFunctionSet {
    private DBFormalConcreteSpaceMeasurementFunction csmf;
    private DBFormalConcreteTimeMeasurementFunction ctmf;

    public DBFormalConcreteSpaceMeasurementFunction getCsmf() {
        return csmf;
    }

    public void setCsmf(DBFormalConcreteSpaceMeasurementFunction csmf) {
        this.csmf = csmf;
    }

    public DBFormalConcreteTimeMeasurementFunction getCtmf() {
        return ctmf;
    }

    public void setCtmf(DBFormalConcreteTimeMeasurementFunction ctmf) {
        this.ctmf = ctmf;
    }
}
