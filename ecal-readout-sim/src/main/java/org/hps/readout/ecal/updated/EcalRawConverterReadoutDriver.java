package org.hps.readout.ecal.updated;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.RawConverterReadoutDriver;
import org.hps.readout.rawconverter.AbstractMode3RawConverter;
import org.hps.readout.rawconverter.EcalReadoutMode3RawConverter;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * <code>EcalRawConverterReadoutDriver</code> is an implementation of
 * {@link org.hps.readout.RawConverterReadoutDriver
 * RawConverterReadoutDriver} for the calorimeter subdetector.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.RawConverterReadoutDriver
 */
public class EcalRawConverterReadoutDriver extends RawConverterReadoutDriver {
    /**
     * The converter object responsible for processing raw hits into
     * proper {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
     * objects.
     */
    private EcalReadoutMode3RawConverter converter = new EcalReadoutMode3RawConverter();
    
    /**
     * Cached copy of the calorimeter conditions. All calorimeter
     * conditions should be called from here, rather than by directly
     * accessing the database manager.
     */
    private EcalConditions ecalConditions = null;
    
    /**
     * Instantiates the driver with the correct default parameters.
     */
    public EcalRawConverterReadoutDriver() {
        super("EcalRawHits", "EcalCorrectedHits");
        setSkipBadChannels(true);
    }
    
    /**
     * Indicates whether or not data from channels flagged as "bad"
     * in the conditions system should be ignored. <code>true</code>
     * indicates that they should be ignored, and <code>false</code>
     * that they should not.
     * @param apply - <code>true</code> indicates that "bad" channels
     * will be ignored and <code>false</code> that they will not.
     */
    @Override
    public void setSkipBadChannels(boolean state) {
        super.skipBadChannels = state;
    }
    
    @Override
    protected AbstractMode3RawConverter getConverter() {
        return converter;
    }
    
    @Override
    protected String getSubdetectorReadoutName(Detector detector) {
        HPSEcal3 calorimeterGeometry = (HPSEcal3) detector.getSubdetector("Ecal");
        return calorimeterGeometry.getReadout().getName();
    }
    
    @Override
    protected boolean isBadChannel(long channelID) {
        return findChannel(channelID).isBadChannel();
    }
    
    @Override
    protected void updateDetectorDependentParameters(Detector detector) {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }
    
    /**
     * Gets the channel parameters for a given channel ID.
     * @param cellID - The <code>long</code> ID value that represents
     * the channel. This is typically acquired from the method {@link
     * org.lcsim.event.CalorimeterHit#getCellID() getCellID()} in a
     * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} object.
     * @return Returns the channel parameters for the channel as an
     * {@link org.hps.conditions.ecal.EcalChannelConstants
     * EcalChannelConstants} object.
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
}