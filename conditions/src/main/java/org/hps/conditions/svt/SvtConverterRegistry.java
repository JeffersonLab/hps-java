package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectConverter;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtPulseParameters.SvtPulseParametersCollection;
import org.hps.conditions.svt.SvtTimeShift.SvtTimeShiftCollection;

@SuppressWarnings({ "unchecked", "rawtypes" })
public final class SvtConverterRegistry {

    public static class SvtBadChannelConverter extends ConditionsObjectConverter<SvtBadChannelCollection> {
        public Class getType() {
            return SvtBadChannelCollection.class;
        }

        public boolean allowMultipleCollections() {
            return true;
        }
    }

    public static class SvtCalibrationConverter extends ConditionsObjectConverter<SvtCalibrationCollection> {
        public Class getType() {
            return SvtCalibrationCollection.class;
        }
    }

    public static class SvtChannelConverter extends ConditionsObjectConverter<SvtChannelCollection> {
        public Class getType() {
            return SvtChannelCollection.class;
        }
    }

    public static class SvtDaqMappingConverter extends ConditionsObjectConverter<SvtDaqMappingCollection> {
        public Class getType() {
            return SvtDaqMappingCollection.class;
        }
    }

    public static class SvtGainConverter extends ConditionsObjectConverter<SvtGainCollection> {
        public Class getType() {
            return SvtGainCollection.class;
        }
    }

    public static class SvtPulseParametersConverter extends ConditionsObjectConverter<SvtPulseParametersCollection> {
        public Class getType() {
            return SvtPulseParametersCollection.class;
        }
    }

    public static class SvtTimeShiftConverter extends ConditionsObjectConverter<SvtTimeShiftCollection> {
        public Class getType() {
            return SvtTimeShiftCollection.class;
        }
    }
}
