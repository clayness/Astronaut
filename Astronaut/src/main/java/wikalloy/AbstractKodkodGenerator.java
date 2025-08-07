package wikalloy;

import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractKodkodGenerator {
    protected final Instance instance;

    protected AbstractKodkodGenerator(A4Solution solution) {
        assert solution.satisfiable();
        this.instance = solution.debugExtractKInstance();
    }

    public AbstractKodkodGenerator(Instance instance) {
        this.instance = instance;
    }

    protected Object getAtom(String s) {
        return StreamSupport.stream(this.instance.universe().spliterator(), false)
                .filter(o -> o.toString().equals(s))
                .findFirst().orElse(null);
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
