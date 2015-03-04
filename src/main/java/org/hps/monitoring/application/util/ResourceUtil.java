package org.hps.monitoring.application.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hps.record.LCSimEventBuilder;
import org.reflections.Reflections;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ResourceUtil {

    private ResourceUtil() {        
    }
    
    /**
     * Get the files with extension 'lcsim' from all loaded jar files.
     * @param packageName The package name for filtering the resources.
     * @return A list of embedded steering file resources.
     */
    public static String[] findSteeringResources(String packageName) {
        List<String> resources = new ArrayList<String>();
        URL url = ResourceUtil.class.getResource("ResourceUtil.class");
        String scheme = url.getProtocol();
        if (!"jar".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported scheme.  Only jar is allowed.");
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".lcsim") && entry.getName().contains(packageName)) {
                    resources.add(entry.getName());
                }
            }
            archive.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        java.util.Collections.sort(resources);
        String[] arr = new String[resources.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = resources.get(i);
        }
        return arr;
    }
    
    /**
     * Find all classes that implement {@link org.hps.record.LCSimEventBuilder} and return
     * a list of their canonical names.
     * @return The list of classes implementing LCSimEventBuilder.
     */
    public static String[] findEventBuilderClassNames() {
        Reflections reflections = new Reflections("org.hps");
        Set<Class<? extends LCSimEventBuilder>> subTypes = reflections.getSubTypesOf(LCSimEventBuilder.class);
        Set<String> classNames = new HashSet<String>();
        for (Class<? extends LCSimEventBuilder> type : subTypes) {
            classNames.add(type.getCanonicalName());
        }
        return classNames.toArray(new String[classNames.size()]);        
    }
 
    /**
     * Find a list of available detector names.
     * Only those detectors that have names starting with "HPS" in their
     * detector.properties files will be returned.
     * @return The list of available detector names.
     */
    public static String[] findDetectorNames() {
        ClassLoader classLoader = ResourceUtil.class.getClassLoader();
        List<String> detectorNames = new ArrayList<String>();
        URL url = ResourceUtil.class.getResource("ResourceUtil.class");
        String protocol = url.getProtocol();
        if (!"jar".equals(protocol)) {
            throw new RuntimeException("Unsupported URL protocol: " + url.getProtocol());
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("detector.properties")) {
                    InputStream inputStream = classLoader.getResourceAsStream(entry.getName());
                    if (inputStream == null) {
                        throw new RuntimeException("Failed to load jar entry: " + entry.getName());
                    }
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String detectorName = properties.getProperty("name");
                    if (detectorName.startsWith("HPS")) {
                        detectorNames.add(detectorName);
                    }
                }
            }
            archive.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(detectorNames);
        return detectorNames.toArray(new String[detectorNames.size()]);
    }        
}
