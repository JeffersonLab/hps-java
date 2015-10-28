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
     * Get the list of available conditions tags from the conditions system.
     *
     * @return the list of available conditions tags
     */
    public static String[] getConditionsTags() {
        return DatabaseConditionsManager.getInstance().getAvailableTags().toArray(new String[] {});
    }

    /**
     * Do not allow class instantiation.
     */
    private ResourceUtil() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
