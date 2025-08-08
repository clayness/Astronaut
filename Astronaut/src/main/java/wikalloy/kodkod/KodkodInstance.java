package wikalloy.kodkod;

import edu.mit.csail.sdg.translator.A4Solution;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class KodkodInstance {
    protected final Instance instance;

    protected KodkodInstance(A4Solution solution) {
        assert solution.satisfiable();
        this.instance = solution.debugExtractKInstance();
    }

    protected Object getAtom(String s) {
        return StreamSupport.stream(this.instance.universe().spliterator(), false)
                .filter(o -> o.toString().equals(s))
                .findFirst().orElse(null);
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

    protected boolean inX(KodkodAtom kkObj, String relationName) {
        return this.join(relationName, this.getAtom("x/" + kkObj.getAtom()), 0)
                .findFirst().isPresent();
    }

    protected Stream<Tuple> join(String relationName, Object atom, int i) {
        return getTuples(relationName).filter(t -> t.atom(i) == atom);
    }
}
