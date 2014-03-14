package org.hps.conditions;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;

/**
 * This is the primary interface in the API for generically instantiating {@link ConditionsObject} objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsObjectFactory {
        
    /**
     * Create a <code>ConditionsObject</code> generically, given a concrete class,
     * an associated table in the conditions database, and a map of field values.
     * 
     * The collection ID should be assigned externally to this method by adding it 
     * to a <code>ConditionsObjectCollection</code>.
     * 
     * @param klass The concrete Class to be instantiated, which must have a zero argument constructor.
     * @param tableName The name of the table in the conditions database.
     * @param collectionId The unique collection ID which should be set to -1 for a new collection.
     * @param fieldValues The field values of the object.
     * @return The new ConditionsObject with concrete type <code>klass</code>.
     * @throws ConditionsObjectException if there is a problem creating the object.
     */
    public <T> T createObject(
            Class<? extends ConditionsObject> klass,
            String tableName,
            int rowId,
            FieldValueMap fieldValues,
            boolean isReadOnly) throws ConditionsObjectException;
    
    /**
     * Get the registry of table meta data used by this factory.
     * @return The registry of table meta data.
     */
    public ConditionsTableRegistry getTableRegistry();
}
