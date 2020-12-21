package org.hps.online.recon.properties;

public class BooleanProperty extends Property<Boolean> {

    public BooleanProperty(String name, String description, Boolean defaultValue, boolean required) {
        super(name, Boolean.class, description, defaultValue, required);
    }

    @Override
    public void convert(Object val) {
        set(Boolean.valueOf(val.toString()));
    }
}
