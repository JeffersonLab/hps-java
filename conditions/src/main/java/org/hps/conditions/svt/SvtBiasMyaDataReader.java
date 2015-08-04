package org.hps.conditions.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hps.conditions.run.RunSpreadsheet.RunData;

public class SvtBiasMyaDataReader {

    public static void main(String[] args) {

        // Parse the command line options.
        if (args.length == 0) {
            System.out.println("SvtBiasMyaDataReader <myaData dump> <run time table - tab separated>");
            System.exit(1);
        }

        if (args.length != 2) {
            throw new RuntimeException("Missing myData dump or run time file.");
        }

        List<SvtBiasMyaRange> ranges = SvtBiasMyaDataReader.readMyaData(new File(args[0]), 178.0, 2000, true);

        List<RunData> runData = SvtBiasMyaDataReader.readRunTable(new File(args[1]));

        List<SvtBiasRunRange> runRanges = findOverlappingRanges(runData, ranges);

        for (SvtBiasRunRange runRange : runRanges) {
            System.out.println(runRange);
        }

    }

    public static List<SvtBiasRunRange> findOverlappingRanges(List<RunData> runList, List<SvtBiasMyaRange> ranges) {
        List<SvtBiasRunRange> runRanges = new ArrayList<SvtBiasRunRange>();

        Iterator<SvtBiasMyaRange> rangesIter = ranges.iterator();
        SvtBiasMyaRange nextRange = rangesIter.next();

        for (RunData run : runList) {
            SvtBiasRunRange runRange = new SvtBiasRunRange(run);
            while (nextRange.getEndDate().before(run.getStartDate()) && rangesIter.hasNext()) {
                nextRange = rangesIter.next();
            }
            while (nextRange.getStartDate().before(run.getEndDate())) {
                runRange.addRange(nextRange);
                if (nextRange.getEndDate().after(run.getEndDate())) {
                    break;
                }
                if (!rangesIter.hasNext()) {
                    break;
                }
                nextRange = rangesIter.next();
            }
            if (!runRange.getRanges().isEmpty()) {
                runRanges.add(runRange);
            }
        }
        return runRanges;
    }

    public static List<SvtBiasMyaRange> readMyaData(File file, double biasValueOn, int endMargin, boolean discardHeader) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        List<SvtBiasMyaRange> ranges = new ArrayList<SvtBiasMyaRange>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            if (discardHeader) {
                System.out.println("myaData header: " + br.readLine()); //discard the first line
            }
            SvtBiasMyaRange currentRange = null;
            while ((line = br.readLine()) != null) {
                String arr[] = line.split(" +");

                if (arr.length < 3) {
                    throw new ParseException("this line is not correct.", 0);
                }

                Date date = dateFormat.parse(arr[0] + " " + arr[1]);
                Double[] values = new Double[arr.length - 2];
                for (int i = 2; i < arr.length; i++) {
                    if (arr[i].equals("<undefined>")) {
                        values[i - 2] = 0.0; //assume it's bad
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
                        currentRange.setEndDate(new Date(date.getTime() - endMargin));
                        ranges.add(currentRange);
                        currentRange = null;
//                            System.out.format("bias off:\t%d %d %f %s\n", date.getTime(), values.length, biasValue, date.toString());
                    }
                }
            }
            br.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
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

        public SvtBiasMyaRange(Date start, double bias) {
            this.start = start;
            this.bias = bias;
        }

        public Date getEndDate() {
            return end;
        }

        public void setEndDate(Date end) {
            this.end = end;
        }

        public Date getStartDate() {
            return start;
        }

        @Override
        public String toString() {
            return String.format("START: %s (%d), END: %s (%d), bias: %f, duration: %d", start.toString(), start.getTime(), end.toString(), end.getTime(), bias, end.getTime() - start.getTime());
        }

        public boolean includes(Date date) {
            return !date.before(getStartDate()) && !date.after(getEndDate());
        }

        Object getValue() {
            return bias;
        }
    }

    public static final class SvtBiasRunRange {

        private RunData run;
        private final List<SvtBiasMyaRange> ranges = new ArrayList<SvtBiasMyaRange>();

        public SvtBiasRunRange(RunData run) {
            setRun(run);
        }

        public RunData getRun() {
            return run;
        }

        public void setRun(RunData run) {
            this.run = run;
        }

        public List<SvtBiasMyaRange> getRanges() {
            return ranges;
        }

        public void addRange(SvtBiasMyaRange range) {
            ranges.add(range);
        }

        public boolean includes(Date date) {
            for (SvtBiasMyaRange r : ranges) {
                if (date.after(r.getStartDate()) && date.before(r.getEndDate())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nRun ").append(run.toString()).append(":");
            for (SvtBiasMyaRange range : ranges) {
                sb.append("\n").append(range.toString());
            }
            return sb.toString();
        }
    }

}
