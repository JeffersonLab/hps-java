package org.hps.conditions.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class SvtBiasMyaDataReader {

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(new Option("q", false, "quiet - don't print event contents"));
        options.addOption(new Option("c", false, "print control events"));
        options.addOption(new Option("s", false, "sequential read (not mem-mapped)"));

        // Parse the command line options.
        if (args.length == 0) {
            System.out.println("SvtBiasMyaDataReader <myaData dump> <run time table - tab separated>");
            final HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        final CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (final org.apache.commons.cli.ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        if (cl.getArgs().length != 2) {
            throw new RuntimeException("Missing myData dump or run time file.");
        }

        List<SvtBiasMyaRange> ranges = SvtBiasMyaDataReader.readMyaData(new File(cl.getArgs()[0]), 178.0, 2000);

//        for (SvtBiasMyaRange range : ranges) {
//            System.out.println(range);
//        }
        List<RunData> runData = SvtBiasMyaDataReader.readRunTable(new File(cl.getArgs()[1]));

        List<SvtBiasRunRange> runRanges = new ArrayList<SvtBiasRunRange>();

        Iterator<SvtBiasMyaRange> rangesIter = ranges.iterator();
        SvtBiasMyaRange nextRange = rangesIter.next();

        runLoop:
        for (RunData data : runData) {

            while (nextRange.getEnd().before(data.getStart())) {
                nextRange = rangesIter.next();
                if (!rangesIter.hasNext()) {
                    break runLoop;
                }
            }
            while (nextRange.getStart().before(data.getEnd())) {
                runRanges.add(new SvtBiasRunRange(data, nextRange));
                nextRange = rangesIter.next();
                if (!rangesIter.hasNext()) {
                    break runLoop;
                }
            }
//            System.out.println(data);
        }

        for (SvtBiasRunRange runRange : runRanges) {
            System.out.println(runRange);
        }

//        
//        boolean quiet = cl.hasOption("q");
//        boolean printControlEvents = cl.hasOption("c");
//        boolean seqRead = cl.hasOption("s");
//
//        SvtBiasMyaDataReader dumpReader = new SvtBiasMyaDataReader(args);
//
//        dumpReader.printRanges();
    }

//    private static final SimpleDateFormat DATE_FORMAT = new RunSpreadsheet.AnotherSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //private static final TimeZone timeZone = TimeZone.getTimeZone("EST");
    public SvtBiasMyaDataReader(double biasValueOn, int endMargin) {
    }

    public static List<SvtBiasMyaRange> readMyaData(File file, double biasValueOn, int endMargin) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        List<SvtBiasMyaRange> ranges = new ArrayList<SvtBiasMyaRange>();
        try {

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            System.out.println(br.readLine());

            SvtBiasMyaRange currentRange = null;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                String arr[] = line.split(" +");
                try {

                    if (arr.length < 3) {
                        throw new ParseException("this line is not correct.", 0);
                    }

                    Date date = dateFormat.parse(arr[0] + " " + arr[1]);
                    Double[] values = new Double[arr.length - 2];
                    for (int i = 2; i < arr.length; i++) {
                        if (arr[i].equals("<undefined>")) {
                            values[i - 2] = 0.0;
                        } else {
                            values[i - 2] = Double.parseDouble(arr[i]);
                        }
                    }
                    double biasValue = Collections.min(Arrays.asList(values));
                    if (biasValue > biasValueOn) {
                        if (currentRange == null) {
                            currentRange = new SvtBiasMyaRange(date, biasValue);
//                            System.out.format("bias on:\t%d %d %f %s\n", date.getTime(), values.length, biasValue, date.toString());
                        }
                    } else {
                        if (currentRange != null) {
                            currentRange.setEnd(new Date(date.getTime() - endMargin));
                            ranges.add(currentRange);
                            currentRange = null;
//                            System.out.format("bias off:\t%d %d %f %s\n", date.getTime(), values.length, biasValue, date.toString());
                        }
                    }
//                    System.out.format("%d %d %f\n", date.getTime(), values.length, biasValue);
//                    SvtBiasMyaEntry entry = new SvtBiasMyaEntry(file.getName(), date, value);
//                    myaEntries.add(entry);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return ranges;
    }

    public static List<RunData> readRunTable(File file) {
        List<CSVRecord> records = null;
        List<RunData> data = new ArrayList<RunData>();
        try {
            FileReader reader = new FileReader(file);
            final CSVFormat format = CSVFormat.DEFAULT;

            final CSVParser parser;
            parser = new CSVParser(reader, format);

            records = parser.getRecords();

            // Remove first two rows of headers.
            records.remove(0);
            records.remove(0);

            parser.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SvtBiasMyaDataReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SvtBiasMyaDataReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (CSVRecord record : records) {
            int runNum = Integer.parseInt(record.get(0));
            long startTime = Long.parseLong(record.get(7)) * 1000;
            long endTime = Long.parseLong(record.get(8)) * 1000;
            if (startTime != 0 && endTime != 0) {
                data.add(new RunData(new Date(startTime), new Date(endTime), runNum));
            }
        }

        return data;
    }

    public static class SvtBiasMyaRange {

        private Date start;
        private Date end;
        private double bias;

        public SvtBiasMyaRange() {
        }

//        public boolean overlap(Date date_start, Date date_end) {
//            if (date_end.before(getStartDate())) {
//                return false;
//            } else if (date_start.after(getEndDate())) {
//                return false;
//            }
//            return true;
//        }
//
        public SvtBiasMyaRange(Date start, double bias) {
            this.start = start;
            this.bias = bias;
        }

        public Date getEnd() {
            return end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }

        public Date getStart() {
            return start;
        }

        @Override
        public String toString() {
            return String.format("START: %s (%d), END: %s (%d), bias: %f, duration: %d", start.toString(), start.getTime(), end.toString(), end.getTime(), bias, end.getTime() - start.getTime());
        }

        public boolean includes(Date date) {
            return !date.before(getStart()) && !date.after(getEnd());
        }
    }

    public static class RunData {

        private final Date start;
        private final Date end;
        private final int run;

        public RunData(Date start, Date end, int run) {
            this.start = start;
            this.end = end;
            this.run = run;
        }

        public Date getStart() {
            return start;
        }

        public Date getEnd() {
            return end;
        }

        public int getRun() {
            return run;
        }

        @Override
        public String toString() {
            return String.format("Run %d - START: %s (%d), END: %s (%d), duration: %d", run, start.toString(), start.getTime(), end.toString(), end.getTime(), end.getTime() - start.getTime());
        }
    }

    public static final class SvtBiasRunRange {

        private RunData run;
        private SvtBiasMyaRange range;

        public SvtBiasRunRange(RunData run, SvtBiasMyaRange range) {
            setRun(run);
            setRange(range);
        }

        public RunData getRun() {
            return run;
        }

        public void setRun(RunData run) {
            this.run = run;
        }

        public SvtBiasMyaRange getRange() {
            return range;
        }

        public void setRange(SvtBiasMyaRange range) {
            this.range = range;
        }

        @Override
        public String toString() {
//            StringBuffer sb = new StringBuffer();
//            sb.append("\nRun " + run.toString() + ":");
//            sb.append("\n" + range.toString());
            return String.format("%s, range %s", run.toString(), range.toString());
        }
    }

}
