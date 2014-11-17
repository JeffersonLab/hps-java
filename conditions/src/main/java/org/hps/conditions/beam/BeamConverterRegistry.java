package org.hps.conditions.beam;

import org.hps.conditions.beam.BeamCurrent.BeamCurrentCollection;
import org.hps.conditions.database.ConditionsObjectConverter;

public final class BeamConverterRegistry {
    public static final class BeamCurrentConverter extends ConditionsObjectConverter<BeamCurrentCollection> {
        public Class getType() {
            return BeamCurrentCollection.class;
        }
    }
}
