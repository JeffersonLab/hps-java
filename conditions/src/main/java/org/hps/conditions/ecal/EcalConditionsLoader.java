package org.hps.conditions.ecal;

import java.util.List;

import org.hps.conditions.ecal.EcalChannelMap.GeometryId;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.geometry.Detector;

/**
 * Load {@link EcalConditions} data onto <code>EcalCrystal</code> objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsLoader {
    
    /**
     * Load ECal conditions data onto a full detector object.
     * @param detector The detector object.
     * @param conditions The conditions object.
     */
    public void load(Detector detector, EcalConditions conditions) {
        
        // Find EcalCrystal objects.        
        List<EcalCrystal> crystals = detector.getDetectorElement().findDescendants(EcalCrystal.class);
        
        // Get the full channel map created by the conditions system.
        EcalChannelMap channelMap = conditions.getChannelMap();
                
        // Loop over crystals.
        for (EcalCrystal crystal : crystals) {
            
            //System.out.println(crystal.getName() + " @ " + crystal.getX() + ", " + crystal.getY());
            
            // Reset possibly existing conditions data.
            crystal.reset();
            
            // Find the corresponding entry in the channel map for this crystal.
            GeometryId geometryId = new GeometryId();
            geometryId.x = crystal.getX();
            geometryId.y = crystal.getY();
            EcalChannel channel = channelMap.findChannel(geometryId);
            if (channel == null) {
                throw new RuntimeException("EcalChannel not found for crystal: " + crystal.getName());
            }
            
            // Set the crate.
            crystal.setCrate(channel.getCrate());
            
            // Set the slot.
            crystal.setSlot(channel.getSlot());
            
            // Set the channel number.
            crystal.setChannel(channel.getChannel());
            
            // Get the channel constants.
            EcalChannelConstants constants = conditions.getChannelConstants(channel);
            if (constants == null) {
                throw new RuntimeException("EcalChannelConstants object not found for crystal: " + crystal.getName());
            }
                        
            // Set bad channel.
            crystal.setBadChannel(constants.isBadChannel());
            
            // Set pedestal.
            crystal.setPedestal(constants.getCalibration().getPedestal());
            
            // Set noise.
            crystal.setNoise(constants.getCalibration().getNoise());
            
            // Set gain.
            crystal.setGain(constants.getGain().getGain());
        }
    }
}