package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.TempOutputWriter;
import org.hps.record.daqconfig.ConfigurationManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This <code>Driver</code> converts raw ECal data collections to {@link org.lcsim.event.CalorimeterHit} collections 
 * with energy and time information.  The {@link EcalRawConverter} does most of the low-level work.
 * <p>
 * The following input collections are used:
 * <ul>
 * <li>EcalReadoutHits<li>
 * <li>EcalReadoutExtraDataRelations</li>
 * <li>EcalRunningPedestals</li>
 * </ul>
 * <p>
 * The results are by default written to the <b>EcalCalHits</b> output collection.
 */
public class EcalRawConverterDriver extends Driver {
    private final TempOutputWriter writer = new TempOutputWriter("converted_hits_old.log");

    // To import database conditions
    private EcalConditions ecalConditions = null;

    private EcalRawConverter converter = null;
    /**
     * Input collection name (unless runBackwards=true, then it's output).
     * Can be a {@link org.lcsim.event.RawTrackerHit} or {@link org.lcsim.event.RawCalorimeterHit}
     * These have ADC and sample time information.
     */
    private String rawCollectionName = "EcalReadoutHits";
    
    /**
     * Output collection name (unless runBackwards=true, then it's input).
     * Always a {@link org.lcsim.event.CalorimeterHit}
     * This has energy (GeV) and ns time information.
     */
    private String ecalCollectionName = "EcalCalHits";
    
    /**
     * Defines the name of the collection that contains the truth
     * relations for raw hits.
     */
    private String truthRelationsCollectionName = "EcalTruthRelations";
    
    /**
     * ecalCollectionName "type" (must match detector-data) 
     */
    private final String ecalReadoutName = "EcalHits";

    /*
     * Output relation between ecalCollectionName and Mode-7 pedestals
     */
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";
    
    /**
     * Hit threshold in GeV.  Anything less will not be put into LCIO. 
     */
    private double threshold = Double.NEGATIVE_INFINITY;
    
    /**
     * Whether to reject bad crystals. 
     */
    private boolean applyBadCrystalMap = true;
    
    /**
     * Whether to reject bad FADC channels.
     */
    private boolean dropBadFADC = false;
    
    /**
     * If true, convert ecalCollectionName to rawCollectionName (GeV to ADC).
     * Else, convert rawCollectionName to ecalCollectionName (ADC to GeV).
     */
    private boolean runBackwards = false;
  
    /**
     * 
     */
    private boolean useTimestamps = false;
    
    /**
     * 
     */
    private boolean useTruthTime = false;
    
    /**
     * Whether to use DAQ config read from EVIO for EcalRawConverter parameters.
     * Should be completely removed to a standalone class soilely for trigger emulation.
     */
    private boolean useDAQConfig = false;

    /**
     * Whether to perform "firmware algorithm" on Mode-1 data.
     * 
     * If so, this includes finding a threshold crossing, extracting
     * a pulse time, and integrating over some configurable sample
     * range inside the window to extract pulse integral.
     * 
     * If not, it simply integrates the entire window and makes
     * no attempt at extracting pulse time.
     * 
     * This is poorly named.
     */
    private boolean emulateFirmware = true;
    
    /**
     * Specifies whether truth information is to be persisted through
     * this driver. If set to true, the driver will attempt to access
     * calorimeter truth relations and will merge them into the hits
     * it generates. An error will occur if these relations are not
     * found. 
     */
    private boolean persistTruth = false;
    
    public EcalRawConverterDriver() {
        converter = new EcalRawConverter();
    }
    
    @Override
    public void endOfData() {
        writer.close();
    }

    /**
     * Set to <code>true</code> to use pulse fitting instead of arithmetic integration:<br/>
     */
    public void setUseFit(boolean useFit) { converter.setUseFit(useFit); }
    
    /**
     * Fix 3-pole function width to be the same for all 442 ECal channels.  Units=samples.
     */
    public void setGlobalFixedPulseWidth(double width) { converter.setGlobalFixedPulseWidth(width); }
    
