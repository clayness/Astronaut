package wikalloy.generic;

import wikalloy.kodkod.KodkodAtom;

import java.util.Collection;
import java.util.Map;

public class AbstractQuery extends KodkodAtom {

    private final AbstractLoad.AbstractAssoc association;

    private final Collection<AbstractFilter> filters;

    public AbstractQuery(Object type, Collection<AbstractFilter> filters, AbstractLoad.AbstractAssoc association) {
        super(type);
        this.filters = filters;
        this.association = association;
    }

    public AbstractLoad.AbstractAssoc getAssociation() {
        return this.association;
    }

    public Collection<AbstractFilter> getFilters() {
        return this.filters;
    }
}
