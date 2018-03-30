package org.hps.monitoring.application.model;

public final class PropertyTypes {
    
    private PropertyTypes() {
    }
    
    public static class StringProperty extends AbstractProperty<String> {

        public StringProperty(String name, String description, String defaultValue) {
            super(name, defaultValue, description);
        }       
        
        public void setValue(String string) {            
            String newValue = string;
            String oldValue = this.value;
            this.value = newValue;
            this.firePropertyChanged(newValue, oldValue);
        }
        
        public void setRawValue(String rawValue) {
            this.setValue(rawValue);
        }
    }
    
    public static class IntegerProperty extends AbstractProperty<Integer> {

        public IntegerProperty(String name, String description, Integer defaultValue) {
            super(name, description, defaultValue);
        }       
        
        public void setRawValue(String rawValue) {
            this.setValue(Integer.valueOf(rawValue));
        }
    }
    
    public static class DoubleProperty extends AbstractProperty<Double> {

        public DoubleProperty(String name, String description, Double defaultValue) {
            super(name, description, defaultValue);
        }
        
        public void setRawValue(String rawValue) {
            this.setValue(Double.valueOf(rawValue));
        }
    }
    
    public static class BooleanProperty extends AbstractProperty<Boolean> {

        public BooleanProperty(String name, String description, Boolean defaultValue) {
            super(name, description, defaultValue);
        }
                   
        public void setRawValue(String rawValue) {
            this.setValue(Boolean.valueOf(rawValue));
        }
    }
    
    public static class LongProperty extends AbstractProperty<Long> {

        public LongProperty(String name, String description, Long defaultValue) {
            super(name, description, defaultValue);
        }
        
        public void setRawValue(String rawValue) {
            this.setValue(Long.valueOf(rawValue));
        }
    }
}
