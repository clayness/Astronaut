package edu.virginia.cs.Framework.Types;

import java.util.ArrayList;
import java.util.List;

public class DBFormalAbstractTimeMeasurementFunction extends DBFormalAbstractMeasurementFunction {
    public DBFormalAbstractTimeMeasurementFunction(AbstractLoad ins_load, AbstractLoad sel_load) {
        List<AbstractLoad> l = new ArrayList<>();
        l.add(ins_load);
        l.add(sel_load);
        super.setLoads(l);
    }
}