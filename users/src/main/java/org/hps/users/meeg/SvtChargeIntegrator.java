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
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

    private static final double angleTolerance = 1e-4;
    private static final double burstModeNoiseEfficiency = 0.965;

    /**
     *
     * @param args the command line arguments (requires a CSV run/file log file
     * and a MYA dump file.)
     */
    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("r", false, "use per-run CSV log file (default is per-file)"));
        options.addOption(new Option("t", false, "use TI timestamp instead of Unix time (higher precision, but requires TI time offset in run DB)"));
        options.addOption(new Option("c", false, "get TI time offset from CSV log file instead of run DB"));

        final CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse.", e);
        }

        boolean perRun = cl.hasOption("r");
        boolean useTI = cl.hasOption("t");
        boolean useCrawlerTI = cl.hasOption("c");

        if (cl.getArgs().length != 2) {
            printUsage(options);
            return;
        }

        List<CSVRecord> records = null;
        try {
            FileReader reader = new FileReader(cl.getArgs()[0]);
            final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

            records = csvParser.getRecords();

            csvParser.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
        }

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
            Date lastDate;

            for (CSVRecord record : records) {
                int runNum = Integer.parseInt(record.get(0));
                if (useCrawlerTI) {
                    if (perRun) {
                        tiTimeOffset = Long.parseLong(record.get(12));
                    } else {
                        tiTimeOffset = Long.parseLong(record.get(8));
                    }
                    if (tiTimeOffset == 0) {
                        continue;
                    }
                }

                if (runNum != currentRun) {
                    if (useTI && !useCrawlerTI) {
                        RunManager.getRunManager().setRun(runNum);
                        if (!RunManager.getRunManager().runExists() || RunManager.getRunManager().getTriggerConfig().getTiTimeOffset() == null) {
                            continue;
                        }
                        tiTimeOffset = RunManager.getRunManager().getTriggerConfig().getTiTimeOffset();
                        if (tiTimeOffset == 0) {
                            continue;
                        }
                    }

                    try {
                        DatabaseConditionsManager.getInstance().setDetector("HPS-EngRun2015-Nominal-v3", runNum);
                    } catch (Exception ex) {
                        continue;
                    }

                    try {
                        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
                    } catch (Exception ex) {
                        svtBiasConstants = null;
                    }
                    try {
                        svtPositionConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtMotorPosition.SvtMotorPositionCollection.class, "svt_motor_positions").getCachedData();
                    } catch (Exception ex) {
                        svtPositionConstants = null;
                    }

                    try {
                        alignmentConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtAlignmentConstant.SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();
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
                    } catch (Exception ex) {
                        alignmentConstants = null;
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

                Date startDate, endDate;
                long firstTime, lastTime;//Unix time from head bank
                long firstTI, lastTI;//TI timestamp from TI bank

                if (perRun) {
                    firstTime = Long.parseLong(record.get(7));
                    lastTime = Long.parseLong(record.get(8));
                    firstTI = Long.parseLong(record.get(10));
                    lastTI = Long.parseLong(record.get(11));

                } else {
                    firstTime = Long.parseLong(record.get(4));
                    lastTime = Long.parseLong(record.get(5));
                    firstTI = Long.parseLong(record.get(6));
                    lastTI = Long.parseLong(record.get(7));
                }

                if (useTI) {
                    if (firstTI == 0 || lastTI == 0) {
                        continue;
                    }
                    startDate = new Date((long) ((firstTI + tiTimeOffset) / 1e6));
                    endDate = new Date((long) ((lastTI + tiTimeOffset) / 1e6));
                } else {
                    if (firstTime == 0 || lastTime == 0) {
                        continue;
                    }
                    startDate = new Date(firstTime * 1000);
                    endDate = new Date(lastTime * 1000);
                }

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
                    SvtBiasConstant biasConstant = null;
                    if (svtBiasConstants != null) {
                        biasConstant = svtBiasConstants.find(date);
                        if (biasConstant == null && lastDate != null) {
                            biasConstant = svtBiasConstants.find(lastDate);
                        }
                        if (biasConstant != null) {
                            biasGood = true;
                        }
                    }
                    SvtMotorPosition positionConstant = null;
                    if (svtPositionConstants != null) {
                        positionConstant = svtPositionConstants.find(date);
                        if (positionConstant == null && lastDate != null) {
                            positionConstant = svtPositionConstants.find(lastDate);
                        }
                        if (positionConstant != null && alignmentConstants != null) {
//                    System.out.format("%f %f %f %f\n", positionConstant.getBottom(), nominalAngleBottom, positionConstant.getTop(), nominalAngleTop);
                            if (Math.abs(positionConstant.getBottom() - nominalAngleBottom) < angleTolerance && Math.abs(positionConstant.getTop() - nominalAngleTop) < angleTolerance) {
                                positionGood = true;
                            }
                        }
                    }

                    if (lastDate != null) {
                        double biasDt = 0;
                        double positionDt = 0;
                        long dtStart = Math.max(startDate.getTime(), lastDate.getTime());
                        long dtEnd = Math.min(date.getTime(), endDate.getTime());
                        double dt = (dtEnd - dtStart) / 1000.0;
                        if (biasConstant != null) {
                            long biasStart = Math.max(dtStart, biasConstant.getStart());
                            long biasEnd = Math.min(dtEnd, biasConstant.getEnd());
                            biasDt = (biasEnd - biasStart) / 1000.0;
                            if (positionConstant != null) {
                                long positionStart = Math.max(biasStart, positionConstant.getStart());
                                long positionEnd = Math.min(biasEnd, positionConstant.getEnd());
                                positionDt = (positionEnd - positionStart) / 1000.0;
                            }
                        }
//                        System.out.format("start %d end %d date %d lastDate %d current %f dt %f\n", startDate.getTime(), endDate.getTime(), date.getTime(), lastDate.getTime(), current, dt);
                        totalCharge += dt * current; // nC
                        totalGatedCharge += dt * current * livetime;
                        totalGoodCharge += dt * current * livetime * efficiency;
                        if (biasGood) {
                            totalChargeWithBias += biasDt * current;
                            totalGatedChargeWithBias += biasDt * current * livetime;
                            totalGoodChargeWithBias += biasDt * current * livetime * efficiency;
                            if (positionGood) {
                                totalChargeWithBiasAtNominal += positionDt * current;
                                totalGatedChargeWithBiasAtNominal += positionDt * current * livetime;
                                totalGoodChargeWithBiasAtNominal += positionDt * current * livetime * efficiency;
                            }
                        }
                    }
                    if (date.after(endDate)) {//this is the last interval overlapping the file's time range; backtrack so this line will be read again for the next file
                        date = lastDate;
                        try {
                            br.reset();
                        } catch (IOException e) {
                        }
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
        System.err.println("SvtChargeIntegrator <CSV log file> <MYA dump file>");
        formatter.printHelp("Need to adhere to these options", options);
    }
}
