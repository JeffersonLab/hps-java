package org.hps.users.meeg;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtMotorPosition;
import org.hps.conditions.svt.SvtMotorPosition.SvtMotorPositionCollection;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.run.database.RunManager;

/**
 * @author Sho Uemura <meeg@slac.stanford.edu>
 *
 */
public class SvtChargeIntegrator {

    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(SvtChargeIntegrator.class.getPackage().getName());

    private static final double angleTolerance = 1e-4;
    private static final double burstModeNoiseEfficiency = 0.965;

    /**
     *
     * @param args the command line arguments (requires a CVS run log file and a
     * MYA dump file.)
     */
    public static void main(String[] args) {

        Options options = new Options();
//        options.addOption(new Option("c", true, "CSV run file"));
//        options.addOption(new Option("m", true, "MYA dump file for bias"));
//        options.addOption(new Option("p", true, "MYA dump file for motor positions"));
//        options.addOption(new Option("t", false, "use run table format (from crawler) for bias"));
//        options.addOption(new Option("d", false, "discard first line of MYA data (for myaData output)"));
//        options.addOption(new Option("g", false, "Actually load stuff into DB"));
//        options.addOption(new Option("b", true, "beam current file"));
//        options.addOption(new Option("s", false, "Show plots"));

        final CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse.", e);
        }

//        if (!cl.hasOption("c") || (!cl.hasOption("m") && !cl.hasOption("p"))) {
//            printUsage(options);
//            return;
//        }
        List<CSVRecord> records = null;
        try {
            FileReader reader = new FileReader(cl.getArgs()[0]);
            final CSVFormat format = CSVFormat.DEFAULT;

            final CSVParser csvParser;
            csvParser = new CSVParser(reader, format);

            records = csvParser.getRecords();

//            // Remove first two rows of headers.
//            records.remove(0);
//            records.remove(0);
            csvParser.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }

//        for (CSVRecord record : records) {
//            int runNum = Integer.parseInt(record.get(0));
//            int fileNum = Integer.parseInt(record.get(1));
////            int nEvents = Integer.parseInt(record.get(2));
////            int badEvents = Integer.parseInt(record.get(3));
////            int firstTimestamp = Integer.parseInt(record.get(4));
////            int lastTimestamp = Integer.parseInt(record.get(5));
//            long firstTI = Long.parseLong(record.get(6));
//            long lastTI = Long.parseLong(record.get(7));
////            long tiOffset = Long.parseLong(record.get(8));
////            data.add(new FileData(runNum, fileNum, firstTI, lastTI, record));
//        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        try {
            BufferedReader br = new BufferedReader(new FileReader(cl.getArgs()[1]));
            String line;
            System.err.println("myaData header: " + br.readLine()); //discard the first line
            if (perRun) {
                System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
            } else {
                System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
            }

            int currentRun = 0;
            double nominalAngleTop = -999;
            double nominalAngleBottom = -999;
            String nominalPosition = null;
            long tiTimeOffset = 0;
            double efficiency = 0;
            SvtBiasConstantCollection svtBiasConstants = null;
            SvtMotorPositionCollection svtPositionConstants = null;
            SvtAlignmentConstant.SvtAlignmentConstantCollection alignmentConstants = null;
            Date date = null;
            Date lastDate = null;

            for (CSVRecord record : records) {
                int runNum = Integer.parseInt(record.get(0));
                int fileNum = Integer.parseInt(record.get(1));
                long firstTI = Long.parseLong(record.get(6));
                long lastTI = Long.parseLong(record.get(7));

                if (runNum != currentRun) {
                    RunManager.getRunManager().setRun(runNum);
                    if (!RunManager.getRunManager().runExists() || RunManager.getRunManager().getRunSummary().getTiTimeOffset() == null) {
                        continue;
                    }
                    try {
                        DatabaseConditionsManager.getInstance().setDetector("HPS-EngRun2015-Nominal-v3", runNum);

                        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
                        svtPositionConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtMotorPosition.SvtMotorPositionCollection.class, "svt_motor_positions").getCachedData();
                        alignmentConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtAlignmentConstant.SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();
                    } catch (Exception ex) {
                        continue;
                    }

                    tiTimeOffset = RunManager.getRunManager().getRunSummary().getTiTimeOffset();

                    for (final SvtAlignmentConstant constant : alignmentConstants) {
                        switch (constant.getParameter()) {
                            case 13100:
                                nominalAngleTop = constant.getValue();
                                break;
                            case 23100:
                                nominalAngleBottom = -constant.getValue();
                                break;
                        }
                    }

                    if (Math.abs(nominalAngleBottom) < angleTolerance && Math.abs(nominalAngleTop) < angleTolerance) {
                        nominalPosition = "0pt5";
                    } else if (Math.abs(nominalAngleBottom - 0.0033) < angleTolerance && Math.abs(nominalAngleTop - 0.0031) < angleTolerance) {
                        nominalPosition = "1pt5";
                    } else {
                        nominalPosition = "unknown";
                    }
                    efficiency = burstModeNoiseEfficiency;
                    SvtTimingConstants svtTimingConstants;
                    try {
                        svtTimingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
                    } catch (Exception ex) {
                        svtTimingConstants = null;
                    }
                    if (svtTimingConstants != null) {
                        if (svtTimingConstants.getOffsetTime() > 27) {
                            efficiency *= 2.0 / 3.0; // bad latency: drop 2 out of 6 trigger phases
                        }// otherwise, we have good latency
                    } else {
                        efficiency = 0;
                    }//no latency info in conditions: give up
                    currentRun = runNum;
                }

