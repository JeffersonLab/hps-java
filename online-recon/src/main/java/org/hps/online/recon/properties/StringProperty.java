package org.hps.online.recon.properties;

public class StringProperty extends Property<String> {

    public StringProperty(String name, String description, String defaultValue, boolean required) {
        super(name, String.class, description, defaultValue, required);
    }

    @Override
    public void convert(Object val) {
        set(val.toString());
    }
}
