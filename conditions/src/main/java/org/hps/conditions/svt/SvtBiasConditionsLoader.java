package org.hps.conditions.svt;

import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunRange;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtBiasMyaDataReader.SvtBiasMyaRange;
import org.hps.conditions.svt.SvtBiasMyaDataReader.SvtBiasRunRange;
import org.hps.conditions.svt.SvtMotorMyaDataReader.SvtPositionMyaRange;
import org.hps.conditions.svt.SvtMotorMyaDataReader.SvtPositionRunRange;
import org.hps.conditions.svt.SvtMotorPosition.SvtMotorPositionCollection;
import org.lcsim.util.aida.AIDA;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtBiasConditionsLoader {

    private static final Set<String> FIELDS = new HashSet<String>();
    
    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(SvtBiasConditionsLoader.class.getPackage().getName());

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
    private static final AIDA aida = AIDA.defaultInstance();
    static IDataPointSet dpsRuns = null;
    static IDataPointSet dpsBiasRuns = null;
    static IDataPointSet dpsPositionRuns = null;

    private static void setupPlots(boolean show) {
        IDataPointSetFactory dpsf = aida.analysisFactory().createDataPointSetFactory(aida.tree());
        dpsRuns = dpsf.create("dpsRuns", "Run intervals", 2);
        dpsBiasRuns = dpsf.create("dpsBiasRuns", "Bias ON intervals associated with runs", 2);
        dpsPositionRuns = dpsf.create("dpsPositionRuns", "Position stable intervals associated with runs", 2);
        IPlotter plotter = aida.analysisFactory().createPlotterFactory().create("Bias run ranges");
        IPlotterStyle plotterStyle = aida.analysisFactory().createPlotterFactory().createPlotterStyle();
        plotterStyle.xAxisStyle().setParameter("type", "date");
        plotter.createRegions(1, 4);
        plotter.region(0).plot(dpsRuns, plotterStyle);
        plotter.region(1).plot(dpsBiasRuns, plotterStyle);
        plotter.region(2).plot(dpsPositionRuns, plotterStyle);
        plotter.region(3).plot(dpsRuns, plotterStyle);
        plotter.region(3).plot(dpsBiasRuns, plotterStyle, "mode=overlay");
        plotter.region(3).plot(dpsPositionRuns, plotterStyle, "mode=overlay");
        if (show) {
            plotter.show();
        }

    }

    private static IDataPoint addPoint(IDataPointSet dps, long mstime, double val) {
        IDataPoint dp = dps.addPoint();
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
    private static boolean isValid(RunData data) {
        if (data.getStartDate() == null || data.getEndDate() == null || data.getStartDate().before((new GregorianCalendar(1999, 1, 1)).getTime())) {
            LOGGER.fine("This run data is not valid: " + data.toString());
            return false;
        }
        if (data.getStartDate().after(data.getEndDate())) {
            throw new RuntimeException("start date is after end date?!" + data.toString());
        }
        return true;
    }

    //private static Options options = null;
    public static RunMap getRunMapFromSpreadSheet(String path) {
        // Load in CSV records from the exported run spreadsheet.
        LOGGER.info(path);
        final RunSpreadsheet runSheet = new RunSpreadsheet(new File(path));

        // Find the run ranges that have the same fields values.
        final List<RunRange> ranges = RunRange.findRunRanges(runSheet, FIELDS);
        LOGGER.info("Found " + ranges.size() + " ranges.");
        for (RunRange range : ranges) {
            LOGGER.fine(range.toString());
        }
        // find the run records (has converted dates and stuff) for these ranges
        RunMap runmap = runSheet.getRunMap(ranges);
        LOGGER.info("Found " + runmap.size() + " runs in the run map.");
        return runmap;
    }

    public static List<RunData> getRunListFromSpreadSheet(String path) {
        // Load in CSV records from the exported run spreadsheet.
        List<RunData> runList = new ArrayList<RunData>();

        // find the run records (has converted dates and stuff) for these ranges
        RunMap runmap = getRunMapFromSpreadSheet(path);

        List<Integer> runNums = new ArrayList<Integer>(runmap.keySet());
        Collections.sort(runNums);
        for (Integer runNum : runNums) {
            RunData data = runmap.get(runNum);
            if (isValid(data)) {
                runList.add(data);
            }
        }
        return runList;
    }

    /**
     * Load SVT HV bias constants into the conditions database.
     *
     * @param args the command line arguments (requires a CVS run log file and a
     * MYA dump file.)
     */
    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("c", true, "CSV run file"));
        options.addOption(new Option("m", true, "MYA dump file for bias"));
        options.addOption(new Option("p", true, "MYA dump file for motor positions"));
        options.addOption(new Option("t", false, "use run table format (from crawler) for bias"));
        options.addOption(new Option("d", false, "discard first line of MYA data (for myaData output)"));
        options.addOption(new Option("g", false, "Actually load stuff into DB"));
        options.addOption(new Option("b", true, "beam current file"));
        options.addOption(new Option("s", false, "Show plots"));

        final CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse.", e);
        }

        if (!cl.hasOption("c") || (!cl.hasOption("m") && !cl.hasOption("p"))) {
            printUsage(options);
            return;
        }

        // Setup plots
        setupPlots(cl.hasOption("s"));

        // Load in CSV records from the exported run spreadsheet.
        List<RunData> runList;
        if (cl.hasOption("t")) {
            runList = SvtBiasMyaDataReader.readRunTable(new File(cl.getOptionValue("c")));
        } else {
            runList = getRunListFromSpreadSheet(cl.getOptionValue("c"));
        }

        List<SvtBiasRunRange> biasRunRanges = null;
        List<SvtPositionRunRange> positionRunRanges = null;
        // Load MYA dump
        if (cl.hasOption("m")) {
            List<SvtBiasMyaRange> biasRanges = SvtBiasMyaDataReader.readMyaData(new File(cl.getOptionValue("m")), 178.0, 2000, cl.hasOption("d"));
            LOGGER.info("Got " + biasRanges.size() + " bias ranges");
            biasRunRanges = SvtBiasMyaDataReader.findOverlappingRanges(runList, biasRanges);
        }

        if (cl.hasOption("p")) {
            List<SvtPositionMyaRange> positionRanges = SvtMotorMyaDataReader.readMyaData(new File(cl.getOptionValue("p")), 200, 10000);
            LOGGER.info("Got " + positionRanges.size() + " position ranges");
            positionRunRanges = SvtMotorMyaDataReader.findOverlappingRanges(runList, positionRanges);
        }

        // Combine them to run ranges when bias was on        
        // each run may have multiple bias ranges
        // fill graphs
        if (cl.hasOption("s")) {
            if (cl.hasOption("m")) {
                for (SvtBiasRunRange r : biasRunRanges) {
                    LOGGER.info(r.toString());
                    if (r.getRun().getRun() > 5600) {//9999999999999.0) {
                        //if(dpsRuns.size()/4.0<500) {//9999999999999.0) {
                        addPoint(dpsRuns, r.getRun().getStartDate().getTime(), 0.0);
                        addPoint(dpsRuns, r.getRun().getStartDate().getTime(), 1.0);
                        addPoint(dpsRuns, r.getRun().getEndDate().getTime(), 1.0);
                        addPoint(dpsRuns, r.getRun().getEndDate().getTime(), 0.0);

                        for (SvtBiasMyaRange br : r.getRanges()) {
                            addPoint(dpsBiasRuns, br.getStartDate().getTime(), 0.0);
                            addPoint(dpsBiasRuns, br.getStartDate().getTime(), 0.3);
                            addPoint(dpsBiasRuns, br.getEndDate().getTime(), 0.3);
                            addPoint(dpsBiasRuns, br.getEndDate().getTime(), 0.0);
                        }
                    }
                }
            }
            if (cl.hasOption("p")) {
                for (SvtPositionRunRange r : positionRunRanges) {
                    LOGGER.info(r.toString());
                    if (r.getRun().getRun() > 5600) {//9999999999999.0) {
                        //if(dpsRuns.size()/4.0<500) {//9999999999999.0) {
                        for (SvtPositionMyaRange br : r.getRanges()) {
                            addPoint(dpsPositionRuns, br.getStartDate().getTime(), 0.0);
                            addPoint(dpsPositionRuns, br.getStartDate().getTime(), 0.5 + 100 * Math.max(br.getTop(), br.getBottom()));
                            addPoint(dpsPositionRuns, br.getEndDate().getTime(), 0.5 + 100 * Math.max(br.getTop(), br.getBottom()));
                            addPoint(dpsPositionRuns, br.getEndDate().getTime(), 0.0);
                        }
                    }
                }
            }
        }

        if (cl.hasOption("b") && cl.hasOption("m") && cl.hasOption("p")) {
            readBeamData(new File(cl.getOptionValue("b")), runList, positionRunRanges, biasRunRanges);
        }

        // load to DB
        if (cl.hasOption("g")) {
            if (cl.hasOption("m")) {
                loadBiasesToConditionsDB(biasRunRanges);
            }
            if (cl.hasOption("p")) {
                loadPositionsToConditionsDB(positionRunRanges);
            }
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Need to adhere to these options", options);

    }

    private static void loadBiasesToConditionsDB(List<SvtBiasRunRange> ranges) {
        LOGGER.info("Load to DB...");

        // Create a new collection for each run
        List<Integer> runsadded = new ArrayList<Integer>();

        for (SvtBiasRunRange range : ranges) {
            LOGGER.info("Loading " + range.toString());
            RunData rundata = range.getRun();
            if (runsadded.contains(rundata.getRun())) {
                LOGGER.warning("Run " + Integer.toString(rundata.getRun()) + " was already added?");
                throw new RuntimeException("Run " + Integer.toString(rundata.getRun()) + " was already added?");
            }
            runsadded.add(rundata.getRun());

            if (range.getRanges().isEmpty()) {
                LOGGER.info("No bias range for run " + range.getRun().getRun());
                continue;
            }

            // create a collection
            SvtBiasConstantCollection collection = new SvtBiasConstantCollection();

            // Set the table meta data
            collection.setTableMetaData(MANAGER.findTableMetaData("svt_bias_constants"));
            collection.setConnection(MANAGER.getConnection());

            int collectionId = -1;
            try {
                collectionId = MANAGER.getCollectionId(collection, "run ranges for SVT HV bias ON");
            } catch (SQLException e1) {
                throw new RuntimeException(e1);
            }

            collection.setCollectionId(collectionId);

            final ConditionsRecord condition = new ConditionsRecord();
            condition.setFieldValue("run_start", rundata.getRun());
            condition.setFieldValue("run_end", rundata.getRun());
            condition.setFieldValue("name", "svt_bias_constants");
            condition.setFieldValue("table_name", "svt_bias_constants");
            condition.setFieldValue("notes", "constants from mya");
            condition.setFieldValue("created", new Date());
            condition.setFieldValue("created_by", System.getProperty("user.name"));
            condition.setFieldValue("collection_id", collectionId);
            condition.setTableMetaData(MANAGER.findTableMetaData("conditions"));
            condition.setConnection(MANAGER.getConnection());
            LOGGER.info(condition.toString());

            try {

                for (SvtBiasMyaRange biasRange : range.getRanges()) {
                    // create a constant and add to the collection
                    final SvtBiasConstant constant = new SvtBiasConstant();
                    constant.setFieldValue("start", biasRange.getStartDate().getTime());
                    constant.setFieldValue("end", biasRange.getEndDate().getTime());
                    constant.setFieldValue("value", biasRange.getValue());
                    collection.add(constant);
                }

                // Insert collection data.
                collection.insert();

                // Insert conditions record.
                condition.insert();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void loadPositionsToConditionsDB(List<SvtPositionRunRange> ranges) {
        LOGGER.info("Load to DB...");

        // Create a new collection for each run
        List<Integer> runsadded = new ArrayList<Integer>();

        for (SvtPositionRunRange range : ranges) {
            LOGGER.info("Loading " + range.toString());
            RunData rundata = range.getRun();
            if (runsadded.contains(rundata.getRun())) {
                LOGGER.warning("Run " + Integer.toString(rundata.getRun()) + " was already added?");
                throw new RuntimeException("Run " + Integer.toString(rundata.getRun()) + " was already added?");
            }
            runsadded.add(rundata.getRun());

            if (range.getRanges().isEmpty()) {
                LOGGER.info("No position range for run " + range.getRun().getRun());
                continue;
            }

            // create a collection
            SvtMotorPositionCollection collection = new SvtMotorPositionCollection();

            // Set the table meta data
            collection.setTableMetaData(MANAGER.findTableMetaData("svt_motor_positions"));
            collection.setConnection(MANAGER.getConnection());

            int collectionId = -1;
            try {
                collectionId = MANAGER.getCollectionId(collection, "run ranges for SVT positions");
            } catch (SQLException e1) {
                throw new RuntimeException(e1);
            }

            collection.setCollectionId(collectionId);

            final ConditionsRecord condition = new ConditionsRecord();
            condition.setFieldValue("run_start", rundata.getRun());
            condition.setFieldValue("run_end", rundata.getRun());
            condition.setFieldValue("name", "svt_motor_positions");
            condition.setFieldValue("table_name", "svt_motor_positions");
            condition.setFieldValue("notes", "constants from mya");
            condition.setFieldValue("created", new Date());
            condition.setFieldValue("created_by", System.getProperty("user.name"));
            condition.setFieldValue("collection_id", collectionId);
            condition.setTableMetaData(MANAGER.findTableMetaData("conditions"));
            condition.setConnection(MANAGER.getConnection());
            LOGGER.info(condition.toString());

            try {

                for (SvtPositionMyaRange positionRange : range.getRanges()) {
                    // create a constant and add to the collection
                    final SvtMotorPosition constant = new SvtMotorPosition();
                    constant.setFieldValue("start", positionRange.getStartDate().getTime());
                    constant.setFieldValue("end", positionRange.getEndDate().getTime());
                    constant.setFieldValue("top", positionRange.getTop());
                    constant.setFieldValue("bottom", positionRange.getBottom());
                    collection.add(constant);
                }

                // Insert collection data.
                collection.insert();

                // Insert conditions record.
                condition.insert();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void readBeamData(File file, List<RunData> runList, List<SvtPositionRunRange> positionRanges, List<SvtBiasRunRange> biasRanges) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        Map<Integer, SvtPositionRunRange> positionRangeMap = new HashMap<Integer, SvtPositionRunRange>();
        for (SvtPositionRunRange range : positionRanges) {
            positionRangeMap.put(range.getRun().getRun(), range);
        }
        Map<Integer, SvtBiasRunRange> biasRangeMap = new HashMap<Integer, SvtBiasRunRange>();
        for (SvtBiasRunRange range : biasRanges) {
            biasRangeMap.put(range.getRun().getRun(), range);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            System.out.println("myaData header: " + br.readLine()); //discard the first line
            System.out.println("run\ttotalQ\ttotalQBias\tfracBias\ttotalQNom\tfracNom\ttotalQ1pt5\tfrac1pt5\ttotalGatedQ\ttotalGatedQBias\tfracGatedBias\ttotalGatedQNom\tfracGatedNom\ttotalGatedQ1pt5\tfracGated1pt5");

            for (RunData run : runList) {
                double totalCharge = 0;
                double totalChargeWithBias = 0;
                double totalChargeWithBiasAtNominal = 0;
                double totalChargeWithBiasAt1pt5 = 0;
                double totalGatedCharge = 0;
                double totalGatedChargeWithBias = 0;
                double totalGatedChargeWithBiasAtNominal = 0;
                double totalGatedChargeWithBiasAt1pt5 = 0;
                Date lastDate = null;

                while ((line = br.readLine()) != null) {
                    String arr[] = line.split(" +");

                    if (arr.length != 4) {
                        throw new java.text.ParseException("this line is not correct.", 0);
                    }
                    Date date = dateFormat.parse(arr[0] + " " + arr[1]);
                    if (date.after(run.getEndDate())) {
                        break;
                    }
                    if (date.before(run.getStartDate())) {
                        continue;
                    }

                    double current, livetime;
                    if (arr[2].equals("<undefined>")) {
                        current = 0;
                    } else {
                        current = Double.parseDouble(arr[2]);
                    }
                    if (arr[3].equals("<undefined>")) {
                        livetime = 0;
                    } else {
                        livetime = Math.min(100.0, Math.max(0.0, Double.parseDouble(arr[3]))) / 100.0;
                    }

                    if (date.after(run.getStartDate())) {
                        if (lastDate != null) {
                            double dt = (date.getTime() - lastDate.getTime()) / 1000.0;
                            double dq = dt * current; // nC
                            double dqGated = dt * current * livetime; // nC

                            totalCharge += dq;
                            totalGatedCharge += dqGated;
                            SvtBiasRunRange biasRunRange = biasRangeMap.get(run.getRun());
                            if (biasRunRange != null) {
                                for (SvtBiasMyaRange biasRange : biasRunRange.getRanges()) {
                                    if (biasRange.includes(date)) {
                                        totalChargeWithBias += dq;
                                        totalGatedChargeWithBias += dqGated;

                                        SvtPositionRunRange positionRunRange = positionRangeMap.get(run.getRun());
                                        if (positionRunRange != null) {
                                            for (SvtPositionMyaRange positionRange : positionRunRange.getRanges()) {
                                                if (positionRange.includes(date)) {
                                                    if (Math.abs(positionRange.getBottom()) < 0.0001 && Math.abs(positionRange.getTop()) < 0.0001) {
                                                        totalChargeWithBiasAtNominal += dq;
                                                        totalGatedChargeWithBiasAtNominal += dqGated;
                                                    } else if (Math.abs(positionRange.getBottom() - 0.0033) < 0.0001 && Math.abs(positionRange.getTop() - 0.0031) < 0.0001) {
                                                        totalChargeWithBiasAt1pt5 += dq;
                                                        totalGatedChargeWithBiasAt1pt5 += dqGated;
                                                    }
                                                    break;
                                                }
                                            }
                                        }

                                        break;
                                    }
                                }
                            }

                        }
                    }
                    lastDate = date;
                }
//                System.out.format("run\t%d\ttotalQ\t%.0f\ttotalQBias\t%.0f\tfracBias\t%f\ttotalQNom\t%.0f\tfracNom\t%f\ttotalQ1pt5\t%.0f\tfrac1pt5\t%f\ttotalGatedQ\t%.0f\ttotalGatedQBias\t%.0f\tfracGatedBias\t%f\ttotalGatedQNom\t%.0f\tfracGatedNom\t%f\ttotalGatedQ1pt5\t%.0f\tfracGated1pt5\t%f\n", run.getRun(), totalCharge, totalChargeWithBias, totalChargeWithBias / totalCharge, totalChargeWithBiasAtNominal, totalChargeWithBiasAtNominal / totalCharge, totalChargeWithBiasAt1pt5, totalChargeWithBiasAt1pt5 / totalCharge, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBias / totalGatedCharge, totalGatedChargeWithBiasAtNominal, totalGatedChargeWithBiasAtNominal / totalGatedCharge, totalGatedChargeWithBiasAt1pt5, totalGatedChargeWithBiasAt1pt5 / totalGatedCharge);
                System.out.format("%d\t%.0f\t%.0f\t%f\t%.0f\t%f\t%.0f\t%f\t%.0f\t%.0f\t%f\t%.0f\t%f\t%.0f\t%f\n", run.getRun(), totalCharge, totalChargeWithBias, totalChargeWithBias / totalCharge, totalChargeWithBiasAtNominal, totalChargeWithBiasAtNominal / totalCharge, totalChargeWithBiasAt1pt5, totalChargeWithBiasAt1pt5 / totalCharge, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBias / totalGatedCharge, totalGatedChargeWithBiasAtNominal, totalGatedChargeWithBiasAtNominal / totalGatedCharge, totalGatedChargeWithBiasAt1pt5, totalGatedChargeWithBiasAt1pt5 / totalGatedCharge);
            }
            br.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
