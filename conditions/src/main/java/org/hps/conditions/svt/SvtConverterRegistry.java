package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectConverter;
import org.hps.conditions.svt.SvtAlignmentConstant.SvtAlignmentCollection;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtConfiguration.SvtConfigurationCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;

/**
 * Definitions of converters from the database to SVT specific conditions classes.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class SvtConverterRegistry {

    public static class SvtAlignmentConverter extends ConditionsObjectConverter<SvtAlignmentCollection> {        
        public Class getType() {
            return SvtAlignmentCollection.class;
        }               
    }
    
    public static class SvtBadChannelConverter extends ConditionsObjectConverter<SvtBadChannelCollection> {
        public Class getType() {
            return SvtBadChannelCollection.class;
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
    
    public static class SvtConfigurationConverter extends ConditionsObjectConverter<SvtConfigurationCollection> {
        public Class getType() {
            return SvtConfigurationCollection.class;
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

    public static class SvtShapeFitParametersConverter extends ConditionsObjectConverter<SvtShapeFitParametersCollection> {
        public Class getType() {
            return SvtShapeFitParametersCollection.class;
        }
    }

    public static class SvtT0ShiftConverter extends ConditionsObjectConverter<SvtT0ShiftCollection> {
        public Class getType() {
            return SvtT0ShiftCollection.class;
        }
    }
}