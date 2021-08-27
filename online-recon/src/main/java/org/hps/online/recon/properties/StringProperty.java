package org.hps.online.recon.properties;

/**
 * Property with a string value
 */
public class StringProperty extends Property<String> {

    public StringProperty(String name, String description, String defaultValue, boolean required) {
        super(name, String.class, description, defaultValue, required);
    }

    public StringProperty(StringProperty p) {
        super(p.name, String.class, p.description, p.value, p.required);
    }

    public void from(Object obj) {
        value = obj.toString();
    }

    @Override
    public Object clone() {
        return new StringProperty(this);
    }
}
