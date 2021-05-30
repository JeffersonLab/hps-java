package org.hps.online.recon.properties;

/**
 * Property with name, value, description and default value
 *
 * @param <T> The type of the property's value
 */
public abstract class Property<T> {

    protected String name;
    protected String description;
    protected T value;
    protected Class<T> type;
    protected boolean required;

    public Property(String name, Class<T> type, String description, T value, boolean required) {
        if (name == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Property name contains one or more spaces: " + name);
        }
        this.name = name;
        this.value = value;
        if (description == null) {
            throw new IllegalArgumentException("Property description is null");
        }
        this.description = description;
        this.required = required;
    }

    @SuppressWarnings({ "unchecked" })
    public Property(Property<?> p) {
        name = p.name;
        description = p.description;
        value = (T) p.value;
        type = (Class<T>) p.type;
        required = p.required;
    }

    public T value() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
    }

    public void from(Object val) {
        throw new UnsupportedOperationException("Object conversion not implemented");
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    public boolean valid() {
        return value != null;
    }

    public String description() {
        return description;
    }

    public boolean required() {
        return required;
    }

    abstract public Object clone();
}
