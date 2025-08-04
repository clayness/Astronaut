package wikalloy;

import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractKodkodGenerator {
    protected final Instance instance;

    protected AbstractKodkodGenerator(A4Solution solution) {
        assert solution.satisfiable();
        this.instance = solution.debugExtractKInstance();
    }


    protected Tuple getTuple(Object atom) {
        return this.instance.universe().factory().tuple(atom);
    }

    protected Stream<Tuple> getTuples(String relationName) {
        var r = this.instance.findRelationByName(relationName);
        if (r == null) {
            return Stream.empty();
        } else {
            return Optional.ofNullable(this.instance.tuples(r))
                    .orElse(this.instance.universe().factory().noneOf(r.arity()))
                    .stream();
        }
    }

    protected boolean in(String relation, Tuple t) {
        var r = this.instance.findRelationByName(relation);
        if (r == null) {
            return false;
        } else {
            return Optional.ofNullable(this.instance.tuples(r))
                    .orElse(this.instance.universe().factory().noneOf(r.arity()))
                    .contains(t);
        }
    }

    protected Stream<Tuple> join(String relationName, Object atom, int i) {
        return getTuples(relationName).filter(t -> t.atom(i) == atom);
    }

    protected String name(Object atom) {
        return atom.toString().replaceAll("[/$]", "_");
    }

    protected Set<Tuple> tc(final String relationName) {
        var ts = this.getTuples(relationName).collect(Collectors.toSet());
        //noinspection StatementWithEmptyBody
        while (ts.addAll(ts.stream()
                .flatMap(t -> this.join(relationName, t.atom(1), 0))
                .collect(Collectors.toSet()))) {
            /* no-op */
        }
        return ts;
    }
}
