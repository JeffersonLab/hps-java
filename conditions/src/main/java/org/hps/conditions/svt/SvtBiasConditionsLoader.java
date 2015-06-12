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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunRange;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasMyaRange;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasMyaRanges;
import org.hps.conditions.svt.SvtBiasMyaDumpReader.SvtBiasRunRange;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;
import org.hps.util.BasicLogFormatter;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class SvtBiasConditionsLoader {

    private static final Set<String> FIELDS = new HashSet<String>();
    private static Logger logger = LogUtil.create(SvtBiasConditionsLoader.class, new BasicLogFormatter(), Level.INFO);

    /**
     * Setup conditions.
     */
    private static final DatabaseConditionsManager MANAGER = DatabaseConditionsManager.getInstance();
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

    private static void setupPlots(final boolean show) {
        final IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(aida.tree());
        dpsRuns = dpsf.create("dpsRuns", "Run intervals", 2);
        dpsBiasRuns = dpsf.create("dpsBiasRuns", "Bias ON intervals associated with runs", 2);
        final IPlotter plotter = aida.analysisFactory().createPlotterFactory().create("Bias run ranges");
        final IPlotterStyle plotterStyle = aida.analysisFactory().createPlotterFactory().createPlotterStyle();
        plotterStyle.xAxisStyle().setParameter("type", "date");
        plotter.createRegions(1, 3);
        plotter.region(0).plot(dpsRuns, plotterStyle);
        plotter.region(1).plot(dpsBiasRuns, plotterStyle);
        plotter.region(2).plot(dpsRuns, plotterStyle);
        plotter.region(2).plot(dpsBiasRuns, plotterStyle, "mode=overlay");
        if (show) {
            plotter.show();
        }

    }

    private static IDataPoint addPoint(final IDataPointSet dps, final long mstime, final double val) {
        final IDataPoint dp = dps.addPoint();
        dp.coordinate(0).setValue(mstime / 1000.);
        dp.coordinate(1).setValue(val);
        return dp;
    }

    /**
     * Default constructor
     */
    public SvtBiasConditionsLoader() {
    }

    /**
     * Check validity of @link RunData
     *
     * @param data the @link RunData to check
     * @return <code>true</code> if valid, <code>false</code> otherwise.
     */
    private static boolean isValid(final RunData data) {
        if (data.getStartDate() == null || data.getEndDate() == null || data.getStartDate().before(new Date(99, 1, 1))) {
            logger.warning("This run data is not valid: " + data.toString());
            return false;
        }
        if (data.getStartDate().after(data.getEndDate())) {
            throw new RuntimeException("start date is after end date?!" + data.toString());
        }
        return true;
    }

    // private static Options options = null;

    /**
     * Load SVT HV bias constants into the conditions database.
     *
     * @param args the command line arguments (requires a CVS run log file and a MYA dump file.)
     */
    public static void main(final String[] args) {

        final Options options = new Options();
        options.addOption(new Option("c", true, "CVS run file"));
        options.addOption(new Option("m", true, "MYA dump file"));
        options.addOption(new Option("g", false, "Actually load stuff into DB"));
        options.addOption(new Option("s", false, "Show plots"));

        final CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (final ParseException e) {
            throw new RuntimeException("Cannot parse.", e);
        }

        // Setup plots
        setupPlots(cl.hasOption("s") ? true : false);

        // Load in CSV records from the exported run spreadsheet.
        final String path = cl.getOptionValue("c");
        logger.info(path);
        final RunSpreadsheet runSheet = new RunSpreadsheet(new File(path));

        // Find the run ranges that have the same fields values.
        final List<RunRange> ranges = RunRange.findRunRanges(runSheet, FIELDS);
        logger.info("Found " + ranges.size() + " ranges.");
        for (final RunRange range : ranges) {
            logger.info(range.toString());
        }
        // find the run records (has converted dates and stuff) for these ranges
        final RunMap runmap = runSheet.getRunMap(ranges);
        logger.info("Found " + runmap.size() + " runs in the run map.");

        // Load MYA dump
        final SvtBiasMyaDumpReader biasMyaReader = new SvtBiasMyaDumpReader(cl.getOptionValue("m"));
        logger.info("Got " + biasMyaReader.getRanges().size() + " bias ranges");

        // Combine them to run ranges when bias was on
        // each run may have multiple bias ranges

        final List<SvtBiasRunRange> biasRunRanges = new ArrayList<SvtBiasRunRange>();
        // loop over runs from CSV
        RunData prev = null;
        for (final Entry<Integer, RunData> entry : runmap.entrySet()) {
            final int run = entry.getKey();
            final RunData data = entry.getValue();
            logger.info("Processing " + run + " " + data.toString());

            // check that data is ok
            if (isValid(data)) {
                if (prev != null) {
                    if (isValid(prev)) {
                        if (prev.getEndDate().after(data.getStartDate())) {
                            throw new RuntimeException("prev end date after run started?: " + prev.toString() + "   "
                                    + data.toString());
                        } else if (prev.getStartDate().after(data.getEndDate())) {
                            throw new RuntimeException("prev start date before run ended?: " + prev.toString() + "   "
                                    + data.toString());
                        }
                    }
                }

                // find the bias ranges applicable to this run
                final SvtBiasMyaRanges overlaps = biasMyaReader.findOverlappingRanges(data.getStartDate(),
                        data.getEndDate());
                logger.fine("Found " + overlaps.size() + " overlapping bias ranges");
                logger.fine(overlaps.toString());

                biasRunRanges.add(new SvtBiasRunRange(data, overlaps));
                prev = data;

            }
        }

        // fill graphs
        if (cl.hasOption("s")) {
            for (final SvtBiasRunRange r : biasRunRanges) {
                logger.info(r.toString());
                if (r.getRun().getRun() > 5600) {// 9999999999999.0) {
                    // if(dpsRuns.size()/4.0<500) {//9999999999999.0) {
                    addPoint(dpsRuns, r.getRun().getStartDate().getTime(), 0.0);
                    addPoint(dpsRuns, r.getRun().getStartDate().getTime(), 1.0);
                    addPoint(dpsRuns, r.getRun().getEndDate().getTime(), 1.0);
                    addPoint(dpsRuns, r.getRun().getEndDate().getTime(), 0.0);

                    for (final SvtBiasMyaRange br : r.getRanges()) {
                        addPoint(dpsBiasRuns, br.getStart().getDate().getTime(), 0.0);
                        addPoint(dpsBiasRuns, br.getStart().getDate().getTime(), 0.5);
                        addPoint(dpsBiasRuns, br.getEnd().getDate().getTime(), 0.5);
                        addPoint(dpsBiasRuns, br.getEnd().getDate().getTime(), 0.0);
                    }

                }

            }
        }

        // load to DB
        loadToConditionsDB(biasRunRanges, cl.hasOption("g") ? true : false);

    }

    private final static SvtBiasConstantCollection findCollection(final List<SvtBiasConstantCollection> list,
            final Date date) {
        for (final SvtBiasConstantCollection collection : list) {
            if (collection.find(date) != null) {
                return collection;
            }
        }
        return null;
    }

    private static final void loadToConditionsDB(final List<SvtBiasRunRange> ranges, final boolean doIt) {
        logger.info("Load to DB...");

        // Create a new collection for each run
        final List<Integer> runsadded = new ArrayList<Integer>();

        for(final SvtBiasRunRange range : ranges) {
            logger.info("Loading " + range.toString());
            final RunData rundata = range.getRun();
            if(runsadded.contains(rundata.getRun())) {
                logger.warning("Run " + Integer.toString(rundata.getRun()) + " was already added?");
                throw new RuntimeException("Run " + Integer.toString(rundata.getRun()) + " was already added?");
            }
            runsadded.add(rundata.getRun());
            for (final SvtBiasMyaRange biasRange : range.getRanges()) {
                //create a collection
                final SvtBiasConstantCollection collection = new SvtBiasConstantCollection();
                //create a constant and add to the collection
                final SvtBiasConstant constant = new SvtBiasConstant();
                constant.setFieldValue("start", biasRange.getStartDate());
                constant.setFieldValue("end", biasRange.getEndDate());
                constant.setFieldValue("value", biasRange.getStart().getValue());
                try {
                    collection.add(constant);
                } catch (final ConditionsObjectException e) {
                    throw new RuntimeException(e);
                }

                final ConditionsRecord condition = new ConditionsRecord();
                condition.setFieldValue("run_start", rundata.getRun());
                condition.setFieldValue("run_end", rundata.getRun());
                condition.setFieldValue("name", "svt_bias");
                condition.setFieldValue("table_name", "svt_bias");
                condition.setFieldValue("notes", "constants from mya");
                condition.setFieldValue("created", new Date());
                condition.setFieldValue("created_by", System.getProperty("user.name"));

                condition.setFieldValue("collection_id", collection.getCollectionId());

                logger.info(condition.toString());

                if(doIt) {
                    try {
                        condition.insert();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }


    }
}
