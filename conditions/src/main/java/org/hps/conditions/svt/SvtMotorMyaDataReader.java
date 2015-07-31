package org.hps.conditions.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import org.hps.conditions.run.RunSpreadsheet.RunData;

public class SvtMotorMyaDataReader {

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new RuntimeException("Missing myData dump or run time file.");
        }

        List<SvtPositionMyaRange> ranges = SvtMotorMyaDataReader.readMyaData(new File(args[0]), 1000, 10000);
        List<RunData> runData = SvtBiasMyaDataReader.readRunTable(new File(args[1]));
        List<SvtPositionRunRange> runRanges = findOverlappingRanges(runData, ranges);
        for (SvtPositionRunRange runRange : runRanges) {
            System.out.println(runRange);
        }
    }

    public static List<SvtPositionRunRange> findOverlappingRanges(List<RunData> runList, List<SvtPositionMyaRange> ranges) {
        List<SvtPositionRunRange> runRanges = new ArrayList<SvtPositionRunRange>();

        Iterator<SvtPositionMyaRange> rangesIter = ranges.iterator();
        SvtPositionMyaRange nextRange = rangesIter.next();

        for (RunData run : runList) {
            SvtPositionRunRange runRange = new SvtPositionRunRange(run);
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

    public static List<SvtPositionMyaRange> readMyaData(File file, int endMargin, double minDwellTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        List<SvtPositionMyaRange> ranges = new ArrayList<SvtPositionMyaRange>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            System.out.println("myaData header: " + br.readLine()); //discard the first line
            SvtPositionMyaRange currentRange = null;
            Date lastDate = null;
            while ((line = br.readLine()) != null) {
                String arr[] = line.split(" +");

                if (arr.length != 4) {
                    throw new ParseException("this line is not correct.", 0);
                }
                Date date = dateFormat.parse(arr[0] + " " + arr[1]);
                Double[] values = new Double[2];
                for (int i = 0; i < 2; i++) {
                    if (arr[i + 2].equals("<undefined>")) {
                        values[i] = 0.0; //if no data, assume retracted
                    } else {
                        values[i] = Double.parseDouble(arr[i + 2]);
                    }
                }
                if (lastDate != null && currentRange != null && date.getTime() - lastDate.getTime() > minDwellTime) {
                    currentRange.setEndDate(new Date(date.getTime() - endMargin));
//                    System.out.format("motors stopped:\t %s\n", currentRange.toString());
                    ranges.add(currentRange);
                }
                currentRange = new SvtPositionMyaRange(date, motorToAngleTop(values[0]), motorToAngleBottom(values[1]));
                lastDate = date;
            }
            br.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return ranges;
    }

    private static double motorToAngleTop(double motor) {
        return (17.821 - motor) / 832.714;
    }

    private static double motorToAngleBottom(double motor) {
        return (17.397 - motor) / 832.714;
    }

    public static class SvtPositionMyaRange {

        private final Date start;
        private Date end;
        private final double top, bottom;

        public SvtPositionMyaRange(Date start, double top, double bottom) {
            this.start = start;
            this.top = top;
            this.bottom = bottom;
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
            return String.format("START: %s (%d), END: %s (%d), top: %f, bottom: %f, duration: %d", start.toString(), start.getTime(), end.toString(), end.getTime(), top, bottom, end.getTime() - start.getTime());
        }

        public boolean includes(Date date) {
            return !date.before(getStartDate()) && !date.after(getEndDate());
        }

        double getTop() {
            return top;
        }

        double getBottom() {
            return bottom;
        }
    }

    public static final class SvtPositionRunRange {

        private RunData run;
        private final List<SvtPositionMyaRange> ranges = new ArrayList<SvtPositionMyaRange>();

        public SvtPositionRunRange(RunData run) {
            setRun(run);
        }

        public RunData getRun() {
            return run;
        }

        public void setRun(RunData run) {
            this.run = run;
        }

        public List<SvtPositionMyaRange> getRanges() {
            return ranges;
        }

        public void addRange(SvtPositionMyaRange range) {
            ranges.add(range);
        }

        public boolean includes(Date date) {
            for (SvtPositionMyaRange r : ranges) {
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
            for (SvtPositionMyaRange range : ranges) {
                sb.append("\n").append(range.toString());
            }
            return sb.toString();
        }
    }

}
