package wikalloy.kodkod;

import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.TupleSet;

import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RelationOrder {

    private static final String INT_PATTERN = ".*\\bInt\\b.*";

    /**
     * @param bounds Kodkod bounds
     * @return Total number of primary variables
     */
    public static int primaryVars(Bounds bounds) {
        return bounds.relations().stream().mapToInt(relation -> {
            TupleSet free = bounds.upperBound(relation).clone();
            free.removeAll(bounds.lowerBound(relation));
            return free.size();
        }).sum();
    }

    /**
     * Sorts the relations in the passed bounds object into a canonical order
     * to ensure that all the partial solution strings / feature arrays are
     * always in the same order and returns the result as a stream.
     *
     * @param bounds
     * @return Sorted list of relations
     */
    public static Stream<Relation> relations(Bounds bounds) {
        // sort the relations by name and arity so they are
        // always in a canonical order
        return bounds.relations().stream().sorted(
                        Comparator.comparing(Relation::name)
                                .thenComparing(Relation::arity))
                .filter(r -> !Pattern.matches(INT_PATTERN, r.name()));
    }
}
