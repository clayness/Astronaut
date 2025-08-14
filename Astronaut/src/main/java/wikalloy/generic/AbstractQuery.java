package wikalloy.generic;

import wikalloy.kodkod.KodkodAtom;

import java.io.Serializable;
import java.util.Collection;

public class AbstractQuery extends KodkodAtom implements Serializable {

    private final AbstractAssoc association;

    private final Collection<AbstractFilter> filters;

    public AbstractQuery(Object type, Collection<AbstractFilter> filters, AbstractAssoc association) {
        super(type);
        this.filters = filters;
        this.association = association;
    }

    public AbstractAssoc getAssociation() {
        return this.association;
    }

    public Collection<AbstractFilter> getFilters() {
        return this.filters;
    }
}
