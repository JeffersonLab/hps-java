package org.hps.conditions.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class FieldValuesMap implements FieldValues {

    Map<String, Object> data = new HashMap<String, Object>();

    public FieldValuesMap() {
    }

    FieldValuesMap(final TableMetaData tableMetaData) {
        for (final String fieldName : tableMetaData.getFieldNames()) {
            this.data.put(fieldName, null);
        }
    }

    @Override
    public Set<String> getFieldNames() {
        return this.data.keySet();
    }

    @Override
    public <T> T getValue(final Class<T> type, final String name) {
        return type.cast(this.data.get(name));
    }

    @Override
    public Object getValue(final String name) {
        return this.data.get(name);
    }

    @Override
    public Collection<Object> getValues() {
        return this.data.values();
    }

    @Override
    public boolean hasField(final String name) {
        return this.data.containsKey(name);
    }

    @Override
    public boolean isNonNull(final String name) {
        return this.data.get(name) != null;
    }

    @Override
    public boolean isNull(final String name) {
        return this.data.get(name) == null;
    }

    @Override
    public void setValue(final String name, final Object value) {
        this.data.put(name, value);
    }

    @Override
    public int size() {
        return this.data.size();
    }
}
