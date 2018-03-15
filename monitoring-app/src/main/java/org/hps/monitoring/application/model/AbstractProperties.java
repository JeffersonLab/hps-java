package org.hps.monitoring.application.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public abstract class AbstractProperties {
    
    Map<String, AbstractProperty<?>> properties = new HashMap<String, AbstractProperty<?>>();
    
    public void add(AbstractProperty<?> newProperty) {
        properties.put(newProperty.getName(), newProperty);
    }
    
    public Set<String> getPropertyNames() {
        List<String> names = new ArrayList<String>(properties.keySet());
        Collections.sort(names);
        return new HashSet<String>(names);
    }
    
    public <T> AbstractProperty<T> get(String name) {
        return (AbstractProperty<T>) properties.get(name);
    }
    
    public boolean hasProperty(String name) {
        return properties.keySet().contains(name);
    }
    
    public boolean isValidProperty(String name) {
        return hasProperty(name) && this.get(name).getValue() != null;
    }
    
    public void load(InputStream is) throws IOException {
        Properties rawProperties = new Properties();
        try {
            rawProperties.load(is);
        } catch (final IOException e) {
            throw new IOException("Error loading properties from input stream.");
        }
        for (Object key : rawProperties.keySet()) {
            String name = (String) key;
            if (this.hasProperty(name)) {
                this.get(name).setValue(rawProperties.getProperty(name));
            }
        }
    }
    
    public void load(File file) throws FileNotFoundException, IOException {
        this.load(new FileInputStream(file));
    }
    
    public void load(String resource) throws IOException {
        this.load(this.getClass().getResourceAsStream(resource));
    }
    
    public void write(File file) throws IOException {
        Properties rawProperties = new Properties();
        for (String name : this.getPropertyNames()) {
            rawProperties.setProperty(name, this.get(name).getValue().toString());
        }        
        rawProperties.store(new FileOutputStream(file), null);
    }
}
