package edu.virginia.cs.Uniq;

import java.io.Serializable;

public class Pair<T> implements Serializable {
    private T first;
    private T second;

    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return this.first;
    }

    public void setFirst(T val) {
        this.first = val;
    }

    public T getSecond() {
        return this.second;
    }

    public void setSecond(T val) {
        this.second = val;
    }
}
