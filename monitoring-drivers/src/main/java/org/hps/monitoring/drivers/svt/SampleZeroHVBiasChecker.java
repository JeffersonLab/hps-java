package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.SvtBiasConditionsLoader;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtBiasMyaDumpReader;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasRunRange;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.HeadBankData;
import org.hps.record.epics.EpicsData;
import org.hps.util.BasicLogFormatter;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;


/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SampleZeroHVBiasChecker extends Driver {

    // Logger
    Logger logger = LogUtil.create(getName(), new BasicLogFormatter(), Level.INFO);
    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    private static ITree tree;
    IHistogramFactory histogramFactory;
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    IPlotter plotter1;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    private boolean showPlots = false;

    List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadc;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoise;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseON;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseOFF;
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String triggerBankCollectionName ="TriggerBank";
    private static final String subdetectorName = "Tracker";
    List<SvtBiasRunRange> runRanges;
    SvtBiasRunRange runRange = null;
    private Date eventDate = null;
    private int eventCount = 0;
    FileWriter fWriter;
    PrintWriter pWriter;
    private String fileName = "biasoutput.txt";
    private int eventCountHvOff = 0;
    private String runSpreadSheetPath;
    private String myaDumpPath; 
    private double epicsBiasValue = -1; 
    private boolean hvOnEpics = false;
    private boolean hvOn = false;
    private EpicsData epicsData = null;
    private int eventCountEpicsDisagree = 0;
    SvtBiasConstantCollection svtBiasConstants = null;
    

    

    public void setMyaDumpPath(String myaDumpPath) {
        this.myaDumpPath = myaDumpPath;
    }

    public void setRunSpreadSheetPath(String runSpreadSheetPath) {
        this.runSpreadSheetPath = runSpreadSheetPath;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    
    public void setShowPlots(boolean showPlots) {
        this.showPlots = showPlots;
    }

    
    @Override
    protected void detectorChanged(Detector detector) {
    
        
        ConditionsRecordCollection col_svt_bias = DatabaseConditionsManager.getInstance().findConditionsRecords("svt_bias");
        if(col_svt_bias==null) {
            logger.info("svt_bias name collection wasn't found");
        }
        ConditionsRecordCollection col_svt_bias_constants = DatabaseConditionsManager.getInstance().findConditionsRecords("svt_bias_constants");
        if(col_svt_bias_constants==null) {
            logger.info("col_svt_bias_constants name collection wasn't found");
        }
        
        
        
        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class , "svt_bias").getCachedData();

        
        try {
            fWriter = new FileWriter(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open file " + fileName, e);
        }
        pWriter = new PrintWriter(fWriter);


        
        
        tree = IAnalysisFactory.create().createTreeFactory().create();
        tree.cd("");
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        hists_rawadc = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoise = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoiseON = new HashMap<HpsSiSensor, IHistogram1D>();
        hists_rawadcnoiseOFF = new HashMap<HpsSiSensor, IHistogram1D>();
        
        sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        plotter1 = plotterFactory.create("Pedestal subtracted zero Sample ADC");
        plotter1.createRegions(6, 6);
        plotter2 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4");
        plotter2.createRegions(6, 6);
        plotter3 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4 ON");
        plotter3.createRegions(6, 6);
        plotter4 = plotterFactory.create("Pedestal subtracted zero Sample ADC MaxSample>4 OFF");
        plotter4.createRegions(6, 6);

        for (HpsSiSensor sensor : sensors) {
            AIDA aida = AIDA.defaultInstance();
            hists_rawadc.put(sensor, aida.histogram1D(sensor.getName() + " raw adc - ped", 100, -1000.0, 5000.0));
            plotter1.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadc.get(sensor));
            hists_rawadcnoise.put(sensor, aida.histogram1D(sensor.getName() + " raw adc - ped maxSample>4", 100, -1000.0, 1000.0));
            plotter2.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoise.get(sensor));
            hists_rawadcnoiseON.put(sensor, aida.histogram1D(sensor.getName() + " raw adc - ped maxSample>4 ON", 100, -1000.0, 1000.0));
            plotter3.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoiseON.get(sensor));
            hists_rawadcnoiseOFF.put(sensor, aida.histogram1D(sensor.getName() + " raw adc - ped maxSample>4 OFF", 100, -1000.0, 1000.0));
            plotter4.region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists_rawadcnoiseOFF.get(sensor));
        }

        if(showPlots) {
            plotter1.show();
            plotter2.show();
            plotter3.show();
            plotter4.show();
        }


        RunMap runmap = SvtBiasConditionsLoader.getRunMapFromSpreadSheet(runSpreadSheetPath);
        SvtBiasMyaDumpReader biasDumpReader = new SvtBiasMyaDumpReader(myaDumpPath);
        //SvtBiasConditionsLoader.setTimeOffset(Calendar.)
        runRanges = SvtBiasConditionsLoader.getBiasRunRanges(runmap, biasDumpReader);
        logger.info("Print all " + runRanges.size() + " bias run ranges:");
        for(SvtBiasRunRange r : runRanges) {
            logger.info(r.toString());
            pWriter.println(r.toString());
        }


        
    }

    
    private Date getEventTimeStamp(EventHeader event) {
        List<GenericObject> intDataCollection = event.get(GenericObject.class, triggerBankCollectionName);
        for (GenericObject data : intDataCollection) {
            if (AbstractIntData.getTag(data) == HeadBankData.BANK_TAG) {
                Date date = HeadBankData.getDate(data);
                if (date != null) {
                    return date;
                } 
            }
        }
        return null;
    }
    
    
    @Override
    public void process(EventHeader event) {
        
        
        // Read EPICS data if available
        epicsData = EpicsData.read(event);
        
        if(epicsData!=null) {
            logger.info(epicsData.toString());
            if(epicsData.getUsedNames().contains("SVT:bias:top:0:v_sens")) {
                
                epicsBiasValue = epicsData.getValue("SVT:bias:top:0:v_sens");
                logger.info("epicsBiasValue = " + Double.toString(epicsBiasValue));
                
                if(epicsBiasValue>SvtBiasMyaDumpReader.BIASVALUEON) {
                    hvOnEpics = true;
                }
                
            }
        } else {
            logger.fine("no epics information in this event");
        }
        
        
        
        // Read the timestamp for the event
        // It comes in on block level so not every event has it, use the latest one throughout a block
        
        Date newEventDate = getEventTimeStamp(event);
        if(newEventDate!=null) {
            eventDate = newEventDate;
        }
        
        // only do this analysis where there is a date availabe.
        
        if(eventDate!=null) {

            logger.info("eventDate " + eventDate.toString());
            
            eventCount++;
            
            
            // check what the DB has
            if(svtBiasConstants != null) {
                logger.info("there are " + svtBiasConstants.size() + " constants to search");
                for(SvtBiasConstant constant : svtBiasConstants) {
                    logger.info("start " + constant.getStart().toString() + " end " + constant.getEnd() + " value " + constant.getValue());
                }
                
                
                SvtBiasConstant constant = svtBiasConstants.find(eventDate);
                
                logger.info(constant==null?"No constant found!":("Found constant " + "start " + constant.getStart().toString() + " end " + constant.getEnd() + " value " + constant.getValue()));
                
            }
            
            
            
            if(runRange==null) {
                for(SvtBiasRunRange r : runRanges) {
                    if (r.getRun().getRun()==event.getRunNumber()) {
                        runRange = r;
                    }
                }
            }
            
            hvOn = runRange.getRanges().includes(eventDate);
            
            // print the cases where epics and run range do not agree
            if(hvOn!=hvOnEpics && epicsBiasValue>0.) {
                logger.warning("hvOn is " + (hvOn?"ON":"OFF") + " hvOnEpics " + (hvOnEpics?"ON":"OFF") + " for Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOn?"YES":"NO") + " hvOnEpics " + (hvOnEpics?"YES":"NO"));
                pWriter.println("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOn?"YES":"NO"));
                eventCountEpicsDisagree++;
            }
            
            // print the cases where the HV is OFF
            if(!hvOn) {
                logger.info("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOn?"YES":"NO")+ " hvOnEpics " + (hvOnEpics?"YES":"NO"));
                pWriter.println("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOn?"YES":"NO")+ " hvOnEpics " + (hvOnEpics?"YES":"NO"));
                eventCountHvOff++;
            }
            if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
                // Get RawTrackerHit collection from event.
                List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
                
                for (RawTrackerHit hit : rawTrackerHits) {
                    HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                    int strip = hit.getIdentifierFieldValue("strip");
                    double pedestal = sensor.getPedestal(strip, 0);
                    hists_rawadc.get(sensor).fill(hit.getADCValues()[0] - pedestal);

                    int maxSample = 0;
                    double maxSampleValue = 0;
                    for(int s=0;s<6;++s) {
                        if(((double)hit.getADCValues()[s] - pedestal)>maxSampleValue) {
                            maxSample = s;
                            maxSampleValue =  ((double) hit.getADCValues()[s]) - pedestal;
                        }
                    }
                    if(maxSample>=4) {
                        hists_rawadcnoise.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        if(hvOn) {
                            hists_rawadcnoiseON.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        } else {
                            hists_rawadcnoiseOFF.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void endOfData() {
        
        logger.info("eventCount " + Integer.toString(eventCount) + " eventCountHvOff " + Integer.toString(eventCountHvOff) + " eventCountEpicsDisagree " + Integer.toString(eventCountEpicsDisagree));
        pWriter.println("eventCount " + Integer.toString(eventCount) + " eventCountHvOff " + Integer.toString(eventCountHvOff) + " eventCountEpicsDisagree " + Integer.toString(eventCountEpicsDisagree));
        
        try {
            pWriter.close();
            fWriter.close();
        } catch(IOException ex) {
             logger.log(Level.SEVERE, null, ex);
        }
        
    }
    
   
    
}
