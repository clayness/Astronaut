package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.List;

abstract class MeasurementFunctionByDB implements Serializable {

    protected List<ConcreteLoad> loads;
    protected DBImplementation impl;

    public List<ConcreteLoad> getLoads() {
        return loads;
    }

    public void setLoads(List<ConcreteLoad> loads) {
        this.loads = loads;
    }

    public void setImpl(DBImplementation impl) {
        this.impl = impl;
    }

    public abstract double checkSpace();

    public abstract void createDB();

    public abstract void dropDB();

    public abstract void createTables();

    public abstract double runInsert();

    public abstract double runSelect();

}
