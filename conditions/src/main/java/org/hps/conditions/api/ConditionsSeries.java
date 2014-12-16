package org.hps.conditions.api;

import java.util.ArrayList;

/**
 * This class represents a series of collections containing
 * <tt>ConditionsObjects</tt>. It is used to conditions collections when there
 * are multiple ones with the same key that are valid for the current run.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings("rawtypes")
public class ConditionsSeries extends ArrayList<AbstractConditionsObjectCollection> {

    public AbstractConditionsObjectCollection findCollection(ConditionsRecord record) {
        for (AbstractConditionsObjectCollection collection : this) {
            if (collection.conditionsRecord == record)
                return collection;
        }
        return null;
    }   
}
