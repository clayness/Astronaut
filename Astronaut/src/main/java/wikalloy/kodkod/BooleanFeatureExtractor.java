package wikalloy.kodkod;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;

import java.util.ArrayList;
import java.util.List;

public class BooleanFeatureExtractor {

    public List<String> extractFeatures(A4Solution solution, Bounds bounds) throws Err {
        // Extract the Kodkod instance
        return extractFeatures(solution.debugExtractKInstance(), bounds);
    }

    public List<String> extractFeatures(Instance instance, Bounds bounds) throws Err {
        List<String> features = new ArrayList<>();
        // Iterate over all relations in the bounds
        RelationOrder.relations(bounds).forEachOrdered(relation -> {
            // Check each tuple in the upper bound (all possible tuples for the relation)
            TupleSet free = bounds.upperBound(relation).clone();
            free.removeAll(bounds.lowerBound(relation));
            for (Tuple tuple : free) {
                // If the tuple exists in the solution, assign "1"; otherwise, "0"
                var tuples = instance.tuples(relation.name());
                if (tuples != null && tuples.contains(tuple)) {
                    features.add("1");
                } else {
                    features.add("0");
                }
            }
        });
        return features;
    }

}
