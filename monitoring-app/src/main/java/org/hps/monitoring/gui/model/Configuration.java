package org.hps.monitoring.gui.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class provides a list of key, value pairs backed by a <code>Properties</code> object. The
 * getter and setter methods for these values are not public, because the 
 * {@link org.hps.monitoring.gui.model.ConfigurationModel} class should be used instead
 * to get or set application configuration values.
 */
public final class Configuration {

    Properties properties;
    File file;
    String resourcePath;

    Configuration() {
        properties = new Properties();
    }

    /**
     * Load a configuration from a properties file.
     * @param file The properties file.
     */
    public Configuration(File file) {
        this.file = file;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(this.file));
        } catch (IOException e) {
            throw new RuntimeException("Error parsing properties file.", e);
        }
    }

    /**
     * Load a configuration from a resource path pointing to a properties file.
     * @param resourcePath The resource path to the properties file.
     */
    public Configuration(String resourcePath) {
        this.resourcePath = resourcePath;
        InputStream is = this.getClass().getResourceAsStream(this.resourcePath);
        try {
            properties = new Properties();
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing properties resource.", e);
        }
    }

    /**
     * Get the file associated with this configuration or <code>null</code> if not set.
     * @return The file associated with the configuration.
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the resource path associated with this configuration or <code>null</code> if not
     * applicable.
     * @return The resource path of this configuration.
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * True if configuration has value for the key.
     * @param key The key.
     * @return True if configuration has value for the key.
     */
    boolean hasKey(String key) {
        return properties.getProperty(key) != null;
    }

    /**
     * Get a key value as a string.
     * @param key The key to lookup.
     * @return The value or null if does not exist.
     */
    String get(String key) {
        if (checkKey(key)) {
            return properties.getProperty(key);
        } else {
            throw new IllegalArgumentException("The key " + key + " does not exist in this configuration with a valid value.");
        }
    }

    /**
     * Get a key value as a boolean.
     * @param key The key to lookup.
     * @return The value or null if does not exist.
     */
    Boolean getBoolean(String key) {
        if (checkKey(key)) {
            return Boolean.parseBoolean(properties.getProperty(key));    
        } else {
            return null;
        }        
    }

    /**
     * Get a key value as a double.
     * @param key The key to lookup.
     * @return The value or null if does not exist.
     */
    Double getDouble(String key) {
        if (checkKey(key)) {
            return Double.parseDouble(properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Get a key value as an integer.
     * @param key The key to lookup.
     * @return The value or null if does not exist.
     */
    Integer getInteger(String key) {
        if (checkKey(key)) {
            return Integer.parseInt(properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Write this configuration to a file and set that file as the current one.
     * @param file The output file.
     */
    public void writeToFile(File file) {
        this.file = file;
        try {
            properties.store(new FileOutputStream(this.file), null);
        } catch (IOException e) {
            throw new RuntimeException("Error saving properties file.", e);
        }
    }
    
    /**
     * Check if the properties contains the key and if it has a non-null value.
     * @param key The properties key.
     * @return True if properties key is valid.
     */
    private boolean checkKey(String key) {
        return hasKey(key) && properties.getProperty(key) != null;
    }

    /**
     * Set a configuration value.
     * @param key The key for lookup.
     * @param value The value to assign to that key.
     */
    void set(String key, Object value) {
        properties.put(key, String.valueOf(value));
    }

    /**
     * Remove a configuration value.
     * @param key The key of the value.
     */
    void remove(String key) {
        properties.remove(key);
    }

    /**
     * Convert this object to a string by printing out its properties list.
     */
    public String toString() {
        return properties.toString();
    }
}