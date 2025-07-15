package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import org.apache.commons.math3.util.Pair;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtSyncStatus.SvtSyncStatusCollection;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.svt.HPSSVTConstants;
import org.hps.recon.ecal.cluster.TriggerTime;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.cat.util.Const;
import org.lcsim.util.Driver;



// TODO: Add class documentation.
public class RawTrackerHitFitterDriver extends Driver {

    private boolean debug = false;
    private ShaperFitAlgorithm fitter = new DumbShaperFit();
    private PulseShape shape = new PulseShape.FourPole();
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String fitCollectionName = "SVTShapeFitParameters";
    private String fittedHitCollectionName = "SVTFittedRawTrackerHits";
    private String fitTimeMinimizer = "Simplex";
    private SvtTimingConstants timingConstants;
    private SvtSyncStatusCollection syncStatusColl;
    private int genericObjectFlags = 1 << LCIOConstants.GOBIT_FIXED;
    private int relationFlags = 0;
    private double jitter;
    private boolean correctTimeOffset = false;
    private boolean correctT0Shift = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;
    private boolean subtractTOF = false;
    private boolean subtractTriggerTime = false;
    private boolean correctChanT0 = false;
    //    private boolean correctPhaseDepT0Shift = false; 
    private boolean subtractRFTime = false;
    private boolean correctPerSensorPerPhase=false;
    private Boolean syncGood = true;
    private Boolean isMC = false;
    private Map<Pair,Double> sensorPhaseCalibConstants=new HashMap<Pair,Double>();

    private double trigTimeScale = 43.0;//  the mean time of the trigger...changes with run period!!!  43.0 is for 2015 Eng. Run
    private double trigTimeOffset = 14.0;
    private double tsCorrectionScale = 240;
    private double chiSqrThresh = .5;

    private int doOldDT = 1;

    private boolean isFirstEvent=true;

    private TrackerHitUtils tkHitUtils=new TrackerHitUtils();

    /**
     * Report time relative to the nearest expected truth event time.
     *
     * @param useTruthTime
     */
    public void setChiSqrThresh(double chiSqrThresh){
        this.chiSqrThresh = chiSqrThresh;
    }

    public void setDoOldDT(int doOldDT){
        this.doOldDT = doOldDT;
    }

    public void setUseTruthTime(boolean useTruthTime) {
        this.useTruthTime = useTruthTime;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setCorrectTimeOffset(boolean correctTimeOffset) {
        this.correctTimeOffset = correctTimeOffset;
    }

    public void setCorrectT0Shift(boolean correctT0Shift) {
        this.correctT0Shift = correctT0Shift;
    }

    public void setUseTimestamps(boolean useTimestamps) {
        this.useTimestamps = useTimestamps;
    }

    public void setTsCorrectionScale(double tsCorrectionScale) {
        this.tsCorrectionScale = tsCorrectionScale;
    }

    public void setSubtractTOF(boolean subtractTOF) {
        this.subtractTOF = subtractTOF;
    }

    public void setSubtractTriggerTime(boolean subtractTriggerTime) {
        this.subtractTriggerTime = subtractTriggerTime;
    }

    public void setCorrectChanT0(boolean correctChanT0) {
        this.correctChanT0 = correctChanT0;
    }

    public void setSubtractRFTime(boolean subtractRFTime) {
        this.subtractRFTime = subtractRFTime;
    }

    public void setCorrectPerSensorPerPhase(boolean correctPerSensorPerPhase) {
        this.correctPerSensorPerPhase = correctPerSensorPerPhase;
    }

    public void setTrigTimeScale(double time) {
        this.trigTimeScale = time;
    }

    public void setTrigTimeOffset(double offset) {
        this.trigTimeOffset = offset;
    }

    public void setIsMC(boolean isMc) {
        this.isMC = isMc;
    }

    public void setFitAlgorithm(String fitAlgorithm) {
        if (fitAlgorithm.equals("Analytic"))
            fitter = new ShaperAnalyticFitAlgorithm();
        else if (fitAlgorithm.equals("Linear"))
            fitter = new ShaperLinearFitAlgorithm(1);
        else if (fitAlgorithm.equals("PileupAlways"))
            fitter = new ShaperPileupFitAlgorithm(1.0,this.doOldDT);
        else if (fitAlgorithm.equals("Pileup"))
            fitter = new ShaperPileupFitAlgorithm(this.chiSqrThresh,this.doOldDT);
        else
            throw new RuntimeException("Unrecognized fitAlgorithm: " + fitAlgorithm);
    }

    public void setPulseShape(String pulseShape) {
        if (pulseShape.equals("CR-RC"))
            shape = new PulseShape.CRRC();
        else if (pulseShape.equals("FourPole"))
            shape = new PulseShape.FourPole();
        else
            throw new RuntimeException("Unrecognized pulseShape: " + pulseShape);
    }

    public void setFitCollectionName(String fitCollectionName) {
        this.fitCollectionName = fitCollectionName;
    }

    public void setFittedHitCollectionName(String fittedHitCollectionName) {
        this.fittedHitCollectionName = fittedHitCollectionName;
    }

    public void setFitTimeMinimizer(String fitTimeMinimizer) {
        this.fitTimeMinimizer = fitTimeMinimizer;
    }

    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }

