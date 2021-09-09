package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;

public class DBConcreteSpaceMeasurementFunction extends DBConcreteMeasurementFunction {
    public DBConcreteSpaceMeasurementFunction() {
        super();
    }

    public DBConcreteSpaceMeasurementFunction(ArrayList<ConcreteLoad> loads) {
        super();
        super.setLoads(loads);
    }

    public DBSpaceMeasurementResult run() {
        // run insert only, and check the space consumption
        double spaceConsumption = super.mfByDB.checkSpace();
        // drop database after test
        super.mfByDB.dropDB();
        return new DBSpaceMeasurementResult(spaceConsumption);
    }
}
