package wikalloy.generic;

import wikalloy.kodkod.KodkodAtom;

public class AbstractFilter extends KodkodAtom {

    private final String operator;
    private final Object value;

    public AbstractFilter(Object field, String operator, Object value) {
        super(field);
        this.operator = operator;
        this.value = value;
    }

    public String getOperator() {
        return this.operator;
    }

    public Object getValue() {
        return this.value;
    }
}
