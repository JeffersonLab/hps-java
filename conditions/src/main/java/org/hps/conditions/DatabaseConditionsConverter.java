package org.hps.conditions;

import org.lcsim.conditions.ConditionsConverter;

/**
 * This class is basically just a typedef right now but functionality may be added here
 * that all converters can use.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class DatabaseConditionsConverter<T> implements ConditionsConverter<T> {   
	    
    protected ConditionsObjectFactory _objectFactory;
    
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
}