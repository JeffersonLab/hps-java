package org.hps.conditions;

import org.lcsim.conditions.ConditionsConverter;

/**
 * This class is basically just a typedef right now but functionality may be added here
 * that all converters can use.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class DatabaseConditionsConverter<T> implements ConditionsConverter<T> {   
	
    /**
     * Get the the {@link ConnectionManager} associated with this converter.
     * For now, this calls the singleton method of the ConnectionManager
     * to get its instance.
     * @return The ConnectionManager of this converter.
     */
    public ConnectionManager getConnectionManager() {
        return ConnectionManager.getConnectionManager();
    }
}