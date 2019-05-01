package org.hps.readout.rawconverter;

import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.Ecal3PoleFunction;
import org.hps.recon.ecal.EcalPulseFitter;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;

import hep.aida.IFitResult;

public class EcalMode1RawConverter extends AbstractMode1RawConverter {
    /**
     * Stores the calibrations and conditions for the calorimeter
     * subdetector.
     */
    private EcalConditions ecalConditions = null;

    /**
     * Whether to use pulse fitting (EcalPulseFitter) to extract pulse energy time. Only applicable to Mode-1 data.
     */
    private boolean useFit = true;

    /**
     * The pulse fitter class.
     */
    private EcalPulseFitter pulseFitter = new EcalPulseFitter();

    /**
     * Defines whether or not to use a running pedestal. A running
     * pedestal calculates the pedestal based on the values of
     * previous events.
     */
    private boolean useRunningPedestal = true;
    
    /**
     * Stores the running pedestal.
     */
    private Map<EcalChannel, Double> runningPedestalMap = null;
    
    /**
     * Sets a maximum time before which no pulses will be fit.
     * @param sample - The maximum sample.
     */
    public void setFitThresholdTimeHi(int sample) {
        pulseFitter.threshRange[1] = sample;
    }
    
    /**
     * Sets a minimum time before which no pulses will be fit.
     * @param sample - The minimum sample.
     */
    public void setFitThresholdTimeLo(int sample) {
        pulseFitter.threshRange[0] = sample;
    }
    
    /**
     * Requires that the pulse time parameter in the fit fall below
     * this value.
     * @param sample - The maximum.
     */
    public void setFitLimitTimeHi(int sample) {
        pulseFitter.t0limits[1] = sample;
    }
    
    /**
     * Requires that the pulse time parameter in the fit exceed this
     * value.
     * @param sample - The minimum.
     */
    public void setFitLimitTimeLo(int sample) {
        pulseFitter.t0limits[0] = sample;
    }
    
    /**
     * Sets whether or not the pulse shape is a free parameter in the
     * pulse fit.
     * @param fix - <code>true</code> means that the width is
     * <b>not</b> a free parameter and <code>false</code> that it
     * is.
     */
    public void setFixShapeParameter(boolean fix) {
        pulseFitter.fixShapeParameter = fix;
    }
    
    /**
     * Sets the pulse width as a fixed parameter of the specified
     * value. This also calls {@link
     * org.hps.readout.rawconverter.EcalMode1RawConverter#setFixShapeParameter(boolean)
     * setFixShapeParameter(boolean)} to <code>true</code>.
     * @param width - The fixed-value width.
     */
    public void setGlobalFixedPulseWidth(double width) {
        pulseFitter.globalThreePoleWidth = width;
        pulseFitter.fixShapeParameter = true;
    }
    
    /**
     * Defines whether or not to perform pulse-fitting.
     * @param useFit - <code>true</code> indicates that pulse-fitting
     * should be used and <code>false</code> that it should not.
     */
    public void setUseFit(boolean useFit) {
        this.useFit = useFit;
    }
    
    /**
     * Defines whether or not to use a running pedestal.
     * @param useRunningPedestal - <code>true</code> employs the
     * running pedestal and <code>false</code> does not.
     */
    public void setUseRunningPedestal(boolean useRunningPedestal) {
        this.useRunningPedestal = useRunningPedestal;
    }
    
    @Override
    public void updateDetector(Detector detector) {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }
    
    /**
     * Updates the converter with event-specific data.
     * @param event - The current event.
     */
    @SuppressWarnings("unchecked")
    public void updateEvent(EventHeader event) {
        // If it exists, get the running pedestal map from the event
        // data and store it.
        if(event.hasItem("EcalRunningPedestals")) {
            runningPedestalMap = (Map<EcalChannel, Double>) event.get("EcalRunningPedestals");
        } else {
            runningPedestalMap = null;
        }
    }
    
    @Override
    protected double[] convertWaveformToPulse(RawTrackerHit hit, int thresholdCrossing) {
        // Get the superclass results.
        double[] values = super.convertWaveformToPulse(hit, thresholdCrossing);
        
        // If pulse-fitting is supported, perform the pulse fit.
        if(useFit) {
            // Get the results. If the fit results exist and are of
            // a non-negative fit quality, use the fitted results.
            IFitResult fitResult = pulseFitter.fitPulse(hit, thresholdCrossing, values[3]);
            if(fitResult != null) {
                double fitQuality = fitResult.quality();
                if(fitQuality > 0) {
                    // Get the values from the fit.
                    double pulseTime = fitResult.fittedParameter("time0") * NS_PER_SAMPLE;
                    double sumADC = fitResult.fittedParameter("integral");
                    double minADC = fitResult.fittedParameter("pedestal");
                    double maxADC = ((Ecal3PoleFunction) fitResult.fittedFunction()).maximum();
                    
                    // Return the fit values.
                    return new double[] { pulseTime, sumADC, minADC, maxADC, fitQuality };
                }
            }
        }
        
        // If pulse-fitting is disabled or the fit was insufficiently
        // good, return the original values from the superclass.
        return values;
    }
    
    @Override
    protected double getGain(long cellID) {
        return findChannel(cellID).getGain().getGain();
    }
    
    @Override
    protected double getPedestal(long cellID) {
        // Attempt to use the running pedestal if appropriate.
        if(useRunningPedestal) {
            // If the pedestal map is defined, get the pedestal from
            // it.
            if(runningPedestalMap != null) {
                // Get the pedestal if possible. If not possible,
                // default back to the conditions database.
                EcalChannel chan = ecalConditions.getChannelCollection().findGeometric(cellID);
                if(!runningPedestalMap.containsKey(chan)) {
                    System.err.println("************** Missing Pedestal");
                } else {
                    return runningPedestalMap.get(chan);
                }
            }
            
            // Otherwise, the running pedestal is not available.
            else {
                System.err.println("*****************************************************************");
                System.err.println("**  You Requested a Running Pedestal, but it is NOT available. **");
                System.err.println("**     Reverting to the database. Only printing this ONCE.     **");
                System.err.println("*****************************************************************");
                useRunningPedestal = false;
            }
        }
        
        // If the running pedestal is disabled, unavailable, or not
        // found, use the default value from the conditions database.
        return findChannel(cellID).getCalibration().getPedestal();
    }
    
    /**
     * Get the calorimeter conditions for the specified channel.
     * @param cellID - The channel ID.
     * @return Returns an object containing the conditions for the
     * specified calorimeter channel.
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
}