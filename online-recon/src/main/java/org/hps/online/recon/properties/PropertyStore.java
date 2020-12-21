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

    private final Map<String, Property<?>> props = new HashMap<String, Property<?>>();

    public PropertyStore() {
    }

    public PropertyStore(PropertyStore store) {
        for (Property<?> p : props.values()) {
            store.add(p);
        }
    }

    public boolean has(String name) {
        return props.containsKey(name);
    }

    public final void add(Property<?> prop) {
        if (props.containsKey(prop.name())) {
            throw new IllegalArgumentException("Property already exists: " + prop.name());
        }
        props.put(prop.name(), prop);
    }

    public final void add(Property<?> propArr[]) {
        for (Property<?> prop : propArr) {
            add(prop);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> Property<T> get(String name) {
        return (Property<T>) props.get(name);
    }

    public final void loadDefaults() {
        for (Entry<String, Property<?>> entry : props.entrySet()) {
            entry.getValue().fromDefault();
        }
    }

    public final void load(Properties propFile) {
        for (Object keyObj : propFile.keySet()) {
            String key = keyObj.toString();
            if (props.containsKey(key)) {
                Property<?> prop = props.get(key);
                Object rawVal = propFile.get(key);
                prop.convert(rawVal);
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

    public Object clone() {
        return new PropertyStore(this);
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
        for (Object ko : this.props.keySet()) {
            jo.put((String) ko, this.props.get(ko));
        }
        return jo;
    }

    public void save(Properties propOut) {
        for (Entry<String, Property<?>> entry : this.props.entrySet()) {
            propOut.setProperty(entry.getKey(), entry.getValue().value().toString());
        }
    }

    public void write(File file, String comment) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        save(props);
        props.store(new FileOutputStream(file), comment);
    }

    public void validate() {
        List<String> badProps = new ArrayList<String>();
        for (Entry<String, Property<?>> entry : this.props.entrySet()) {
            Property<?> prop = entry.getValue();
            if (!prop.valid()) {
                badProps.add(prop.name());
            }
        }
        if (badProps.size() > 0) {
            String msg = String.join(" ", badProps);
            throw new RuntimeException("The following properties are not valid: " + msg);
        }
    }
}
