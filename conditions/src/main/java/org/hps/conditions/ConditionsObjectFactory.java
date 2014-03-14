package org.hps.conditions;

import org.hps.conditions.AbstractConditionsDatabaseObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;

/**
 * This is the primary interface in the API for generically instantiating {@link ConditionsObject} objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsObjectFactory {
        
    /**
     * Create a <code>ConditionsObject</code> generically, given a concrete class,
     * an associated table in the conditions database, a collection ID (which may be -1 for a new object),
     * and a map of field values.
     * 
     * @param klass The concrete Class to be instantiated, which must have a zero argument constructor.
     * @param tableName The name of the table in the conditions database.
     * @param collectionId The unique collection 
     * @param fieldValues The field values of the object.
     * @return The new ConditionsObject with concrete type <code>klass</code>.
     * @throws ConditionsObjectException if there is a problem creating the object.
     */
    public ConditionsObject createObject(
            Class<? extends ConditionsObject> klass, 
            String tableName, 
            int collectionId,
            FieldValueMap fieldValues) throws ConditionsObjectException;    
    
    /**
     * Get the registry of table meta data used by this factory.
     * @return The registry of table meta data.
     */
    public ConditionsTableRegistry getTableRegistry();
}
