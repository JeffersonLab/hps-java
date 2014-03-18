package org.hps.conditions;

import org.lcsim.conditions.ConditionsConverter;

/**
 * The abstract base class for database conditions converters to extend.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class DatabaseConditionsConverter<T> implements ConditionsConverter<T> {   
	    
    protected ConditionsObjectFactory _objectFactory;
    
    public DatabaseConditionsConverter(ConditionsObjectFactory objectFactory) {
        _objectFactory = objectFactory;
    }
    
    /**
     * Get the the {@link ConnectionManager} associated with this converter.
     * For now, this calls the singleton method of the ConnectionManager
     * to get its instance.
     * @return The ConnectionManager of this converter.
     */
    protected ConnectionManager getConnectionManager() {
        return ConnectionManager.getConnectionManager();
    }
        
    protected void setObjectFactory(ConditionsObjectFactory objectFactory) {
        _objectFactory = objectFactory;
    }               
    
    public final ConditionsTableMetaData getTableMetaData(String tableName) {
        return _objectFactory.getTableRegistry().getTableMetaData(tableName);
    }
}