package edu.virginia.cs.Framework.Types;

/**
 * @author tang
 * AbstractQuery is a pair of action and object
 */
public class AbstractQuery {

    public enum Action {
        INSERT, SELECT, UPDATE
    }

    private final Action action;
    private final ObjectOfDM oodm;

    public AbstractQuery(Action a, ObjectOfDM oodm) {
        this.action = a;
        this.oodm = oodm;
    }

    public Action getAction() {
        return this.action;
    }

    public ObjectOfDM getOodm() {
        return oodm;
    }

}
