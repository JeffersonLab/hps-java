package org.hps.conditions.ecal;

import java.util.List;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Subdetector;

/**
 * Load {@link EcalConditions} data onto <code>EcalCrystal</code> objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalConditionsLoader {
    
    /**
     * Load ECal conditions data onto a full detector object.
     * @param detector The detector object.
     * @param conditions The conditions object.
     */
    public void load(Subdetector subdetector, EcalConditions conditions) {
        
        // Find EcalCrystal objects.        
        List<EcalCrystal> crystals = subdetector.getDetectorElement().findDescendants(EcalCrystal.class);
        
        // Get the ID helper.
        IIdentifierHelper helper = subdetector.getDetectorElement().getIdentifierHelper();
        
        // Get the system ID.
        int system = subdetector.getSystemID();
        
        // Get the full channel map created by the conditions system.
        EcalChannelCollection channelMap = conditions.getChannelCollection();
        
        // Build the map of geometry IDs.
        channelMap.buildGeometryMap(helper, system);
                
        // Loop over crystals.
        for (EcalCrystal crystal : crystals) {
            
            //System.out.println(crystal.getName() + " @ " + crystal.getX() + ", " + crystal.getY());
            
            // Reset in case of existing conditions data.
            crystal.resetConditions();
            
            // Find the corresponding entry in the channel map for this crystal.
            int[] geomValues = new int[] {system, crystal.getX(), crystal.getY()};
            GeometryId geometryId = new GeometryId(helper, geomValues);
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
            
            // Set time shift.
            crystal.setTimeShift(constants.getTimeShift().getTimeShift());
        }
    }
}