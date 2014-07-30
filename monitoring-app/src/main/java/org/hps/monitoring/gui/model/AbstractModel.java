package org.hps.monitoring.gui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstract class that updates listeners of property changes in backing model.
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
    
    abstract public String[] getProperties();
    
    public void fireAllChanged() {
        //System.out.println("AbstractModel.fireAllChanged");
        //System.out.println("  listeners: ");
        //for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
        //    System.out.print(listener.getClass().getCanonicalName() + " ");
        //}
        System.out.println();
        for (String property : getProperties()) {
            Method getMethod = null;
            for (Method method : getClass().getMethods()) {
                if (method.getName().equals("get" + property)) {
                    getMethod = method;
                    break;
                }
            }
            try {
                //System.out.println("firePropertyChange");
                //System.out.println("  property: " + property);
                //System.out.println("  getMethod: " + getMethod.getName());
                Object value = getMethod.invoke(this, null);
                //System.out.println("  value: " + value);
                if (value != null) {
                    firePropertyChange(property, value, value);                    
                    for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
                        //System.out.println("  propertyChange: " + listener.getClass().getCanonicalName());
                        listener.propertyChange(new PropertyChangeEvent(this, property, value, value));
                    }
                    //firePropertyChange(new PropertyChangeEvent(this, property, value, value));
                } else {
                    System.err.println("WARNING: AbstractModel.fireAllChanged - missing property " + property);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }            
        }
    }        
}