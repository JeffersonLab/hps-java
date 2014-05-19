package org.hps.conditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents a series of collections containing <tt>ConditionsObjects</tt>.  It 
 * is used to conditions collections when there are multiple ones with the same key that
 * are valid for the current run. 
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 * @param <CollectionType> The specific type of the <tt>ConditionsObjectCollection</tt> the series contains.
 */
public class ConditionsSeries<CollectionType extends ConditionsObjectCollection> implements Iterable<CollectionType> {
    
    List<CollectionType> collections = new ArrayList<CollectionType>();
    
    CollectionType getCollection(int series) {
        return collections.get(series);
    }
        
    int addCollection(CollectionType collection) {
        if (collections.contains(collection))
            throw new IllegalArgumentException("The collection is already registered with this object.");
        collections.add(collection);
        return collections.indexOf(collection);
    }
    
    int getNumberOfCollections() {
        return collections.size();
    }
    
    CollectionType findCollection(ConditionsRecord record) {
        for (CollectionType collection : collections) {
            if (collection.conditionsRecord == record)
                return collection;
        }
        return null;
    }
    
    public Iterator<CollectionType> iterator() {
        return collections.iterator();
    }
}