                Date startDate = new Date((long) ((firstTI + tiTimeOffset) / 1e6));
                Date endDate = new Date((long) ((lastTI + tiTimeOffset) / 1e6));

                double totalCharge = 0;
                double totalChargeWithBias = 0;
                double totalChargeWithBiasAtNominal = 0;
                double totalGatedCharge = 0;
                double totalGatedChargeWithBias = 0;
                double totalGatedChargeWithBiasAtNominal = 0;
                double totalGoodCharge = 0;
                double totalGoodChargeWithBias = 0;
                double totalGoodChargeWithBiasAtNominal = 0;
                br.mark(1000);

                while ((line = br.readLine()) != null) {
                    String arr[] = line.split(" +");

                    if (arr.length != 4) {
                        throw new RuntimeException("this line is not correct.");
                    }
                    lastDate = date;
                    date = dateFormat.parse(arr[0] + " " + arr[1]);
                    if (date.before(startDate)) { //not in the file's time range yet; keep reading the file 
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

                    boolean biasGood = false;
                    boolean positionGood = false;

                    SvtBiasConstant biasConstant = svtBiasConstants.find(date);
                    if (biasConstant != null) {
                        biasGood = true;
                    }
                    if (svtPositionConstants != null) {
                        SvtMotorPosition positionConstant = svtPositionConstants.find(date);
                        if (positionConstant != null) {
//                    System.out.format("%f %f %f %f\n", positionConstant.getBottom(), nominalAngleBottom, positionConstant.getTop(), nominalAngleTop);
                            if (Math.abs(positionConstant.getBottom() - nominalAngleBottom) < angleTolerance && Math.abs(positionConstant.getTop() - nominalAngleTop) < angleTolerance) {
                                positionGood = true;
                            }
                        }
                    }

                    if (lastDate != null) {
                        double dt = (Math.min(date.getTime(), endDate.getTime()) - Math.max(startDate.getTime(), lastDate.getTime())) / 1000.0;
//                        System.out.format("start %d end %d date %d lastDate %d current %f dt %f\n", startDate.getTime(), endDate.getTime(), date.getTime(), lastDate.getTime(), current, dt);
                        totalCharge += dt * current; // nC
                        totalGatedCharge += dt * current * livetime;
                        totalGoodCharge += dt * current * livetime * efficiency;
                        if (biasGood) {
                            totalChargeWithBias += dq;
                            totalGatedChargeWithBias += dqGated;
                            totalGoodChargeWithBias += biasDt * current * livetime * efficiency;
                            if (positionGood) {
                                totalChargeWithBiasAtNominal += dq;
                                totalGatedChargeWithBiasAtNominal += dqGated;
                                totalGoodChargeWithBiasAtNominal += positionDt * current * livetime * efficiency;
                            }
                        }
                    }
                    if (date.after(endDate)) {//this is the last interval overlapping the file's time range; backtrack so this line will be read again for the next file
                        date = lastDate;
                        br.reset();
                        break;
                    }
                    br.mark(1000);
                }
                if (perRun) {
                    int nEvents = Integer.parseInt(record.get(9));
                    System.out.format("%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal);
                } else {
                    int fileNum = Integer.parseInt(record.get(1));
                    int nEvents = Integer.parseInt(record.get(2));
                    System.out.format("%d\t%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, fileNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Need to adhere to these options", options);

    }
}
