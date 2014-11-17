package org.hps.conditions.api;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a series of collections containing
 * <tt>ConditionsObjects</tt>. It is used to conditions collections when there
 * are multiple ones with the same key that are valid for the current run.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings("rawtypes")
public class ConditionsSeries {

    List<ConditionsObjectCollection> collections = new ArrayList<ConditionsObjectCollection>();

    public ConditionsObjectCollection getCollection(int series) {
        return collections.get(series);
    }

    public int addCollection(ConditionsObjectCollection collection) {
        if (collections.contains(collection))
            throw new IllegalArgumentException("The collection is already registered with this object.");
        collections.add(collection);
        return collections.indexOf(collection);
    }

    public int getNumberOfCollections() {
        return collections.size();
    }

    public ConditionsObjectCollection findCollection(ConditionsRecord record) {
        for (ConditionsObjectCollection collection : collections) {
            if (collection.conditionsRecord == record)
                return collection;
        }
        return null;
    }
    
    public List<ConditionsObjectCollection> getCollections() {
        return collections;
    }
}
