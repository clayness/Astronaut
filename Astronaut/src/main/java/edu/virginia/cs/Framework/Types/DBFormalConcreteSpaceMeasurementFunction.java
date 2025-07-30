package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;
import java.util.List;

public class DBFormalConcreteSpaceMeasurementFunction extends DBFormalConcreteMeasurementFunction {
    public DBFormalConcreteSpaceMeasurementFunction(ConcreteLoad load) {
        List<ConcreteLoad> l = new ArrayList<>();
        l.add(load);
        super.setLoads(l);
    }
}
