/**
 * 
 */
package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Set;

import org.hps.conditions.database.TableMetaData;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsObjectCollection<ObjectType extends ConditionsObject> extends Set<ObjectType> {

    /**
     * Get the table meta data.
     * @return
     */
    public TableMetaData getTableMetaData();

    /**
     * Get the collection ID.
     * @return
     */
    public int getCollectionId();
    
    /**
     * Get the conditions record.
     * @return
     */
    public ConditionsRecord getConditionsRecord();
        
    /**
     * Set the collection ID.  
     * Once set it cannot be assign again, which will cause an exception.
     * @param collectionId
     * @throws ConditionsObjectException
     */
    public void setCollectionId(int collectionId) throws ConditionsObjectException;
        
    public void insert() throws ConditionsObjectException, SQLException;
    
    public int select();
    
    public int delete();
    
    public int update();
    
    /**
     * Get an object by index.
     * @param index The index in the set.
     * @return The object at the index.
     * @throws IndexOutOfBoundsException If the index value is invalid.
     */
    public ObjectType get(int index);
    
    /**
     * Sort the collection in place.
     * @param comparator The comparator to use for sorting.
     */
    public void sort(Comparator<ObjectType> comparator);
    
    /**
     * Get a sorted list of the objects, leaving original collection in place.
     * @param comparator The comparator to use for the sort.
     * @return A sorted list of the objects.
     */
    public AbstractConditionsObjectCollection<ObjectType> sorted(Comparator<ObjectType> comparator);
    
    public void sort();
    
    public AbstractConditionsObjectCollection<ObjectType> sorted();
}
