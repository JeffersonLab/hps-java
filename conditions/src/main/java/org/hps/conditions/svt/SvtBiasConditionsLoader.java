/**
 * 
 */
package org.hps.conditions.svt;

import hep.aida.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunRange;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasMyaRange;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasMyaRanges;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasRunRange;
import org.hps.util.BasicLogFormatter;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtBiasConditionsLoader {

    private static final Set<String> FIELDS = new HashSet<String>();
    private static Logger logger = LogUtil.create(SvtBiasConditionsLoader.class, new BasicLogFormatter(),Level.INFO);
    

    /**
     * Setup conditions.
     */
    //private static final DatabaseConditionsManager MANAGER = DatabaseConditionsManager.getInstance();
    static {
        FIELDS.add("run");
        FIELDS.add("date");
        FIELDS.add("start_time");
        FIELDS.add("end_time");
    }

    /**
     * Setup control plots.
     */
    private static AIDA aida = AIDA.defaultInstance();
    static IDataPointSet dpsRuns = null;
    static IDataPointSet dpsBiasRuns = null;
    
    private static void setupPlots() {
        IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(aida.tree());
        dpsRuns = dpsf.create("dpsRuns","Run intervals", 2);
        dpsBiasRuns = dpsf.create("dpsBiasRuns","Bias ON intervals associated with runs", 2);
        IPlotter plotter = aida.analysisFactory().createPlotterFactory().create("Bias run ranges");
        IPlotterStyle plotterStyle = aida.analysisFactory().createPlotterFactory().createPlotterStyle();
        plotterStyle.xAxisStyle().setParameter("type", "date");
        plotter.createRegions(1, 3);
        plotter.region(0).plot(dpsRuns,plotterStyle);
        plotter.region(1).plot(dpsBiasRuns,plotterStyle);
        plotter.region(2).plot(dpsRuns,plotterStyle);
        plotter.region(2).plot(dpsBiasRuns,plotterStyle,"mode=overlay");
        plotter.show();
        
    }
    
    private static IDataPoint addPoint(IDataPointSet dps, long mstime, double val) {
        IDataPoint dp = dps.addPoint();
        dp.coordinate(0).setValue(mstime/1000.);
        dp.coordinate(1).setValue(val);
        return dp;
    }
    
    
    /**
     * 
     */
    public SvtBiasConditionsLoader() {
    }

    
    
    
    private static boolean isValid(RunData data) {
        if(data.getStartDate() == null || data.getEndDate() == null || data.getStartDate().before(new Date(99,1,1))) {
            logger.warning("This run data is not valid: " + data.toString());
            return false;
        } 
        if (data.getStartDate().after(data.getEndDate())) {
            throw new RuntimeException("start date is after end date?!" + data.toString());
        }
        return true;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        setupPlots();

        
     // Load in CSV records from the exported run spreadsheet.
        final String path = args[0];
        logger.info(args[0]);
        final RunSpreadsheet runSheet = new RunSpreadsheet(new File(path));

        // Find the run ranges that have the same fields values.
        final List<RunRange> ranges = RunRange.findRunRanges(runSheet, FIELDS);
        logger.info("Found " + ranges.size() + " ranges.");
        for(RunRange range : ranges) logger.info(range.toString());
        // find the run records (has converted dates and stuff) for these ranges
        RunMap runmap  = runSheet.getRunMap(ranges);
        logger.info("Found " + runmap.size() + " runs in the run map.");
        
        
        
        // Load MYA dump
        SvtBiasMyaDumpReader biasMyaReader = new SvtBiasMyaDumpReader(args[1]);
        logger.info("Got " + biasMyaReader.getRanges().size() + " bias ranges");
        
        
        // Combine them to run ranges when bias was on        
        // each run may have multiple bias ranges
        
        List<SvtBiasRunRange> biasRunRanges = new ArrayList<SvtBiasRunRange>();
        // loop over runs from CSV        
        RunData prev = null;
        for(Entry<Integer,RunData> entry : runmap.entrySet()) {
            int run = entry.getKey();
            RunData data = entry.getValue();
            logger.info("Processing " + run + " " + data.toString());
            
            //check that data is ok
            if (isValid(data)) {
                if(prev!=null) {
                    if(isValid(prev)) {
                        if(prev.getEndDate().after(data.getStartDate())) {
                            throw new RuntimeException("prev end date after run started?: " + prev.toString() + "   " + data.toString());
                        } else if(prev.getStartDate().after(data.getEndDate())) {
                            throw new RuntimeException("prev start date before run ended?: " + prev.toString() + "   " + data.toString());
                        }
                    }
                }
                
                // find the bias ranges applicable to this run
                SvtBiasMyaRanges overlaps = biasMyaReader.findOverlappingRanges(data.getStartDate(), data.getEndDate());
                logger.fine("Found " + overlaps.size() + " overlapping bias ranges");
                logger.fine(overlaps.toString());

                biasRunRanges.add(new SvtBiasRunRange(data,overlaps));
                prev = data;

            }
        }
        
        

        for(SvtBiasRunRange r : biasRunRanges) {
            logger.info(r.toString());
            if(r.getRun().getRun()>5600) {//9999999999999.0) {
                //if(dpsRuns.size()/4.0<500) {//9999999999999.0) {
                addPoint(dpsRuns, r.getRun().getStartDate().getTime(),0.0);
                addPoint(dpsRuns, r.getRun().getStartDate().getTime(),1.0);
                addPoint(dpsRuns, r.getRun().getEndDate().getTime(),1.0);
                addPoint(dpsRuns, r.getRun().getEndDate().getTime(),0.0);

                for(SvtBiasMyaRange br : r.getRanges()) {
                    addPoint(dpsBiasRuns, br.getStart().getDate().getTime(),0.0);
                    addPoint(dpsBiasRuns, br.getStart().getDate().getTime(),0.5);
                    addPoint(dpsBiasRuns, br.getEnd().getDate().getTime(),0.5);
                    addPoint(dpsBiasRuns, br.getEnd().getDate().getTime(),0.0);
                }
                

            }   

        }
        
        
        
    }

    
   
    

}
