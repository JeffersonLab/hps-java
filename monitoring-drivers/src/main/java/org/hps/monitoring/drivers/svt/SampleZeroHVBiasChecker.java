package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.analysis.trigger.util.TriggerDataUtils;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.svt.SvtBiasConditionsLoader;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtBiasMyaDataReader;
import org.hps.conditions.svt.SvtBiasMyaDataReader.SvtBiasMyaRange;
import org.hps.conditions.svt.SvtBiasMyaDataReader.SvtBiasRunRange;
import org.hps.recon.tracking.SvtPlotUtils;
import org.hps.record.epics.EpicsData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.HeadBankData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SampleZeroHVBiasChecker extends Driver {

    // Logger
    private static Logger LOGGER = Logger.getLogger(SampleZeroHVBiasChecker.class.getPackage().getName());

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
    private boolean useRunTableFormat = false;
    private boolean discardMyaDataHeader = false;
    private boolean debug = false;
    private boolean dropSmallHitEvents = true;
    private double eventNumRange = 1e6;
    private int timeRange = 200;

    List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadc;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoise;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseON;
    private Map<HpsSiSensor, IHistogram1D> hists_rawadcnoiseOFF;
    private final Map<HpsSiSensor, IHistogram1D> hists_hitCounts = new HashMap<HpsSiSensor, IHistogram1D>();
    private final Map<HpsSiSensor, IHistogram1D> hists_hitCountsON = new HashMap<HpsSiSensor, IHistogram1D>();
    private final Map<HpsSiSensor, IHistogram1D> hists_hitCountsOFF = new HashMap<HpsSiSensor, IHistogram1D>();
    private IHistogram1D allHitCount;
    private IHistogram1D allHitCountON;
    private IHistogram1D allHitCountOFF;
    private IHistogram2D allHitCountVsNum;
    private IHistogram2D numVsTime;
    private IHistogram2D allHitCountVsTime;
    private IHistogram2D biasVsTime;
    private final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private final String triggerBankCollectionName = "TriggerBank";
    private static final String subdetectorName = "Tracker";
    List<SvtBiasRunRange> runRanges;
    SvtBiasRunRange runRange = null;
    private Date firstDate = null;
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
    private boolean hvOnMya = false;
    private boolean hvOnConditions = false;
    private boolean hvOnEventFlag = false;
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

    public void setUseRunTableFormat(boolean useRunTableFormat) {
        this.useRunTableFormat = useRunTableFormat;
    }

    public void setDiscardMyaDataHeader(boolean discardMyaDataHeader) {
        this.discardMyaDataHeader = discardMyaDataHeader;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDropSmallHitEvents(boolean dropSmallHitEvents) {
        this.dropSmallHitEvents = dropSmallHitEvents;
    }

    public void setEventNumRange(double eventNumRange) {
        this.eventNumRange = eventNumRange;
    }

    public void setTimeRange(int timeRange) {
        this.timeRange = timeRange;
    }

    @Override
    protected void detectorChanged(Detector detector) {

//                ConditionsRecordCollection col_svt_align_constants = DatabaseConditionsManager.getInstance().findConditionsRecords("svt_alignments");
//        if (col_svt_align_constants == null) {
//            logger.info("svt_alignments collection wasn't found");
//        }
//        col_svt_align_constants.
        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
        System.out.println("found " + svtBiasConstants.size() + " bias ON ranges");

//                timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
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

        allHitCount = AIDA.defaultInstance().histogram1D("all hit count", 200, 0, 200);
        allHitCountON = AIDA.defaultInstance().histogram1D("all hit count ON", 200, 0, 200);
        allHitCountOFF = AIDA.defaultInstance().histogram1D("all hit count OFF", 200, 0, 200);
        allHitCountVsTime = AIDA.defaultInstance().histogram2D("all hit count vs. elapsed time", timeRange, 0, timeRange, 200, 0, 200);
        biasVsTime = AIDA.defaultInstance().histogram2D("bias vs. elapsed time", timeRange, 0, timeRange, 200, 0, 200);

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
            hists_hitCounts.put(sensor, aida.histogram1D(sensor.getName() + " hit count", 100, 0, 100));
            hists_hitCountsON.put(sensor, aida.histogram1D(sensor.getName() + " hit count ON", 100, 0, 100));
            hists_hitCountsOFF.put(sensor, aida.histogram1D(sensor.getName() + " hit count OFF", 100, 0, 100));
        }

        if (showPlots) {
            plotter1.show();
            plotter2.show();
            plotter3.show();
            plotter4.show();
        }

        List<RunSpreadsheet.RunData> runmap;
        if (useRunTableFormat) {
            runmap = SvtBiasMyaDataReader.readRunTable(new File(runSpreadSheetPath));
        } else {
            runmap = SvtBiasConditionsLoader.getRunListFromSpreadSheet(runSpreadSheetPath);
        }
        List<SvtBiasMyaRange> ranges = SvtBiasMyaDataReader.readMyaData(new File(myaDumpPath), 178.0, 2000, discardMyaDataHeader);

        //SvtBiasConditionsLoader.setTimeOffset(Calendar.)
        runRanges = SvtBiasMyaDataReader.findOverlappingRanges(runmap, ranges);
        LOGGER.info("Print all " + runRanges.size() + " bias run ranges:");
        for (SvtBiasRunRange r : runRanges) {
            if (debug) {
                LOGGER.info(r.toString());
            }
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

        Map<String, int[]> params = event.getIntegerParameters();

        int[] biasGood = params.get("svt_bias_good");
//        int[] positionGood = params.get("svt_position_good");
//        int[] burstmodeNoiseGood = params.get("svt_burstmode_noise_good");

//        System.out.format("%d %d %d\n", biasGood[0], positionGood[0], burstmodeNoiseGood[0]);
        if (biasGood == null) {
            hvOnEventFlag = false;
        } else {
            hvOnEventFlag = biasGood[0] == 1;
        }

        if (allHitCountVsNum == null) {
            allHitCountVsNum = AIDA.defaultInstance().histogram2D("all hit count vs. event num", 1000, event.getEventNumber(), event.getEventNumber() + eventNumRange, 50, 0, 200);
        }
        if (numVsTime == null) {
            numVsTime = AIDA.defaultInstance().histogram2D("event num vs. elapsed time", timeRange, 0, timeRange, 1000, event.getEventNumber(), event.getEventNumber() + eventNumRange);
        }

        // Read EPICS data if available
        epicsData = EpicsData.read(event);

        if (epicsData != null) {
            LOGGER.info(epicsData.toString());
            if (epicsData.getKeys().contains("SVT:bias:top:0:v_sens")) {

                epicsBiasValue = epicsData.getValue("SVT:bias:top:0:v_sens");
                LOGGER.info("epicsBiasValue = " + Double.toString(epicsBiasValue));

                if (epicsBiasValue > 178.0) {
                    hvOnEpics = true;
                } else {
                    hvOnEpics = false;
                }
            }
        } else {
            LOGGER.fine("no epics information in this event");
        }

        // Read the timestamp for the event
        // It comes in on block level so not every event has it, use the latest one throughout a block
        Date newEventDate = TriggerDataUtils.getEventTimeStamp(event, triggerBankCollectionName);
        if (newEventDate != null) {
            if (firstDate == null) {
                firstDate = newEventDate;
            }
            hvOnConditions = svtBiasConstants.find(newEventDate) != null;
            if (eventDate == null || !eventDate.equals(newEventDate)) {
                System.out.format("event %d with new timestamp %s\n", event.getEventNumber(), newEventDate.toString());
                System.out.println("hvOnMya is " + (hvOnMya ? "ON" : "OFF") + " hvOnEpics " + (hvOnEpics ? "ON" : "OFF") + " hvOnConditions " + (hvOnConditions ? "ON" : "OFF") + " hvOnEventFlag " + (hvOnEventFlag ? "ON" : "OFF") + " for Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + newEventDate.toString() + " epoch " + newEventDate.getTime());
                // check what the DB has
                if (svtBiasConstants != null) {
                    LOGGER.info("there are " + svtBiasConstants.size() + " constants to search");
                    for (SvtBiasConstant constant : svtBiasConstants) {
                        LOGGER.info("start " + constant.getStart() + " end " + constant.getEnd() + " value " + constant.getValue());
                    }

                    SvtBiasConstant constant = svtBiasConstants.find(newEventDate);

                    LOGGER.info(constant == null ? "No constant found!" : ("Found constant " + "start " + constant.getStart() + " end " + constant.getEnd() + " value " + constant.getValue()));

                }
            }
            eventDate = newEventDate;
        }
        if (eventDate != null) {
            numVsTime.fill((eventDate.getTime() - firstDate.getTime()) / 1000, event.getEventNumber());
            biasVsTime.fill((eventDate.getTime() - firstDate.getTime()) / 1000, epicsBiasValue);
        }

        // only do this analysis where there is a date availabe.
        if (eventDate != null) {
            if (debug) {
                LOGGER.info("eventDate " + eventDate.toString());
            }

            eventCount++;

            if (runRange == null) {
                for (SvtBiasRunRange r : runRanges) {
                    if (r.getRun().getRun() == event.getRunNumber()) {
                        runRange = r;
                    }
                }
            }

            hvOnMya = runRange.includes(eventDate);

            // print the cases where epics and run range do not agree
            if (hvOnMya != hvOnEpics && epicsBiasValue > 0.) {
                if (debug) {
                    LOGGER.warning("hvOnMya is " + (hvOnMya ? "ON" : "OFF") + " hvOnEpics " + (hvOnEpics ? "ON" : "OFF") + " for Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOnMya ? "YES" : "NO") + " hvOnEpics " + (hvOnEpics ? "YES" : "NO"));
                }
                pWriter.println("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOn " + (hvOnMya ? "YES" : "NO"));
                eventCountEpicsDisagree++;
            }

            // print the cases where the HV is OFF
            if (!hvOnMya) {
                if (debug) {
                    LOGGER.info("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOnMya " + (hvOnMya ? "YES" : "NO") + " hvOnEpics " + (hvOnEpics ? "YES" : "NO"));
                }
                pWriter.println("Run " + event.getRunNumber() + " Event " + event.getEventNumber() + " date " + eventDate.toString() + " epoch " + eventDate.getTime() + " hvOnMya " + (hvOnMya ? "YES" : "NO") + " hvOnEpics " + (hvOnEpics ? "YES" : "NO"));
                eventCountHvOff++;
            }
            if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
                Map<HpsSiSensor, Integer> hitCountMap = new HashMap<HpsSiSensor, Integer>();

                // Get RawTrackerHit collection from event.
                List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

                for (RawTrackerHit hit : rawTrackerHits) {
                    HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                    Integer count = hitCountMap.get(sensor);
                    if (count == null) {
                        count = 0;
                    }
                    hitCountMap.put(sensor, count + 1);

                    int strip = hit.getIdentifierFieldValue("strip");
                    double pedestal = sensor.getPedestal(strip, 0);
                    hists_rawadc.get(sensor).fill(hit.getADCValues()[0] - pedestal);

                    int maxSample = 0;
                    double maxSampleValue = 0;
                    for (int s = 0; s < 6; ++s) {
                        if (((double) hit.getADCValues()[s] - pedestal) > maxSampleValue) {
                            maxSample = s;
                            maxSampleValue = ((double) hit.getADCValues()[s]) - pedestal;
                        }
                    }
                    if (maxSample >= 4) {
                        hists_rawadcnoise.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        if (hvOnMya) {
                            hists_rawadcnoiseON.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        } else {
                            hists_rawadcnoiseOFF.get(sensor).fill(hit.getADCValues()[0] - pedestal);
                        }
                    }
                }

                allHitCount.fill(rawTrackerHits.size());

//                if (dropSmallHitEvents && SvtPlotUtils.countSmallHits(rawTrackerHits) > 3) {
//                    return;
//                }
                if (hvOnMya) {
                    allHitCountON.fill(rawTrackerHits.size());
                } else {
                    allHitCountOFF.fill(rawTrackerHits.size());
                }
                allHitCountVsNum.fill(event.getEventNumber(), rawTrackerHits.size());
                if (eventDate != null) {
                    allHitCountVsTime.fill((eventDate.getTime() - firstDate.getTime()) / 1000, rawTrackerHits.size());
                }
                for (HpsSiSensor sensor : sensors) {
                    Integer count = hitCountMap.get(sensor);
                    if (count == null) {
                        count = 0;
                    }
                    hists_hitCounts.get(sensor).fill(count);
                    if (hvOnMya) {
                        hists_hitCountsON.get(sensor).fill(count);
                    } else {
                        hists_hitCountsOFF.get(sensor).fill(count);
                    }
                }
            }
        }
    }

    @Override
    public void endOfData() {

        LOGGER.info("eventCount " + Integer.toString(eventCount) + " eventCountHvOff " + Integer.toString(eventCountHvOff) + " eventCountEpicsDisagree " + Integer.toString(eventCountEpicsDisagree));
        pWriter.println("eventCount " + Integer.toString(eventCount) + " eventCountHvOff " + Integer.toString(eventCountHvOff) + " eventCountEpicsDisagree " + Integer.toString(eventCountEpicsDisagree));

        try {
            pWriter.close();
            fWriter.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

}
