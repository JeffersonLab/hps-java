package org.hps.analysis.trigger.util;

/**
 * Class <code>Pair</code> represents a pair of two objects.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <E> - The object type of the first element in the pair.
 * @param <F> - The object type of the second element in the pair.
 */
public class Pair<E, F> {
    private final E firstObject;
    private final F secondObject;
    
    /**
     * Creates a pair of the two indicated objects.
     * @param firstElement - The first object.
     * @param secondElement - The second object.
     */
    public Pair(E firstElement, F secondElement) {
        this.firstObject = firstElement;
        this.secondObject = secondElement;
    }
    
    /**
     * Gets the first element of the pair.
     * @return Returns the first element.
     */
    public E getFirstElement() {
        return firstObject;
    }
    
    /**
     * Gets the second element of the pair.
     * @return Returns the second element.
     */
    public F getSecondElement() {
        return secondObject;
    }
}
