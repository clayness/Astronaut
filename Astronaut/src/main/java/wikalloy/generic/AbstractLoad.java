package wikalloy.generic;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AbstractLoad implements Serializable {

    private final Set<AbstractInst> instances = new HashSet<>();
    private final Set<AbstractAssoc> associations = new HashSet<>();
    private final Set<AbstractQuery> queries = new HashSet<>();
    private final UUID uuid = UUID.randomUUID();

    public Collection<AbstractAssoc> getAssociations() {
        return associations;
    }

    public Collection<AbstractInst> getInstances() {
        return instances;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void newAssociation(Object name, AbstractInst src, AbstractInst dst) {
        this.associations.add(new AbstractAssoc(name, src, dst));
    }

    public AbstractInst newInstance(Object type) {
        var i = new AbstractInst(type);
        this.instances.add(i);
        return i;
    }

    public Collection<AbstractQuery> getQueries() {
        return queries;
    }

    public void newQuery(Object type, AbstractAssoc association, Collection<AbstractFilter> filters) {
        this.queries.add(new AbstractQuery(type, filters, association));
    }

}
