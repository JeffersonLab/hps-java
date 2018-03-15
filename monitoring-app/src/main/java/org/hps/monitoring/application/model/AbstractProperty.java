package org.hps.monitoring.application.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class AbstractProperty<T> implements PropertyChangeListener {
    
    private String name;
    private String description;
    private T defaultValue = null;
    protected T value = null;
    
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
      
    public AbstractProperty(String name, String description, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.value = this.defaultValue;
    }
                 
    public T getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
    
    public void addPropertyChangeListener(final PropertyChangeListener propertyChangedListener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyChangedListener);
    }
    
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
        
    /**
     * Send new property to a listener, such as a GUI component.
     */
    protected void firePropertyChanged(T oldValue, T newValue) {
        this.propertyChangeSupport.firePropertyChange(name, oldValue, newValue);
    }
    
    void firePropertyChanged() {
        this.propertyChangeSupport.firePropertyChange(name, this.value, this.value);
    }
    
    /**
     * Receive new property value e.g. from a GUI component.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Property " + name + " received property change with newValue '" + evt.getNewValue().toString() + "' from " + evt.getSource());
        this.setRawValue(evt.getNewValue().toString());
    }
    
    public void setValue(T value) {
        T oldValue = this.value;
        this.value = value;
        this.firePropertyChanged(oldValue, this.value);
    }
    
    /**
     * Conversion method from a string, to be implemented by concrete type.
     */
    abstract public void setRawValue(String value);
}
