package org.hps.online.recon.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.JSONObject;

public class PropertyStore {

    protected final Map<String, Property<?>> props = new HashMap<String, Property<?>>();

    public PropertyStore() {
    }

    public final boolean has(String name) {
        return props.containsKey(name);
    }

    public final void add(Property<?> prop) {
        if (props.containsKey(prop.name())) {
            throw new IllegalArgumentException("Property already exists: " + prop.name());
        }
        props.put(prop.name(), prop);
    }

    public final void add(Property<?> arr[]) {
        for (Property<?> prop : arr) {
            add(prop);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> Property<T> get(String name) {
        if (!has(name)) {
            throw new IllegalArgumentException("Property does not exist: " + name);
        }
        return (Property<T>) props.get(name);
    }

    public final void load(Properties propFile) {
        for (Object keyObj : propFile.keySet()) {
            String key = keyObj.toString();
            if (has(key)) {
                Property prop = props.get(key);
                Object rawVal = propFile.get(key);
                prop.from(rawVal);
            }
        }
    }

    public final void load(File file) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        load(p);
    }

    public void fromJSON(JSONObject jo) {
        for (String key : jo.keySet()) {
            get(key).set(jo.get(key).toString());
        }
    }

    /**
     * Convert properties to JSON.
     * @return The converted JSON object
     */
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        for (Entry<String, Property<?>> entry : props.entrySet()) {
            jo.put((String) entry.getKey(), entry.getValue().value);
        }
        return jo;
    }

    public void save(Properties propOut) {
        for (Entry<String, Property<?>> entry : this.props.entrySet()) {
            if (entry.getValue().valid()) {
                propOut.setProperty(entry.getKey(), entry.getValue().value().toString());
            } else {
                System.out.println("Skipping invalid prop: " + entry.getKey());
            }
        }
    }

    public void write(File file, String comment) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        save(props);
        props.store(new FileOutputStream(file), comment);
    }

    public void validate() throws PropertyValidationException {
        List<Property<?>> badProps = new ArrayList<Property<?>>();
        for (Entry<String, Property<?>> entry : this.props.entrySet()) {
            Property<?> prop = entry.getValue();
            if (prop.required() && !prop.valid()) {
                badProps.add(prop);
            }
        }
        if (badProps.size() > 0) {
            throw new PropertyValidationException(badProps);
        }
    }

    public String toString() {
        return toJSON().toString();
    }

    public int size() {
        return this.props.size();
    }
}