    /**
     * Set to <code>true</code> to fix fitted pulse widths to their channel's mean value:<br/>
     */
    public void setFixShapeParameter(boolean fix) { converter.setFixShapeParameter(fix); }
   
    /**
     * Limit threshold crossing range that is candidate for pulse-fitting.   Units=samples.
     */
    public void setFitThresholdTimeLo(int sample) { converter.setFitThresholdTimeLo(sample); }
    public void setFitThresholdTimeHi(int sample) { converter.setFitThresholdTimeHi(sample); }
    
    /**
     * Constrain pulse fit time0 parameter.  Units=samples. 
     */
    public void setFitLimitTimeLo(int sample) { converter.setFitLimitTimeLo(sample); }
    public void setFitLimitTimeHi(int sample) { converter.setFitLimitTimeHi(sample); }
    
    /**
     * Set to <code>true</code> to use the "2014" gain formula:<br/>
     * <pre>channelGain * adcSum * gainFactor * readoutPeriod</pre>
     * <p>
     * Set to <code>false</code> to use the gain formula for the Test Run:
     * <pre>gain * adcSum * ECalUtils.MeV</pre> 
     * 
     * @param use2014Gain True to use 2014 gain formulation.
     */
    public void setUse2014Gain(boolean use2014Gain) {
        converter.setUse2014Gain(use2014Gain);
    }

    /**
     * Set to <code>true</code> to apply time walk correction from {@link EcalTimeWalk#correctTimeWalk(double, double)}.
     * <p>
     * This is only applicable to Mode-3 data.
     * 
     * @param useTimeWalkCorrection True to apply time walk correction.
     */
    public void setUseTimeWalkCorrection(boolean useTimeWalkCorrection) {
        converter.setUseTimeWalkCorrection(useTimeWalkCorrection);
    }
    
    /**
     * Set to <code>true</code> to use a running pedestal calibration from mode 7 data.
     * <p>
     * The running pedestal values are retrieved from the event collection "EcalRunningPedestals"
     * which is a <code>Map</code> between {@link org.hps.conditions.ecal.EcalChannel} objects
     * are their average pedestal.
     * 
     * @param useRunningPedestal True to use a running pedestal value.
     */
    public void setUseRunningPedestal(boolean useRunningPedestal) {
        converter.setUseRunningPedestal(useRunningPedestal);
    }
    
    public void setFixedWidth(boolean fixedWidth){
        this.converter.setFixedWidth(fixedWidth);
    }

    /**
     * Set to <code>true</code> to generate a {@link org.lcsim.event.CalorimeterHit} 
     * collection which is a conversion from energy to raw signals.
     * 
     * @param runBackwards True to run the procedure backwards.
     */
    public void setRunBackwards(boolean runBackwards) {
        this.runBackwards = runBackwards;
    }

    /**
     * Set to <code>true</code> to drop hits that are mapped to a hard-coded 
     * bad FADC configuration from the Test Run.
     * 
     * @param dropBadFADC True to drop hits mapped to a bad FADC.
     */
    public void setDropBadFADC(boolean dropBadFADC) {
        this.dropBadFADC = dropBadFADC;
    }

    /**
     * Set a minimum energy threshold in GeV for created {@link org.lcsim.event.CalorimeterHit}
     * objects to be written into the output collection.
     * @param threshold The minimum energy threshold in GeV.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Set to <code>true</code> to use Mode-7 emulation in calculations.
     * False is Mode-3.
     * 
     * @param mode7 True to use Mode-7 emulation in calculations.
     */
    public void setEmulateMode7(boolean mode7) {
        converter.setMode7(mode7);
    }
    
    /**
     * Set to <code>true</code> to emulate firmware conversion of Mode-1 to Mode-3/7 data.
     * 
     * @param emulateFirmware True to use firmware emulation.
     */
    public void setEmulateFirmware(boolean emulateFirmware) {
        this.emulateFirmware = emulateFirmware;
    }
    
