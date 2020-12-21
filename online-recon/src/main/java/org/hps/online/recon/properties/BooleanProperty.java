package org.hps.online.recon.properties;

public class BooleanProperty extends Property<Boolean> {

    public BooleanProperty(String name, String description, Boolean defaultValue, boolean required) {
        super(name, Boolean.class, description, defaultValue, required);
    }

    public BooleanProperty(BooleanProperty p) {
        super(p.name, Boolean.class, p.description, p.value, p.required);
    }

    @Override
    public void from(Object val) {
        value = Boolean.valueOf(val.toString());
    }

    @Override
    public Object clone() {
        return new BooleanProperty(this);
    }
}
