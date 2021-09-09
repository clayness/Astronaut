package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;

public class DBFormalConcreteSpaceMeasurementFunction extends DBFormalConcreteMeasurementFunction {
    public DBFormalConcreteSpaceMeasurementFunction(ConcreteLoad load) {
        super();
        ArrayList<ConcreteLoad> l = new ArrayList<>();
        l.add(load);
        super.setLoads(l);
    }
}
