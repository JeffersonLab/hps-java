package org.hps.online.recon.properties;

public class IntegerProperty extends Property<Integer> {

    public IntegerProperty(String name, String description, Integer defaultValue, boolean required) {
        super(name, Integer.class, description, defaultValue, required);
    }

    @Override
    public void convert(Object val) {
        this.set(Integer.valueOf(val.toString()));
    }
}
