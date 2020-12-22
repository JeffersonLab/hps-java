package org.hps.online.recon.properties;

import java.util.List;

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