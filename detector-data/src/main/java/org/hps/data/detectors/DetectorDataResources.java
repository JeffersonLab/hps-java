package org.hps.data.detectors;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Get information about available HPS detector resources.
 * 
 */
public class DetectorDataResources {
    
    private DetectorDataResources() {        
    }
    
    /**
     * Find a list of available detector names.
     * <p>
     * Only those detectors that have names starting with "HPS" in their <code>detector.properties</code> files will be
     * returned.
     *
     * @return the list of available HPS detector names
     */
    public static Set<String> getDetectorNames() {
        final ClassLoader classLoader = DetectorDataResources.class.getClassLoader();
        final Set<String> detectorNames = new TreeSet<String>();
        final URL url = DetectorDataResources.class.getResource("DetectorDataResources.class");
        final String protocol = url.getProtocol();
        if (!"jar".equals(protocol)) {
            throw new RuntimeException("Unsupported URL protocol: " + url.getProtocol());
        }
        try {
            final JarURLConnection con = (JarURLConnection) url.openConnection();
            final JarFile archive = con.getJarFile();
            final Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("detector.properties")) {
                    final InputStream inputStream = classLoader.getResourceAsStream(entry.getName());
                    if (inputStream == null) {
                        throw new RuntimeException("Failed to load jar entry: " + entry.getName());
                    }
                    final Properties properties = new Properties();
                    properties.load(inputStream);
                    final String detectorName = properties.getProperty("name");
                    detectorNames.add(detectorName);
                }
            }
            archive.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return detectorNames;
    }   
}
