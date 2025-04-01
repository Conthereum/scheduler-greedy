package it.unitn.emvscheduling.greedy.domain;

import java.util.Objects;


/**
 * this implementation of pair, considers the order (i,j) is NOT equal to (j, i)
 */
public class OrderedPair<T> {
    public T first;
    public T second;

    public OrderedPair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ')';
    }

    // Override equals to ignore order
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderedPair<?> pair = (OrderedPair<?>) o;

        // Compare both ways to ignore order
        return (Objects.equals(first, pair.first) && Objects.equals(second, pair.second));
    }

    //ignore order
    @Override
    public int hashCode() {
        int hashFirst = Objects.hash(first);
        int hashSecond = Objects.hash(second);

        // Combine the hash codes using a combination of sum and product
        return hashFirst + hashSecond + 31 * hashFirst * hashSecond;
    }

    public void setValues(T i, T j) {
        this.first = i;
        this.second = j;
    }
}