    @Override
    public void startOfData() {
        fitter.setDebug(debug);
        fitter.setFitTimeMinimizer(fitTimeMinimizer);
        if (rawHitCollectionName == null)
            throw new RuntimeException("The parameter rawHitCollectionName1 was not set!");
    }

    protected void detectorChanged(Detector detector) {
        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        try {
            syncStatusColl = DatabaseConditionsManager.getInstance().getCachedConditions(SvtSyncStatusCollection.class, "svt_sync_statuses").getCachedData();
            syncGood = syncStatusColl.get(0).isGood();
        }
        catch (Exception e) {
            syncGood = true;
            getLogger().config("svt_sync_statuses was not found.");
        }
    
        
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(RawTrackerHit.class, rawHitCollectionName))
            // System.out.println(rawHitCollectionName + " does not exist; skipping event");
            return;
        
        if(isFirstEvent && correctPerSensorPerPhase){
            //this loads the sensorPhaseCalibConstants or returns false if run not in range or calib file not found/broken
            //...there should be a better way to get run number...right?  
            correctPerSensorPerPhase=LoadPerSensorPerPhaseConstants(event);
        }
        isFirstEvent=false;
        
        jitter = -666;
        if (subtractRFTime)
            if (event.hasCollection(TriggerTime.class, "TriggerTime")) {
                if (debug)
                    System.out.println("Getting TriggerTime Object");
                List<TriggerTime> jitterList = event.get(TriggerTime.class, "TriggerTime");
                if (debug)
                    System.out.println("TriggerTime List Size = " + jitterList.size());
                TriggerTime jitterObject = jitterList.get(0);
                jitter = jitterObject.getDoubleVal();
                if (debug)
                    System.out.println("RF time jitter " + jitter);

            } else {
                System.out.println("Requested RF Time correction but TriggerTime Collection doesn't exist!!!");
                return;
            }

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        if (rawHits == null)
            throw new RuntimeException("Event is missing SVT hits collection!");
        List<FittedRawTrackerHit> hits = new ArrayList<FittedRawTrackerHit>();
        List<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();

        // Make a fitted hit from this cluster
        for (RawTrackerHit hit : rawHits) {
            int strip = hit.getIdentifierFieldValue("strip");
            HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
            //===> ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) hit.getDetectorElement(), strip);
            fitter.setRunNum(event.getRunNumber());
            for (ShapeFitParameters fit : fitter.fitShape(hit, shape)) {

                if (correctTimeOffset) {
                    if (debug)
                        System.out.println("subtracting svt time offset " + timingConstants.getOffsetTime());
                    fit.setT0(fit.getT0() - timingConstants.getOffsetTime());
                }
                if (subtractTriggerTime) {
                    double tt = (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - trigTimeOffset);
                    if (!syncGood) tt = tt - 8;
                    if (!syncGood && (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24)/8 < 1)) {
                        tt = tt + 24;
                    }
                    if (isMC && (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24)/8 == 1)) {
                        tt = tt + 24;
                    }
                    if (debug) {
                        System.out.println("event time stamp " + event.getTimeStamp());
                        System.out.println("trigger offset time " + timingConstants.getOffsetPhase());
                        System.out.println("subtracting trigger time from event " + tt);
                        System.out.println("T0 " + fit.getT0());
                    }
                    fit.setT0(fit.getT0() - tt);
                }
                if (subtractRFTime && jitter != -666) {
                    if (debug)
                        System.out.println("subtracting RF time jitter " + jitter);
                    fit.setT0(fit.getT0() - jitter + trigTimeScale);
                }
                if (correctChanT0) {
                    if (debug)
                        System.out.println("subtracting channel t0 " + sensor.getShapeFitParameters(strip)[HpsSiSensor.T0_INDEX]);
                    fit.setT0(fit.getT0() - sensor.getShapeFitParameters(strip)[HpsSiSensor.T0_INDEX]);
                }
                if (correctT0Shift) {
                    if (debug)
                        System.out.println("subtracting sensor shift " + sensor.getT0Shift());
                    //===> fit.setT0(fit.getT0() - constants.getT0Shift());
                    fit.setT0(fit.getT0() - sensor.getT0Shift());
                }

