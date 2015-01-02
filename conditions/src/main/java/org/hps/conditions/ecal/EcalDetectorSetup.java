package org.hps.conditions.ecal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.log.LogUtil;

/**
 * Puts {@link EcalConditions} data onto <code>EcalCrystal</code> objects on the
 * detector.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalDetectorSetup implements ConditionsListener {

    private static Logger logger = LogUtil.create(EcalDetectorSetup.class);
    
    private String ecalName = "Ecal";
    private boolean enabled = true;
    
    public EcalDetectorSetup(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setLogLevel(Level level) {
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void conditionsChanged(ConditionsEvent event) {
        if (enabled) {
            logger.info("setting up ECAL detector conditions");
            DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();
            Subdetector subdetector = manager.getDetectorObject().getSubdetector(ecalName); 
            EcalConditions conditions = manager.getConditionsData(EcalConditions.class, "ecal_conditions");
            load(subdetector, conditions);
        } else {
            
        }
    }
        
    /**
     * Load ECal conditions data onto a full detector object.
     * @param detector The detector object.
     * @param conditions The conditions object.
     */
    void load(Subdetector subdetector, EcalConditions conditions) {

        logger.info("loading ECAL conditions onto subdetector " + subdetector.getName());
        
        // Find EcalCrystal objects.
        List<EcalCrystal> crystals = subdetector.getDetectorElement().findDescendants(EcalCrystal.class);

        // Get the ID helper.
        IIdentifierHelper helper = subdetector.getDetectorElement().getIdentifierHelper();

        // Get the system ID.
        int system = subdetector.getSystemID();

        // Get the full channel map created by the conditions system.
        EcalChannelCollection channelMap = conditions.getChannelCollection();

        // Build the map of geometry IDs.
        logger.info("building ECAL geometry channel map");
        channelMap.buildGeometryMap(helper, system);
        logger.info("built ECAL geometry channel with " + channelMap.geometryMap.size() + " entries");

        // Loop over crystals.
        for (EcalCrystal crystal : crystals) {

            // Reset in case of existing conditions data.
            crystal.resetConditions();

            // Find the corresponding entry in the channel map for this crystal.
            int[] geomValues = new int[] { system, crystal.getX(), crystal.getY() };
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
        
        logger.info("done loading ECAL conditions onto subdetector");
    }
}