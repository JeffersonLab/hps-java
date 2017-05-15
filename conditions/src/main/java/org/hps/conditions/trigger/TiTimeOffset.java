package org.hps.conditions.trigger;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Table;

/**
 * <p>
 * Represents the per-run trigger time offset in nanoseconds.
 * </p>
 * <p>
 * A single instance of this class is returned for an entire run.
 * </p>
 */
@Table(names = {"ti_time_offsets"})
@Converter(converter = TiTimeOffsetConverter.class)
public class TiTimeOffset extends BaseConditionsObject {
    
    // FIXME: This is not actually used but it is here to make the conditions manager happy.
    public static class TiTimeOffsetCollection extends BaseConditionsObjectCollection<TiTimeOffset> {
    }
    
    private Long value = null;
    
    TiTimeOffset(Long value) {
        this.value = value;
    }
    
    public Long getValue() {
        return value;
    }
}