    /**
     * Set the leading-edge threshold in ADC counts, relative to pedestal, for pulse-finding 
     * and time determination.
     * <p>
     * Used to convert Mode-1 readout into Mode-3 or Mode-7 data that is usable by clustering.
     * 
     * @param threshold The leading edge threshold in ADC counts.
     */
    public void setLeadingEdgeThreshold(double threshold) {
        converter.setLeadingEdgeThreshold(threshold);
    }
    
    /**
     * Set the number of samples in the FADC readout window.
     * <p>
     * This is needed in order to properly pedestal-correct clipped pulses for mode-3 and mode-7.  
     * It is ignored for mode-1 input, since this data already includes the number of samples.
     * <p>
     * A non-positive number disables pulse-clipped pedestals and reverts to the old behavior which 
     * assumed that the integration range was constant.
     * 
     * @param windowSamples The number of samples in the FADC readout window.
     */
    public void setWindowSamples(int windowSamples) {
        converter.setWindowSamples(windowSamples);
    }
    
    /**
     * Set the integration range in nanoseconds after the threshold crossing. 
     * <p>
     * These numbers must be multiples of 4 nanoseconds.
     * <p>
     * This value is used for pulse integration in Mode-1, and pedestal subtraction in all modes.
     * 
     * @param nsa The number of nanoseconds after the threshold crossing.
     * @see #setNsb(int)
     */
    public void setNsa(int nsa) {
        converter.setNSA(nsa);
    }
    
    /**
     * Set the integration range in nanoseconds before the threshold crossing.
     * <p>
     * These numbers must be multiples of 4 nanoseconds.
     * <p>
     * This value is used for pulse integration in Mode-1, and pedestal subtraction in all modes.
     * 
     * @param nsb The number of nanoseconds after the threshold crossing.
     * @see #setNsa(int)
     */
    public void setNsb(int nsb) {
        converter.setNSB(nsb);
    }
    
    /**
     * Set the maximum number of peaks to search for in the signal, 
     * which must be between 1 and 3, inclusive.
     * @param nPeak The maximum number of peaks to search for in the signal.
     */
    public void setNPeak(int nPeak) {
        converter.setNPeak(nPeak);
    }
    
    /**
     * Set a constant gain factor in the converter for all channels.
     * @param gain The constant gain value.
     */
    public void setGain(double gain) {
        converter.setGain(gain);
    }

    /**
     * Set the {@link org.lcsim.event.CalorimeterHit} collection name,
     * which is used as input in "normal" mode and output when running
     * "backwards".
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     * @see #runBackwards
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    /**
     * Set the raw collection name which is used as output in "normal" mode
     * and input when running "backwards".
     * <p>
     * Depending on the Driver configuration, this could be a collection
     * of {@link org.lcsim.event.RawTrackerHit} objects for Mode-1
     * or {@link org.lcsim.event.RawCalorimeterHit} objects for Mode-3
     * or Mode-7.
     * 
     * @param rawCollectionName The raw collection name.
     */
    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    /**
     * Set to <code>true</code> to ignore data from channels that
     * are flagged as "bad" in the conditions system.
     * @param apply True to ignore bad channels.
     */
    public void setApplyBadCrystalMap(boolean apply) {
        this.applyBadCrystalMap = apply;
    }
    
    public void setDisplay(boolean display){
        converter.setDisplay(display);
    }
    

    /**
     * Set to <code>true</code> to use timestamp information from the ECal or trigger.
     * @param useTimestamps True to use timestamp information.
     */
    // FIXME: What does this actually do?  What calculations does it affect?  
    public void setUseTimestamps(boolean useTimestamps) {
        this.useTimestamps = useTimestamps;
    }

    /**
     * Set to <code>true</code> to use MC truth information.
     * @param useTruthTime True to use MC truth information.
     */
    // FIXME: What does this actually do?  What calculations does it affect?  
    public void setUseTruthTime(boolean useTruthTime) {
        this.useTruthTime = useTruthTime;
    }
    
