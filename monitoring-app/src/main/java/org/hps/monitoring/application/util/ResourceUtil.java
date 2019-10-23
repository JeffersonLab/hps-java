package org.hps.monitoring.application.util;

import java.util.HashSet;
import java.util.Set;

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
        // FIXME: New database manager should probably not be instantiated here.
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        String[] tags = mgr.getAvailableTags().toArray(new String[]{});
        try {
            mgr.getConnection().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tags;
    }

    /**
     * Do not allow class instantiation.
     */
    private ResourceUtil() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
