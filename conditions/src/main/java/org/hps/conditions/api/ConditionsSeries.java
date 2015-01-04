package org.hps.conditions.api;

import java.util.ArrayList;


/**
 * This class represents a series of collections containing
 * <tt>ConditionsObjects</tt>. It is used to conditions collections when there
 * are multiple ones with the same key that are valid for the current run.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings({ "serial" })
public class ConditionsSeries<ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> extends ArrayList<ConditionsObjectCollection<ObjectType>> {

    Class<CollectionType> collectionType;
    Class<ObjectType> objectType;
    
    public ConditionsSeries(Class<ObjectType> objectType, Class<CollectionType> collectionType) {
        this.collectionType = collectionType;
        this.objectType = objectType;
    }
}