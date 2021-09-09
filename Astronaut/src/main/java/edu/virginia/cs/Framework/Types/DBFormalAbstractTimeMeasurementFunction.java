package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;

public class DBFormalAbstractTimeMeasurementFunction extends DBFormalAbstractMeasurementFunction {
    public DBFormalAbstractTimeMeasurementFunction(AbstractLoad ins_load, AbstractLoad sel_load) {
        super();
        ArrayList<AbstractLoad> l = new ArrayList<>();
        l.add(ins_load);
        l.add(sel_load);
        super.setLoads(l);
    }
}