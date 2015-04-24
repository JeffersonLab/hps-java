package org.hps.conditions.api;

import java.util.Collection;
import java.util.Set;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public interface FieldValues {

    Set<String> getFieldNames();

    <T> T getValue(Class<T> type, String name);

    Object getValue(String name);

    Collection<Object> getValues();

    boolean hasField(String name);

    boolean isNonNull(String name);

    boolean isNull(String name);

    void setValue(String name, Object value);

    int size();
}