    /**
     * Sets whether the driver should use the DAQ configuration from
     * EvIO file for its parameters. If activated, the converter will
     * obtain gains, thresholds, pedestals, the window size, and the
     * pulse integration window from the EvIO file. This will replace
     * and overwrite any manually defined settings.<br/>
     * <br/>
     * Note that if this setting is active, the driver will not output
     * any data until a DAQ configuration has been read from the data
     * stream.
     * @param state - <code>true</code> indicates that the configuration
     * should be read from the DAQ data in an EvIO file. Setting this
     * to <code>false</code> will cause the driver to use its regular
     * manually-defined settings and pull gains and pedestals from the
     * conditions database.
     */
    public void setUseDAQConfig(boolean state) {
        useDAQConfig = state;
        converter.setUseDAQConfig(state);
    }
    
    /**
     * Sets whether the driver should use calorimeter truth relations
     * and include this data in the hits generates. If enabled, all
     * truth hits associated with a given raw hit will have their
     * contribution references merged into the output hit. This will
     * cause an exception if the truth information is not available.
     * If disabled, no truth information is included or needed.
     * @param state - <code>true</code> enables the merging of truth
     * information, and <code>false</code>  disables it.
     */
    public void setPersistTruth(boolean state) {
        persistTruth = state;
    }
    
    /**
     * Sets the name of the collection that contains the hit truth
     * relations.
     * @param collection - The collection name.
     */
    public void setTruthRelationsCollectionName(String collection) {
        truthRelationsCollectionName = collection;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        writer.initialize();
    }