                ////////////   mg 5/17/2023 ....  this is for reading sensor/phase dependent shifts using database...
                ///////////                       I should make new branch for reading from db because we may want to
                ///////////                       do that in future but for now remove
                //                if (correctPhaseDepT0Shift){
                //    Double phaseShifts[]=sensor.getT0PhaseShifts(); 
                //    int phase=(int)((event.getTimeStamp()%24)/4);
                //    fit.setT0(fit.getT0()-phaseShifts[phase]);
                //}
                // correct time per sensor and event phase (using the constants read in by resource file)
                if(correctPerSensorPerPhase){
                    String sensorName=sensor.getName();
                    String simpleName=tkHitUtils.getSimpleNameFromSensorName(sensorName);
                    //                    System.out.println(sensorName+" --> "+simpleName);
                    Long evtPhaseL=(event.getTimeStamp() % 24)/4;
                    Integer evtPhase=evtPhaseL.intValue();
                    Pair<String,Integer> evtPair=new Pair(simpleName,evtPhase);
                    Double calConstant = Optional.ofNullable(sensorPhaseCalibConstants.get(evtPair)).orElse(0.0);
                    //System.out.println("shifting t0 by "+calConstant); 
                    fit.setT0(fit.getT0()-calConstant);
                }

                if (subtractTOF) {
                    double tof = hit.getDetectorElement().getGeometry().getPosition().magnitude() / (Const.SPEED_OF_LIGHT * Const.nanosecond);
                    fit.setT0(fit.getT0() - tof);
                }
                if (useTimestamps) {
                    double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
                    double t0Trig = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRIGGERBITS, event);
                    // double corMod = (t0Svt - t0Trig) + 200.0;///where does 200.0 come from?  for 2016 MC, looks like should be 240
                    double corMod = (t0Svt - t0Trig) + tsCorrectionScale;
                    fit.setT0(fit.getT0() + corMod);
                }
                if (useTruthTime) {
                    double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
                    double absoluteHitTime = fit.getT0() + t0Svt;
                    double relativeHitTime = ((absoluteHitTime + 250.0) % 500.0) - 250.0;

                    fit.setT0(relativeHitTime);
                }
                if (debug)
                    System.out.println(fit);

                fits.add(fit);
                FittedRawTrackerHit hth = new FittedRawTrackerHit(hit, fit);
                hits.add(hth);
                if (strip == HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR) // drop unbonded channel
                    continue;
                hit.getDetectorElement().getReadout().addHit(hth);
            }
        }
        event.put(fitCollectionName, fits, ShapeFitParameters.class, genericObjectFlags);
        event.put(fittedHitCollectionName, hits, FittedRawTrackerHit.class, relationFlags);
    }

    private boolean LoadPerSensorPerPhaseConstants(EventHeader event){
        
        // get run number from first event
        int runNumber= event.getRunNumber();
        String infilePreResDir = "/org/hps/recon/tracking/timingCorrections/"; 
        String infilePre = "run";
        String infilePost = "_calib_constants_final.txt";
        String runString="foobar"; 

        if(runNumber>10000 && runNumber<10464){
            runString="10377";
        }else if(runNumber>=10464 && runNumber<10660){
            runString="10564"; 
        }else if(runNumber>=10660 && runNumber<11000){
            runString="10666"; 
        }else if(runNumber>14000 && runNumber<14566){        //2021 constants
            runString="14495";
        }else if(runNumber>=14566 &&runNumber<14626){
            runString="14569"; 
        }else if(runNumber>=14626 && runNumber<14680){
            runString="14654";
        }else if(runNumber>=14680 && runNumber<15000){
            runString="14720";
        } else {
            getLogger().config("Run is not in range defined by correctPerSensorPerPhase ... corrections not performed");
            return false;
        }

        //ok, overwrite the run string for some individual funny runs
        if(runNumber==10687 || runNumber==10711||runNumber==10713||runNumber==10714||runNumber==14210||runNumber==14232){
            runString=Integer.toString(runNumber);
        }



        String infile=infilePreResDir+infilePre+runString+infilePost; 
        InputStream inRatios = this.getClass().getResourceAsStream(infile);
        System.out.println("reading in per-sensor per-phase calibs from "+infile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inRatios));
        String line;
        String delims = "[ ]+";// this will split strings between one or more spaces
        try {
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(delims);
                System.out.println("sensor_phase = " + tokens[0] + "; constant = " + tokens[1]);
                String[] sensor_phase = tokens[0].split("_phase"); 
                //                System.out.println("Making Pair::sensor name = "+sensor_phase[0]+", phase = "+Integer.parseInt(sensor_phase[1]));
                Pair<String, Integer> senPhPair=new Pair(sensor_phase[0],Integer.parseInt(sensor_phase[1])); 
                Double constant=Double.parseDouble(tokens[1]);
                sensorPhaseCalibConstants.put(senPhPair, constant); 
            }
        } catch (IOException ex) {
            getLogger().config("died while reading "+infile);
            return false;
        }        
        return true;
    }  
    
}
