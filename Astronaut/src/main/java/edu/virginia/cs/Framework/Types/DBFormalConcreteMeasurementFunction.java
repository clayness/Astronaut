package edu.virginia.cs.Framework.Types;

import java.io.Serializable;
import java.util.List;

public class DBFormalConcreteMeasurementFunction implements Serializable {
    private List<ConcreteLoad> loads;

    public List<ConcreteLoad> getLoads() {
        return loads;
    }

    public void setLoads(List<ConcreteLoad> loads) {
        this.loads = loads;
    }
}
