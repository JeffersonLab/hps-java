package org.hps.monitoring.gui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstract class that updates listeners from property changes in a backing model object.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractModel {

    protected PropertyChangeSupport propertyChangeSupport;

    public AbstractModel() {
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    protected void firePropertyChange(PropertyChangeEvent evt) {
        propertyChangeSupport.firePropertyChange(evt);
    }
    
    abstract public String[] getPropertyNames();
    
    // FIXME: This method is kind of a hack.  Any other good way to do this?
    public void fireAllChanged() {
        for (String property : getPropertyNames()) {
            Method getMethod = null;
            for (Method method : getClass().getMethods()) {
                if (method.getName().equals("get" + property)) {
                    getMethod = method;
                    break;
                }
            }
            try {
                Object value = getMethod.invoke(this, null);
                if (value != null) {
                    firePropertyChange(property, value, value);                    
                    for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
                        // FIXME: For some reason calling the propertyChangeSupport methods directly here doesn't work!!!                        
                        listener.propertyChange(new PropertyChangeEvent(this, property, value, value));
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }            
        }
    }        
}