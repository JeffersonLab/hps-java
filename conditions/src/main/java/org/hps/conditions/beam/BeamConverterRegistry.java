package org.hps.conditions.beam;

import org.hps.conditions.ConditionsObjectConverter;
import org.hps.conditions.beam.BeamCurrent.BeamCurrentCollection;

public class BeamConverterRegistry {   
    public static final class BeamCurrentConverter extends ConditionsObjectConverter<BeamCurrentCollection> {
        public Class getType() {
            return BeamCurrentCollection.class;
        }
    }
}
