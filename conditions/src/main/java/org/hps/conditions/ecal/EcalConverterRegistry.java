package org.hps.conditions.ecal;

import org.hps.conditions.ConditionsObjectConverter;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;

/**
 * This is a set of data converters for ECAL conditions in the database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConverterRegistry {
    
    public static final class EcalBadChannelConverter extends ConditionsObjectConverter<EcalBadChannelCollection> {         
        public Class getType() {
            return EcalBadChannelCollection.class;
        }                

        public boolean allowMultipleCollections() {
            return true;
        }        
    }
    
    public static final class EcalCalibrationConverter extends ConditionsObjectConverter<EcalCalibrationCollection> {         
        public Class getType() {
            return EcalCalibrationCollection.class;
        }
    }
    
    public static final class EcalChannelConverter extends ConditionsObjectConverter<EcalChannelCollection> {         
        public Class getType() {
            return EcalChannelCollection.class;
        }                
    }
    
    public static final class EcalGainConverter extends ConditionsObjectConverter<EcalGainCollection> {         
        public Class getType() {
            return EcalGainCollection.class;
        }
    }
}
