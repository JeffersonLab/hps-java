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
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class AbstractModel {

    /**
     * This method will statically extract property names from a class, which in this package's conventions are
     * statically declared, public strings that end with "_PROPERTY".
     *
     * @param type The class with the properties.
     * @return The list of property names.
     */
    protected static String[] getPropertyNames(final Class<? extends AbstractModel> type) {
        final List<String> fields = new ArrayList<String>();
        for (final Field field : type.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
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

    protected PropertyChangeSupport propertyChangeSupport;

    public AbstractModel() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void fireModelChanged() {
        firePropertiesChanged(Arrays.asList(getPropertyNames()));
    }

    void firePropertiesChanged(final Collection<String> properties) {
        propertyLoop: for (final String property : properties) {
            Method getMethod = null;
            for (final Method method : getClass().getMethods()) {
                if (method.getName().equals("get" + property)) {
                    getMethod = method;
                    break;
                }
            }
            // System.out.println(getMethod.getName());
            try {
                Object value = null;
                try {
                    value = getMethod.invoke(this, (Object[]) null);
                    // System.out.println("  value = " + value);
                } catch (final NullPointerException e) {
                    // This means there is no get method for the property which is a throwable error.
                    throw new RuntimeException("Property " + property + " is missing a get method.", e);
                } catch (final InvocationTargetException e) {
                    // Is the cause of the problem an illegal argument to the method?
                    if (e.getCause() instanceof IllegalArgumentException) {
                        // For this error, assume that the key itself is missing from the configuration which is a
                        // warning.
                        System.err.println("The key " + property + " is not set in the configuration.");
                        continue propertyLoop;
                    } else {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                if (value != null) {
                    firePropertyChange(property, value, value);
                    for (final PropertyChangeListener listener : this.propertyChangeSupport
                            .getPropertyChangeListeners()) {
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

    protected void firePropertyChange(final PropertyChangeEvent evt) {
        this.propertyChangeSupport.firePropertyChange(evt);
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        this.propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    abstract public String[] getPropertyNames();

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
}