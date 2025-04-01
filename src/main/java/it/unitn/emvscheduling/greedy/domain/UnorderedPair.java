package it.unitn.emvscheduling.greedy.domain;

import java.util.Objects;

/**
 * this implementation of pair, ignores the order and (x,y) is equal to (y,x)
 */
public class UnorderedPair implements Comparable<UnorderedPair> {
    public int i;
    public int j;

    // Override equals to ignore order
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnorderedPair unorderedPair = (UnorderedPair) o;

        // Compare both ways to ignore order
        return (Objects.equals(i, unorderedPair.i) && Objects.equals(j, unorderedPair.j)) ||
                (Objects.equals(i, unorderedPair.j) && Objects.equals(j, unorderedPair.i));
    }

    //ignore order
    @Override
    public int hashCode() {
        int hashFirst = Objects.hash(i);
        int hashSecond = Objects.hash(j);

        // Combine the hash codes using a combination of sum and product
        return hashFirst + hashSecond + 31 * hashFirst * hashSecond;
    }

    public void setValues(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public UnorderedPair() {
    }

    public UnorderedPair(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int compareTo(UnorderedPair other) {
        int cmpFirst = Integer.compare(this.i, other.i);
        return (cmpFirst != 0) ? cmpFirst : Integer.compare(this.j, other.j);
    }
}
