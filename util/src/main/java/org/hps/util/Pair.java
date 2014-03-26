
package org.hps.util;


/**
 * A Class to hold a pair of immutable objects
 * 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: Pair.java,v 1.1 2012/03/26 07:05:28 omoreno Exp $
 */
public class Pair<T, S> implements Comparable<Pair<T, S>> {
    
    private final T firstElement;
    private final S secondElement;
    
    /**
     * Default constructor 
     * 
     * @param firstElement
     *      The first element in the pair
     * @param secondElement
     *      The second element in the pair
     */
    public Pair(T firstElement, S secondElement)
    {
        this.firstElement = firstElement;
        this.secondElement = secondElement;
    }
    
    /**
     * Get the first element in the pair
     * 
     * @return firstElement
     *      The first element in the pair
     */
    public T getFirstElement()
    {
        return firstElement;
    }
    
    /**
     * Get the second element in the pair
     * 
     * @return secondElement
     *      The second element in the pair
     */
    public S getSecondElement()
    {
        return secondElement;
    }
    
    /**
     * Compares this pair to the specified pair
     * 
     * @param pair
     *      The pair to compare to
     * 
     * @return returns 0, 1 or -1 if the hash code of the pair is equal, greater
     *         than or less than the specified pair
     */
    @Override
    public int compareTo(Pair<T,S> pair){
        if(pair != null){
            if(pair.equals(this)) return 0;
            else if(pair.hashCode() > this.hashCode()) return 1;
            else if(pair.hashCode() < this.hashCode()) return -1;
        }
        
        return -1;
    }

    /**
     * The hash code for the pair
     */
    @Override
    public int hashCode()
    {
        int hashCode = firstElement.hashCode() + (31*secondElement.hashCode());
        return hashCode;
    }

    /**
     * Checks if the elements in this pair are equal to the elements of the 
     * specified pair
     * 
     * @param obj
     *      The pair to compare to
     * @return true if both elements are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        
        if (obj == null) return false;
      
        if(obj.getClass() == Pair.class){
        
            final Pair<T, S> pair = (Pair<T, S>) obj;
            if(!this.firstElement.equals(pair.firstElement) 
               && (this.firstElement == null || !this.firstElement.equals(pair.firstElement))) return false;
            if (this.secondElement != pair.secondElement 
               && (this.secondElement == null || !this.secondElement.equals(pair.secondElement))) return false;
        }
        
        return true;
    }
}
