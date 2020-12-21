package org.hps.online.recon.properties;

public class IntegerProperty extends Property<Integer> {

    public IntegerProperty(String name, String description, Integer defaultValue, boolean required) {
        super(name, Integer.class, description, defaultValue, required);
    }

    public IntegerProperty(IntegerProperty p) {
        super(p.name, Integer.class, p.description, p.value, p.required);
    }

    @Override
    public void from(Object val) {
        value = Integer.valueOf(val.toString());
    }

    @Override
    public Object clone() {
        return new IntegerProperty(this);
    }
}
