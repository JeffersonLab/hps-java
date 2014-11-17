package org.hps.conditions.api;

import java.util.LinkedHashMap;

/**
 * Simple class extending <code>java.lang.Map</code> that maps field names
 * to values.
 */
public final class FieldValueMap extends LinkedHashMap<String, Object> {

    Object[] valuesToArray() {
        return values().toArray();
    }

    String[] fieldsToArray() {
        return keySet().toArray(new String[] {});
    }
}
