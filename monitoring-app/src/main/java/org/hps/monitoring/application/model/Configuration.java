package org.hps.monitoring.application.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * This class provides a list of key, value pairs backed by a <code>Properties</code> object. The getter and setter
 * methods for these values are not public, because the {@link org.hps.monitoring.application.model.ConfigurationModel}
 * class should be used instead to get or set application configuration values.
 */
public final class Configuration {

    /**
     * The file containing the keys and values.
     */
    private File file;

    /**
     * The Java properties file.
     */
    private Properties properties;

    /**
     * The path to an embedded properties resource from a jar.
     */
    private String resourcePath;

    /**
     * Class constructor.
     */
    Configuration() {
        this.properties = new Properties();
    }

    /**
     * Class constructor.
     * <p>
     * Loads a configuration from a properties file.
     *
     * @param file the properties file
     */
    public Configuration(final File file) {
        this.file = file;
        try {
            this.properties = new Properties();
            this.properties.load(new FileInputStream(this.file));
        } catch (final IOException e) {
            throw new RuntimeException("Error parsing properties file.", e);
        }
    }

    /**
     * Class constructor.
     * <p>
     * Load a configuration from a resource path pointing to a properties file.
     *
     * @param resourcePath the resource path to the properties file
     */
    public Configuration(final String resourcePath) {
        this.resourcePath = resourcePath;
        final InputStream is = this.getClass().getResourceAsStream(this.resourcePath);
        try {
            this.properties = new Properties();
            this.properties.load(is);
        } catch (final IOException e) {
            throw new RuntimeException("Error parsing properties resource.", e);
        }
    }

    /**
     * Check if the properties contains the key and if it has a non-null value.
     *
     * @param key the properties key
     * @return <code>true</code> if properties key is valid
     */
    boolean checkKey(final String key) {
        return this.hasKey(key) && this.properties.getProperty(key) != null;
    }

    /**
     * Get a key value as a string.
     *
     * @param key the key to lookup
     * @return the value or null if does not exist
     */
    String get(final String key) {
        if (this.checkKey(key)) {
            // Return the key value for properties that are set.
            return this.properties.getProperty(key);
        } else {
            // Return null for unset properties.
            return null;
        }
    }

    /**
     * Get a key value as a boolean.
     *
     * @param key the key to lookup
     * @return the value or null if does not exist
     */
    Boolean getBoolean(final String key) {
        if (this.checkKey(key)) {
            return Boolean.parseBoolean(this.properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Get a key value as a double.
     *
     * @param key the key to lookup
     * @return the value as a <code>Double</code> or <code>null</code> if does not exist
     */
    Double getDouble(final String key) {
        if (this.checkKey(key)) {
            return Double.parseDouble(this.properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Get the file associated with this configuration or <code>null</code> if not set.
     *
     * @return the file associated with the configuration
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Get a key value as an integer.
     *
     * @param key the key to lookup
     * @return the value as an <code>Integer</code> or <code>null</code> if does not exist
     */
    Integer getInteger(final String key) {
        if (this.checkKey(key)) {
            return Integer.parseInt(this.properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Get the property keys.
     *
     * @return the collection of property keys
     */
    public Set<String> getKeys() {
        return this.properties.stringPropertyNames();
    }

    /**
     * Get a key value as a Long.
     *
     * @param key the key to lookup
     * @return the key value as a <code>Long</code>
     */
    Long getLong(final String key) {
        if (this.checkKey(key)) {
            return Long.parseLong(this.properties.getProperty(key));
        } else {
            return null;
        }
    }

    /**
     * Get the resource path associated with this configuration or <code>null</code> if not applicable.
     *
     * @return the resource path of this configuration
     */
    public String getResourcePath() {
        return this.resourcePath;
    }

    /**
     * Return <code>true</code> if configuration has value for the key.
     *
     * @param key the key
     * @return <code>true</code> if configuration has value for the key
     */
    boolean hasKey(final String key) {
        try {
            return this.properties.containsKey(key);
        } catch (final java.lang.NullPointerException e) {
            return false;
        }
    }

    /**
     * Merge in values from another configuration into this one which will override properties that already exist with
     * new values.
     *
     * @param configuration the configuration with the properties to merge
     */
    void merge(final Configuration configuration) {
        for (final String property : configuration.getKeys()) {
            this.set(property, configuration.get(property));
        }
    }

    /**
     * Remove a configuration value.
     *
     * @param key the key of the value
     */
    void remove(final String key) {
        this.properties.remove(key);
    }

    /**
     * Set a configuration value.
     *
     * @param key the key for lookup
     * @param value the value to assign to that key
     */
    void set(final String key, final Object value) {
        this.properties.put(key, String.valueOf(value));
    }

    /**
     * Convert this object to a string by printing out its properties list.
     */
    @Override
    public String toString() {
        return this.properties.toString();
    }

    /**
     * Write this configuration to a file and set that file as the current one.
     *
     * @param file The output file.
     */
    public void writeToFile(final File file) {
        this.file = file;
        try {
            this.properties.store(new FileOutputStream(this.file), null);
        } catch (final IOException e) {
            throw new RuntimeException("Error saving properties file.", e);
        }
    }
}