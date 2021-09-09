package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.ArrayList;

public class DBFormalConcreteMeasurementFunction implements Serializable {
    private ArrayList<ConcreteLoad> loads;

    public ArrayList<ConcreteLoad> getLoads() {
        return loads;
    }

    public void setLoads(ArrayList<ConcreteLoad> loads) {
        this.loads = loads;
    }
}
