package org.hps.conditions.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.util.BasicLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian, SLAC
 */
public class SvtBiasMyaDumpReader {

    public static final class SvtBiasMyaEntry {
        private final Date date;
        private final String name;
        private final double value;

        public SvtBiasMyaEntry(final String name, final Date date, final double value) {
            this.date = date;
            this.name = name;
            this.value = value;
        }

        public Date getDate() {
            return this.date;
        }

        public double getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.name + " " + this.date.toString() + " value " + this.value;
        }
    }

    public static class SvtBiasMyaRange {
        private SvtBiasMyaEntry end;
        private SvtBiasMyaEntry start;

        public SvtBiasMyaRange() {
        }

        public SvtBiasMyaRange(final SvtBiasMyaEntry start) {
            this.start = start;
        }

        public SvtBiasMyaEntry getEnd() {
            return this.end;
        }

        public Date getEndDate() {
            return this.getEnd().getDate();
        }

        public SvtBiasMyaEntry getStart() {
            return this.start;
        }

        public Date getStartDate() {
            return this.getStart().getDate();
        }

        public boolean overlap(final Date date_start, final Date date_end) {
            if (date_end.before(this.getStartDate())) {
                return false;
            } else if (date_start.after(this.getEndDate())) {
                return false;
            }
            return true;
        }

        public void setEnd(final SvtBiasMyaEntry end) {
            this.end = end;
        }

        public void setStart(final SvtBiasMyaEntry start) {
            this.start = start;
        }

        @Override
        public String toString() {
            return "START: " + this.start.toString() + "   END: " + this.end.toString();
        }
    }

    public static final class SvtBiasMyaRanges extends ArrayList<SvtBiasMyaRange> {
        public SvtBiasMyaRanges() {
        }

        public SvtBiasMyaRanges findOverlappingRanges(final Date date_start, final Date date_end) {
            logger.fine("look for overlaps from " + date_start.toString() + " to " + date_end.toString());
            final SvtBiasMyaRanges overlaps = new SvtBiasMyaRanges();
            for (final SvtBiasMyaRange range : this) {
                logger.fine("loop bias range " + range.toString());
                if (range.overlap(date_start, date_end)) {
                    overlaps.add(range);
                    logger.fine("overlap found!! ");
                }
            }
            return overlaps;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            for (final SvtBiasMyaRange range : this) {
                sb.append(range.toString() + "\n");
            }
            return sb.toString();
        }
    }

    public static final class SvtBiasRunRange {
        private SvtBiasMyaRanges ranges;
        private RunData run;

        public SvtBiasRunRange(final RunData run, final SvtBiasMyaRanges ranges) {
            this.setRun(run);
            this.setRanges(ranges);
        }

        public SvtBiasMyaRanges getRanges() {
            return this.ranges;
        }

        public RunData getRun() {
            return this.run;
        }

        public void setRanges(final SvtBiasMyaRanges ranges) {
            this.ranges = ranges;
        }

        public void setRun(final RunData run) {
            this.run = run;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("\nRun " + this.run.toString() + ":");
            for (final SvtBiasMyaRange r : this.ranges) {
                sb.append("\n" + r.toString());
            }
            return sb.toString();
        }
    }

    private static final double BIASVALUEON = 178.0;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Logger logger = LogUtil.create(SvtBiasMyaDumpReader.class, new BasicLogFormatter(), Level.INFO);

    public static void main(final String[] args) {

        final SvtBiasMyaDumpReader dumpReader = new SvtBiasMyaDumpReader(args);

        dumpReader.printRanges();

    }

    protected static List<SvtBiasMyaEntry> readMyaDump(final File file) {

        final List<SvtBiasMyaEntry> myaEntries = new ArrayList<SvtBiasMyaEntry>();
        try {

            final BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                // System.out.println(line);
                final String arr[] = line.split(" ");
                try {

                    if (arr.length < 3) {
                        throw new ParseException("this line is not correct.", 0);
                    }
                    final Date date = DATE_FORMAT.parse(arr[0] + " " + arr[1]);
                    final double value = Double.parseDouble(arr[2]);
                    final SvtBiasMyaEntry entry = new SvtBiasMyaEntry(file.getName(), date, value);
                    myaEntries.add(entry);
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }
            br.close();

        } catch (final IOException e) {
            e.printStackTrace();
        }
        return myaEntries;

    }

    private final SvtBiasMyaRanges biasRanges = new SvtBiasMyaRanges();

    private final List<SvtBiasMyaEntry> myaEntries = new ArrayList<SvtBiasMyaEntry>();

    public SvtBiasMyaDumpReader() {
    }

    public SvtBiasMyaDumpReader(final String filepath) {
        final String[] files = {filepath};
        this.buildFromFiles(files);
    }

    public SvtBiasMyaDumpReader(final String[] args) {
        this.buildFromFiles(args);
    }

    public void addEntries(final List<SvtBiasMyaEntry> e) {
        this.myaEntries.addAll(e);
    }

    public void addEntry(final SvtBiasMyaEntry e) {
        this.myaEntries.add(e);
    }

    public void buildFromFiles(final String[] args) {
        for (final String arg : args) {
            this.readFromFile(new File(arg));
        }
        this.buildRanges();
    }

    public void buildRanges() {
        SvtBiasMyaRange range = null;
        SvtBiasMyaEntry eprev = null;
        for (final SvtBiasMyaEntry e : this.myaEntries) {

            // System.out.println(e.toString());

            if (eprev != null) {
                if (e.getDate().before(eprev.getDate())) {
                    throw new RuntimeException("date list is not ordered: " + eprev.toString() + " vs " + e.toString());
                }
            }

            if (e.getValue() > BIASVALUEON) {
                if (range == null) {
                    logger.fine("BIAS ON: " + e.toString());
                    range = new SvtBiasMyaRange();
                    range.setStart(e);
                }
            } else {
                // close it
                if (range != null) {
                    logger.fine("BIAS TURNED OFF: " + e.toString());
                    range.setEnd(e);
                    this.biasRanges.add(range);
                    range = null;
                }
            }
            eprev = e;
        }
        logger.info("Built " + this.biasRanges.size() + " ranges");

    }

    public SvtBiasMyaRanges findOverlappingRanges(final Date date_start, final Date date_end) {
        return this.biasRanges.findOverlappingRanges(date_start, date_end);
    }

    public List<SvtBiasMyaEntry> getEntries() {
        return this.myaEntries;
    }

    public SvtBiasMyaRanges getRanges() {
        return this.biasRanges;
    }

    private void printRanges() {
        for (final SvtBiasMyaRange r : this.biasRanges) {
            logger.info(r.toString());
        }
    }

    private void readFromFile(final File file) {
        this.addEntries(readMyaDump(file));
        logger.info("Got " + this.getEntries().size() + " entries from " + file.getName());

    }

}
