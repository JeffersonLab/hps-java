package org.hps.online.recon.properties;

/**
 * A property with a long value
 */
public class LongProperty extends Property<Long> {

    public LongProperty(String name, String description, Long defaultValue, boolean required) {
        super(name, Long.class, description, defaultValue, required);
    }

    public LongProperty(LongProperty p) {
        super(p.name, Long.class, p.description, p.value, p.required);
    }

    @Override
    public void from(Object val) {
        value = Long.valueOf(val.toString());
    }

    @Override
    public Object clone() {
        return new LongProperty(this);
    }
}
