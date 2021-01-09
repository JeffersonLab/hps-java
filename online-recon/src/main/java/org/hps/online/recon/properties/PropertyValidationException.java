package org.hps.online.recon.properties;

import java.util.List;

/**
 * Exception thrown by {@link PropertyStore#validate()} when one or more
 * properties in a {@link PropertyStore} are invalid
 */
@SuppressWarnings("serial")
public class PropertyValidationException extends Exception {
    PropertyValidationException(List<Property<?>> badProps) {
        super("The following properties are not valid:" + propsToString(badProps));
    }

    private static String propsToString(List<Property<?>> badProps) {
        StringBuffer buff = new StringBuffer();
        for (Property<?> p : badProps) {
            buff.append(" " + p.name);
        }
        return buff.toString();
    }
}