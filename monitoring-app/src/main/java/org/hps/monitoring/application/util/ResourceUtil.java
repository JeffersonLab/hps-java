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

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.LCSimEventBuilder;
import org.reflections.Reflections;

/**
 * This is a set of utility methods for getting jar resources at runtime.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ResourceUtil {

    /**
     * Find a list of available detector names.
     * <p>
     * Only those detectors that have names starting with "HPS" in their <code>detector.properties</code> files will be
     * returned.
     *
     * @return the list of available HPS detector names
     */
    public static String[] findDetectorNames() {
        final ClassLoader classLoader = ResourceUtil.class.getClassLoader();
        final List<String> detectorNames = new ArrayList<String>();
        final URL url = ResourceUtil.class.getResource("ResourceUtil.class");
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
                    if (detectorName.startsWith("HPS")) {
                        detectorNames.add(detectorName);
                    }
                }
            }
            archive.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(detectorNames);
        return detectorNames.toArray(new String[detectorNames.size()]);
    }

    /**
     * Find all classes that implement {@link org.hps.record.LCSimEventBuilder} and return a list of their canonical
     * names.
     *
     * @return the list of fully qualified class names that implement LCSimEventBuilder
     */
    public static String[] findEventBuilderClassNames() {
        final Reflections reflections = new Reflections("org.hps");
        final Set<Class<? extends LCSimEventBuilder>> subTypes = reflections.getSubTypesOf(LCSimEventBuilder.class);
        final Set<String> classNames = new HashSet<String>();
        for (final Class<? extends LCSimEventBuilder> type : subTypes) {
            classNames.add(type.getCanonicalName());
        }
        return classNames.toArray(new String[classNames.size()]);
    }

    /**
     * Get all of the files with the extension "lcsim" which are in a certain package.
     *
     * @param packageName the package name for filtering the list of resources
     * @return a list of embedded steering file resources
     */
    public static String[] findSteeringResources(final String packageName) {
        final List<String> resources = new ArrayList<String>();
        final URL url = ResourceUtil.class.getResource("ResourceUtil.class");
        final String scheme = url.getProtocol();
        if (!"jar".equals(scheme)) {
            throw new RuntimeException("Unsupported URL protocol: " + url.getProtocol());
        }
        try {
            final JarURLConnection con = (JarURLConnection) url.openConnection();
            final JarFile archive = con.getJarFile();
            final Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".lcsim") && entry.getName().contains(packageName)) {
                    resources.add(entry.getName());
                }
            }
            archive.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        java.util.Collections.sort(resources);
        final String[] arr = new String[resources.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = resources.get(i);
        }
        return arr;
    }

    /**
     * Get the list of available conditions tags from the conditions system.
     *
     * @return the list of available conditions tags
     */
    // FIXME: This method probably does not belong in this class.
    public static String[] getConditionsTags() {
        return DatabaseConditionsManager.getInstance().getTags().toArray(new String[] {});
    }

    /**
     * Do not allow class instantiation.
     */
    private ResourceUtil() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
