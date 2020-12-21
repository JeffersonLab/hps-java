package org.hps.online.recon.properties;

public class Property<T> {

    private String name;
    private String description;
    private T value;
    private T defaultValue;
    private Class<T> type;
    private boolean required;

    public Property(String name, Class<T> type, String description, T defaultValue, boolean required) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
    }

    @SuppressWarnings({ "unchecked" })
    public Property(Property<?> p) {
        name = p.name;
        description = p.description;
        value = (T) p.value;
        defaultValue = (T) p.defaultValue;
        type = (Class<T>) p.type;
        required = p.required;
    }

    public T value() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
    }

    public void convert(Object val) {
        throw new UnsupportedOperationException("Conversion not implemented");
    }

    public String name() {
        return name;
    }

    public boolean isDefault() {
        return value == defaultValue;
    }

    public void fromDefault() {
        this.value = defaultValue;
    }

    public Class<T> type() {
        return this.type;
    }

    public boolean valid() {
        return this.value != null;
    }

    public String description() {
        return this.description;
    }

    public boolean required() {
        return required;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object clone() {
        return new Property(this);
    }
}
