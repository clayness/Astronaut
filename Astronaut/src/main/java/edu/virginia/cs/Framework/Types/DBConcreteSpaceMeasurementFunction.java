package edu.virginia.cs.Framework.Types;

import java.util.List;

public class DBConcreteSpaceMeasurementFunction extends DBConcreteMeasurementFunction {
    public DBConcreteSpaceMeasurementFunction() {
        super();
    }

    public DBConcreteSpaceMeasurementFunction(List<ConcreteLoad> loads) {
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
