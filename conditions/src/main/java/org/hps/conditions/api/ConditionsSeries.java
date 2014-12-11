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

    List<AbstractConditionsObjectCollection> collections = new ArrayList<AbstractConditionsObjectCollection>();

    public AbstractConditionsObjectCollection getCollection(int series) {
        return collections.get(series);
    }

    public int addCollection(AbstractConditionsObjectCollection collection) {
        if (collections.contains(collection))
            throw new IllegalArgumentException("The collection is already registered with this object.");
        collections.add(collection);
        return collections.indexOf(collection);
    }

    public int getNumberOfCollections() {
        return collections.size();
    }

    public AbstractConditionsObjectCollection findCollection(ConditionsRecord record) {
        for (AbstractConditionsObjectCollection collection : collections) {
            if (collection.conditionsRecord == record)
                return collection;
        }
        return null;
    }
    
    public List<AbstractConditionsObjectCollection> getCollections() {
        return collections;
    }
}
