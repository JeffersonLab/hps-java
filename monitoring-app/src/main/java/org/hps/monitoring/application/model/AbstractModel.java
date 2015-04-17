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
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.Modifier;

import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * An abstract class which updates a set of listeners when there are property changes to a backing model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class AbstractModel {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER = LogUtil.create(AbstractModel.class.getName(), new DefaultLogFormatter(),
            Level.INFO);

    /**
     * This method will extract property names from a class, which in this package's conventions are statically
     * declared, public strings that end with the sub-string "_PROPERTY".
     *
     * @param type the class with the properties settings
     * @return the list of property names
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

    /**
     * The property change support object.
     */
    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * Class constructor.
     */
    public AbstractModel() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Add a property change listener.
     *
     * @param listener the property change listener
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Fire property change events.
     */
    public void fireModelChanged() {
        this.firePropertiesChanged(Arrays.asList(this.getPropertyNames()));
    }

    /**
     * Fire property change for a list of named properties.
     *
     * @param properties the list of property names
     */
    void firePropertiesChanged(final Collection<String> properties) {
        for (final String property : properties) {

            // Find the getter for this property.
            Method getMethod = null;
            for (final Method method : this.getClass().getMethods()) {
                if (method.getName().equals("get" + property)) {
                    getMethod = method;
                    break;
                }
            }

            // Is there a valid get method for the property?
            if (getMethod != null) {
                LOGGER.fine("property: " + property + ", method: " + getMethod.getName());
                try {
                    Object value = null;
                    try {
                        value = getMethod.invoke(this, (Object[]) null);
                    } catch (final InvocationTargetException e) {
                        // Is the cause of the problem an illegal argument to the method?
                        if (e.getCause() instanceof IllegalArgumentException) {
                            // Property key is not in the configuration (this should not happen under normal
                            // circumstances).
                            LOGGER.log(Level.WARNING, "Property key missing from configuration: " + property, e);
                            continue;
                        } else {
                            // Something else went wrong, which we assume is a fatal error.
                            LOGGER.log(Level.SEVERE, "Error setting property: " + property, e);
                            throw new RuntimeException("Error setting property: " + property, e);
                        }
                    }
                    if (value != null) {
                        this.firePropertyChange(property, value, value);
                        for (final PropertyChangeListener listener : this.propertyChangeSupport
                                .getPropertyChangeListeners()) {
                            // FIXME: For some reason calling the propertyChangeSupport methods directly here doesn't
                            // work!
                            listener.propertyChange(new PropertyChangeEvent(this, property, value, value));
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    // This should not usually happen.
                    LOGGER.log(Level.SEVERE, "Error setting property: " + property, e);
                    throw new RuntimeException("Error setting property: " + property, e);
                }
            } else {
                // There was no getter found for the property which is a non-fatal warning.
                LOGGER.log(Level.WARNING, "Unknown property in configuration: " + property);
            }
        }
    }

    /**
     * Fire a property change event.
     *
     * @param evt the property change event
     */
    protected void firePropertyChange(final PropertyChangeEvent evt) {
        this.propertyChangeSupport.firePropertyChange(evt);
    }

    /**
     * Fire a property change.
     *
     * @param propertyName the name of the property
     * @param oldValue the old property value
     * @param newValue the new property value
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        this.propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Get the property change support object.
     *
     * @return the property change support object
     */
    PropertyChangeSupport getPropertyChangeSupport() {
        return this.propertyChangeSupport;
    }

    /**
     * Get the list of the property names for this model.
     *
     * @return the list of the property names for this model
     */
    abstract public String[] getPropertyNames();

    /**
     * Remove a property change listener from the model.
     *
     * @param listener the property change listener to remove
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
