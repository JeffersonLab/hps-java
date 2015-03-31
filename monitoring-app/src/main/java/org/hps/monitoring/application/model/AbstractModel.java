package org.hps.monitoring.application.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javassist.Modifier;

/**
 * An abstract class which updates a set of listeners when there are property changes to a backing model.
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

    void firePropertiesChanged(Collection<String> properties) {
        propertyLoop: for (String property : properties) {
            Method getMethod = null;
            for (Method method : getClass().getMethods()) {
                if (method.getName().equals("get" + property)) {
                    getMethod = method;
                    break;
                }
            }
            //System.out.println(getMethod.getName());
            try {
                Object value = null;
                try {
                    value = getMethod.invoke(this, (Object[]) null);
                    //System.out.println("  value = " + value);
                } catch (NullPointerException e) {
                    // This means there is no get method for the property which is a throwable error.
                    throw new RuntimeException("Property " + property + " is missing a get method.", e);
                } catch (InvocationTargetException e) {
                    // Is the cause of the problem an illegal argument to the method?
                    if (e.getCause() instanceof IllegalArgumentException) {
                        // For this error, assume that the key itself is missing from the configuration which is a warning.
                        System.err.println("The key " + property + " is not set in the configuration.");
                        continue propertyLoop;
                    } else {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                if (value != null) {
                    firePropertyChange(property, value, value);
                    for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
                        // FIXME: For some reason calling the propertyChangeSupport methods directly here doesn't work!
                        listener.propertyChange(new PropertyChangeEvent(this, property, value, value));
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    public void fireModelChanged() {
        firePropertiesChanged(Arrays.asList(getPropertyNames()));
    }

    /**
     * This method will statically extract property names from a class, which in this package's conventions are statically declared, 
     * public strings that end with "_PROPERTY".
     * 
     * @param type The class with the properties.
     * @return The list of property names.
     */
    protected static String[] getPropertyNames(Class<? extends AbstractModel> type) {
        List<String> fields = new ArrayList<String>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && field.getName().endsWith("_PROPERTY")) {
                try {
                    fields.add((String) field.get(null));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return fields.toArray(new String[] {});
    }
}