package edu.virginia.cs.Framework.Types;

import edu.virginia.cs.AppConfig;
import edu.virginia.cs.Synthesizer.CodeNamePair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DBConcreteMeasurementFunction implements Serializable {
    private Map<String, Map<String, List<CodeNamePair>>> instances;

    MeasurementFunctionByDB mfByDB = null;

    public DBConcreteMeasurementFunction() {
        if (AppConfig.getTestDB().equalsIgnoreCase("mysql")) {
            this.mfByDB = new MySQLMeasurementFunction();
        } else if (AppConfig.getTestDB().equalsIgnoreCase("postgres")) {
            this.mfByDB = new PostgresMeasurementFunction();
        } else {
            System.out.println("DBConcreteMeasurementFunction: error...");
            System.out.println("DBConcreteMeasurementFunction: Non-supported RDBMS");
            System.exit(-1);
        }
    }

    public Map<String, Map<String, List<CodeNamePair>>> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, Map<String, List<CodeNamePair>>> ins) {
        this.instances = ins;
    }

    public void setImpl(DBImplementation impl) {
        this.mfByDB.setImpl(impl);
    }

    public List<ConcreteLoad> getLoads() {
        return this.mfByDB.getLoads();
    }

    public void setLoads(List<ConcreteLoad> loads) {
        this.mfByDB.setLoads(loads);
    }

}