    @Override
    public void detectorChanged(Detector detector) {

        // set the detector for the converter
        // FIXME: This method doesn't even need the detector object and does not use it.
        converter.setDetector(detector);

        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    /**
     * @return false if the channel is a good one, true if it is a bad one
     * @param hit the <code>CalorimeterHit</code> pointing to the channel
     */
    public boolean isBadCrystal(CalorimeterHit hit) {
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        return channelData.isBadChannel();
    }

    /**
     * @return false if the ADC is a good one, true if it is a bad one
     * @param hit the <code>CalorimeterHit</code> pointing to the FADC
     */
    public boolean isBadFADC(CalorimeterHit hit) {
        return (getCrate(hit.getCellID()) == 1 && getSlot(hit.getCellID()) == 3);
    }

    private static double getTimestamp(int system, EventHeader event) { //FIXME: copied from org.hps.readout.ecal.ReadoutTimestamp
        if (event.hasCollection(GenericObject.class, "ReadoutTimestamps")) {
            List<GenericObject> timestamps = event.get(GenericObject.class, "ReadoutTimestamps");
            for (GenericObject timestamp : timestamps) {
                if (timestamp.getIntVal(0) == system) {
                    return timestamp.getDoubleVal(0);
                }
            }
            return 0;
        } else {
            return 0;
        }
    }

    @Override
    public void process(EventHeader event) {
        writer.write("> Event " + event.getEventNumber() + " - ???");
        writer.write("Input");
        
        // Do not process the event if the DAQ configuration should be
        // used for value, but is not initialized.
        if(useDAQConfig && !ConfigurationManager.isInitialized()) {
            return;
        }
        
        final int SYSTEM_TRIGGER = 0;
//        final int SYSTEM_TRACKER = 1;
        final int SYSTEM_ECAL = 2;

        double timeOffset = 0.0;
        if (useTimestamps) {
            double t0ECal = getTimestamp(SYSTEM_ECAL, event);
            double t0Trig = getTimestamp(SYSTEM_TRIGGER, event);
            timeOffset += (t0ECal - t0Trig) + 200.0;
        }
        if (useTruthTime) {
            double t0ECal = getTimestamp(SYSTEM_ECAL, event);
            timeOffset += ((t0ECal + 250.0) % 500.0) - 250.0;
        }

        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store hit time
        flags += 1 << LCIOConstants.RCHBIT_LONG; //store hit position; this flag has no effect for RawCalorimeterHits
        
        // If the converter is to be run forwards (i.e. for readout
        // hit to simulation hits)...
        if (!runBackwards) {
            ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
            ArrayList<SimCalorimeterHit> newTruthHits = new ArrayList<SimCalorimeterHit>();
            
            // Load the truth information, if it exists, and
            // format it into a more usable data structure.
            List<LCRelation> truthRelations = null;
            if(event.hasCollection(LCRelation.class, truthRelationsCollectionName)) {
                truthRelations = event.get(LCRelation.class, truthRelationsCollectionName);
            }
            
            
            // Process Mode-1 data. These are stored as RawTrackerHit
            // objects.
            if(event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
                List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

                for(RawTrackerHit hit : hits) {
                    // Get the readout hits.
                    ArrayList<CalorimeterHit> newHits2 = new ArrayList<CalorimeterHit>();
                    
                    // If truth information exists, read it.
                    Map<RawTrackerHit, Set<SimCalorimeterHit>> hitToTruthMap = null;
                    if(persistTruth && truthRelations != null) {
                        hitToTruthMap = getTruthMap(truthRelations, RawTrackerHit.class);
                    } else if(persistTruth) {
                        throw new RuntimeException("Error: Requested truth data, but no truth relations were found.");
                    }
                    
                    // Convert each of the hits into a simulation hit.
                    if(emulateFirmware) {
                        // Because proper ADC buffer handling only
                        // takes buffer samples across a limited time
                        // range, it is not proper to directly attach
                        // all the truth hits associated with the ADC
                        // buffer to each hit. Instead, the truth
                        // data is sent to the converter. It will
                        // then take only that portion of the truth
                        // associated with the integration window for
                        // each hit, and assign that range to each
                        // truth hit.
                        newHits2.addAll(converter.HitDtoA(event, hit, hitToTruthMap, hit.getMetaData()));
                    } else {
                        // Get the generic hit.
                        CalorimeterHit genericHit = converter.HitDtoA(hit);
                        
                        // This method just adds the entire pulse ADC
                        // together, and as such, should have all the
                        // truth hits associated with that pulse.
                        if(persistTruth) {
                            genericHit = CalorimeterHitUtilities.convertToTruthHit(genericHit, hitToTruthMap.get(hit), genericHit.getMetaData());
                        }
                        
                        // Add the hit to the list of temporary hits.
                        newHits2.add(converter.HitDtoA(hit));
                    }
                    
                    // Check to make sure that the hit meets the
                    // readout criteria.
                    for(CalorimeterHit newHit : newHits2) {
                        // Get the channel data.
                        EcalChannelConstants channelData = findChannel(newHit.getCellID());
                        
                        if(applyBadCrystalMap && channelData.isBadChannel()) {
                            continue;
                        }
                        if(dropBadFADC && isBadFADC(newHit)) {
                            continue;
                        }
                        if(newHit.getRawEnergy() > threshold) {
                            if(!persistTruth) {
                                newHits.add(newHit);
                            } else if(SimCalorimeterHit.class.isAssignableFrom(newHit.getClass())) {
                                newTruthHits.add((SimCalorimeterHit) newHit);
                            } else {
                                throw new RuntimeException("Error: Truth data is available, but output hits do not include it.");
                            }
                        }
                    }
                }
                
                // If truth data is included, then the hits in the
                // output collection are actually SimCalorimeterHit
                // objects. Convert them formally.
                if(persistTruth) {
                    event.put(ecalCollectionName, newTruthHits, SimCalorimeterHit.class, flags, ecalReadoutName);
                    System.out.println("Output collection \"" + ecalCollectionName + "\" of size " + newTruthHits.size()
                            + " and type " + SimCalorimeterHit.class.getSimpleName());
                } else {
                    event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
                    System.out.println("Output collection \"" + ecalCollectionName + "\" of size " + newHits.size()
                            + " and type " + CalorimeterHit.class.getSimpleName());
                }
            }
            
            
            // Process Mode-3 and Mode-7 data. These are both stored
            // as RawCalorimeterHit objects.
            if(event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
                // Process Mode-7 hits. These are distinguished from
                // Mode-3 hits by the presence of extra information
                // in the form of LCRelations.
                if(event.hasCollection(LCRelation.class, extraDataRelationsName)) {
                    List<LCRelation> extraDataRelations = event.get(LCRelation.class, extraDataRelationsName);
                    for (LCRelation rel : extraDataRelations) {
                        // Get the readout hit.
                        RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                        
                        // If truth information exists, read it.
                        Map<RawCalorimeterHit, Set<SimCalorimeterHit>> hitToTruthMap = null;
                        if(persistTruth && truthRelations != null) {
                            hitToTruthMap = getTruthMap(truthRelations, RawCalorimeterHit.class);
                        } else if(persistTruth) {
                            throw new RuntimeException("Error: Requested truth data, but no truth relations were found.");
                        }
                        
                        // Get the extra readout data.
                        GenericObject extraData = (GenericObject) rel.getTo();
                        
                        // Convert the readout hit to a new hit.
                        CalorimeterHit newHit = converter.HitDtoA(event,hit, extraData, timeOffset);
                        
                        // If truth information exists, extract it
                        // and store it as a part of the hit. This is
                        // done by converting the hit to an identical
                        // SimCalorimeterHit object, except now with
                        // the extra truth data.
                        SimCalorimeterHit truthHit = null;
                        if(persistTruth) {
                            Set<SimCalorimeterHit> truthHits = hitToTruthMap.get(hit);
                            truthHit = CalorimeterHitUtilities.convertToTruthHit(newHit, truthHits, newHit.getMetaData());
                        }
                        
                        // If the new hit meets the appropriate
                        // conditions, persist it to the data stream.
                        if(newHit.getRawEnergy() > threshold) {
                            if(applyBadCrystalMap && isBadCrystal(newHit)) {
                                continue;
                            }
                            if(dropBadFADC && isBadFADC(newHit)) {
                                continue;
                            }
                            
                            // If the truth data is not available,
                            // just output a CalorimeterHit. If truth
                            // data is available, then instead output
                            // a SimCalorimeterHit.
                            if(truthHit == null) {
                                newHits.add(newHit);
                            } else {
                                newTruthHits.add(truthHit);
                            }
                        }
                    }
                }
                
                
                // If extra information is not available, the hits
                // are simply Mode-3.
                else {
                    // Get the collection of hits from the event.
                    List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);
                    
                    // DEBUG :: Write the raw hits seen.
                    for(RawCalorimeterHit hit : hits) {
                        writer.write(String.format("%d;%d;%d", hit.getAmplitude(), hit.getTimeStamp(), hit.getCellID()));
                    }
                    
                    // If truth information exists, read it.
                    Map<RawCalorimeterHit, Set<SimCalorimeterHit>> hitToTruthMap = null;
                    if(persistTruth && truthRelations != null) {
                        hitToTruthMap = getTruthMap(truthRelations, RawCalorimeterHit.class);
                    } else if(persistTruth) {
                        throw new RuntimeException("Error: Requested truth data, but no truth relations were found.");
                    }
                    
                    // Iterate over each hit and convert it to a
                    // simulation hit.
                    for (RawCalorimeterHit hit : hits) {
                        // Generate the new hit.
                        CalorimeterHit newHit = converter.HitDtoA(event, hit, timeOffset);
                        
                        // If truth information exists, extract it
                        // and store it as a part of the hit. This is
                        // done by converting the hit to an identical
                        // SimCalorimeterHit object, except now with
                        // the extra truth data.
                        SimCalorimeterHit truthHit = null;
                        if(persistTruth) {
                            Set<SimCalorimeterHit> truthHits = hitToTruthMap.get(hit);
                            truthHit = CalorimeterHitUtilities.convertToTruthHit(newHit, truthHits, newHit.getMetaData());
                        }
                        
                        // Check that the hit meets the output
                        // thresholds.
                        if(newHit.getRawEnergy() > threshold) {
                            if(applyBadCrystalMap && isBadCrystal(newHit)) {
                                continue;
                            }
                            if(dropBadFADC && isBadFADC(newHit)) {
                                continue;
                            }
                            
                            // If the truth data is not available,
                            // just output a CalorimeterHit. If truth
                            // data is available, then instead output
                            // a SimCalorimeterHit.
                            if(truthHit == null) {
                                newHits.add(newHit);
                            } else {
                                newTruthHits.add(truthHit);
                            }
                        }
                    }
                }
                writer.write("Output");
                for(CalorimeterHit hit : newHits) {
                    writer.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
                }
                
                // Output the truth hits if possible. Otherwise, just
                // output the converted hits.
                if(!persistTruth) {
                    event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
                } else {
                    event.put(ecalCollectionName, newTruthHits, SimCalorimeterHit.class, flags, ecalReadoutName);
                }
            }
        }
        
        // Otherwise, run the hit conversion in reverse to get ADC
        // from a calorimeter hit.
        else {
            ArrayList<RawCalorimeterHit> newHits = new ArrayList<RawCalorimeterHit>();
            if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
                List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

                for (CalorimeterHit hit : hits) {
                    RawCalorimeterHit newHit = converter.HitAtoD(hit);
                    if (newHit.getAmplitude() > 0) {
                        newHits.add(newHit);
                    }
                }
                event.put(rawCollectionName, newHits, RawCalorimeterHit.class, flags, ecalReadoutName);
            }
        }
        
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> Map<T, Set<SimCalorimeterHit>> getTruthMap(Collection<LCRelation> truthRelations, Class<T> readoutHitType) {
        // Create a map to store the relations in.
        Map<T, Set<SimCalorimeterHit>> truthMap = new HashMap<T, Set<SimCalorimeterHit>>();
        
        // Iterate over the truth relations and map each distinct
        // raw calorimeter hit to a set containing all of its truth
        // hits.
        for(LCRelation relation : truthRelations) {
            // Cast the relation objects to the appropriate type.
            T rawHit = null;
            SimCalorimeterHit truthHit = null;
            if(readoutHitType.isAssignableFrom(relation.getFrom().getClass())) {
                rawHit = (T) relation.getFrom();
            } else {
                throw new RuntimeException("Error: Expected object of class " + readoutHitType.getClass().getSimpleName() + ", but saw object of class "
                        + relation.getFrom().getClass().getSimpleName() + ".");
            }
            if(SimCalorimeterHit.class.isAssignableFrom(relation.getTo().getClass())) {
                truthHit = (SimCalorimeterHit) relation.getTo();
            } else {
                throw new RuntimeException("Error: Expected object of class " + SimCalorimeterHit.class.getSimpleName() + ", but saw object of class "
                        + relation.getTo().getClass().getSimpleName() + ".");
            }
            
            if(rawHit == null || truthHit == null) {
                throw new RuntimeException("Error: Calorimeter truth relations are not of the expected object types.");
            }
            
            // Add the truth hit to the map.
            Set<SimCalorimeterHit> hitSet = truthMap.get(rawHit);
            if(hitSet == null) {
                hitSet = new HashSet<SimCalorimeterHit>();
                truthMap.put(rawHit, hitSet);
            }
            hitSet.add(truthHit);
        }
        
        // Return the truth map.
        return truthMap;
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

    /**
     * Return crate number from cellID
     *
     * @param cellID (long)
     * @return Crate number (int)
     */
    private int getCrate(long cellID) {
        // Find the ECAL channel and return the crate number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getCrate();
    }

    /**
     * Return slot number from cellID
     *
     * @param cellID (long)
     * @return Slot number (int)
     */
    private int getSlot(long cellID) {
        // Find the ECAL channel and return the slot number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getSlot();
    }
}