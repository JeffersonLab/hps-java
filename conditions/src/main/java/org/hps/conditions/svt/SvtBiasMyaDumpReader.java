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
import java.util.TimeZone;
import java.util.logging.Logger;

import org.hps.conditions.run.RunSpreadsheet.RunData;

public class SvtBiasMyaDumpReader {
    
    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(SvtBiasMyaDumpReader.class.getPackage().getName());
    
    public static void main(String[] args) {
        
        SvtBiasMyaDumpReader dumpReader = new SvtBiasMyaDumpReader(args);
        
        dumpReader.printRanges();
      
        
    }
    
//    private static final SimpleDateFormat DATE_FORMAT = new RunSpreadsheet.AnotherSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //private static final TimeZone timeZone = TimeZone.getTimeZone("EST");
    public static final double BIASVALUEON = 178.0;
    private List<SvtBiasMyaEntry> myaEntries = new ArrayList<SvtBiasMyaEntry>();
    private SvtBiasMyaRanges biasRanges = new SvtBiasMyaRanges();
    
    public SvtBiasMyaDumpReader() {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    public SvtBiasMyaRanges findOverlappingRanges(Date date_start, Date date_end) {
        return this.biasRanges.findOverlappingRanges(date_start, date_end);
    }
    
    private void readFromFile(File file) {
        addEntries(readMyaDump(file));
        LOGGER.info("Got " + getEntries().size() + " entries from " + file.getName());
       
    }
    public void buildFromFiles(String[] args) {
        for( int i=0; i<args.length; ++i) {
            readFromFile(new File(args[i]));
        }
        buildRanges();       
    }
    
    public SvtBiasMyaDumpReader(String[] args) {
        buildFromFiles(args);
    }
    
    public SvtBiasMyaDumpReader(String filepath) {
        String[] files = {filepath};
        buildFromFiles(files);
    }

    
    public void addEntry(SvtBiasMyaEntry e) {
        this.myaEntries.add(e);
    }

    public void addEntries(List<SvtBiasMyaEntry> e) {
        this.myaEntries.addAll(e);
    }

    public List<SvtBiasMyaEntry> getEntries() {
        return this.myaEntries;
    }

    public SvtBiasMyaRanges getRanges() {
        return this.biasRanges;
    }

    
    private void printRanges() {
        for( SvtBiasMyaRange r : biasRanges) {
            LOGGER.info(r.toString());
        }
     }
    
    
    protected static List<SvtBiasMyaEntry> readMyaDump(File file) {

        List<SvtBiasMyaEntry> myaEntries = new ArrayList<SvtBiasMyaEntry>();
        try {

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                String arr[] = line.split(" ");
                try {
                    
                    if(arr.length<3) {
                        throw new ParseException("this line is not correct.",0);
                    }
                    
                    Date date = DATE_FORMAT.parse(arr[0] + " " + arr[1]);
                    double value = Double.parseDouble(arr[2]);
                    SvtBiasMyaEntry entry = new SvtBiasMyaEntry(file.getName(), date, value);
                    myaEntries.add(entry);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return myaEntries;

    }
    
    public void buildRanges() {
        SvtBiasMyaRange range = null;
        SvtBiasMyaEntry eprev = null;
        for(SvtBiasMyaEntry e : this.myaEntries) {
            
            //System.out.println(e.toString());
            
            if(eprev!=null) {
                if(e.getDate().before(eprev.getDate())) {
                    throw new RuntimeException("date list is not ordered: " + eprev.toString() + " vs " + e.toString());
                }
            }
            
            if( e.getValue() > BIASVALUEON) {
                if (range==null) {
                    LOGGER.fine("BIAS ON: " + e.toString());
                    range = new SvtBiasMyaRange();
                    range.setStart(e);
                } 
            } else {
                //close it
                if (range!=null) {
                    LOGGER.fine("BIAS TURNED OFF: " + e.toString());
                    range.setEnd(e);
                    this.biasRanges.add(range);
                    range = null;
                }
            }            
            eprev = e;
        }
        LOGGER.info("Built " + this.biasRanges.size() + " ranges");
        
    }
    
    
    public static final class SvtBiasMyaEntry {
        private Date date;
        private String name;
        private double value;
        public SvtBiasMyaEntry(String name, Date date, double value) {
            this.date = date;
            this.name = name;
            this.value = value;
        }
        public double getValue() {
            return value;
        }
        public Date getDate() {
            return this.date;
        }
        public String toString() {
            return name + " " + date.toString() + " (epoch " + Long.toString(date.getTime()) + ")" + " value " + value;
        }
    }


    
    public static final class SvtBiasMyaRanges extends ArrayList<SvtBiasMyaRange> {
        public SvtBiasMyaRanges() {}
        public SvtBiasMyaRanges findOverlappingRanges(Date date_start, Date date_end) {
            LOGGER.fine("look for overlaps from " + date_start.toString() + " to " + date_end.toString());
            SvtBiasMyaRanges overlaps = new SvtBiasMyaRanges();
            for(SvtBiasMyaRange range : this) {
                LOGGER.fine("loop bias range " + range.toString());
                if( range.overlap(date_start,date_end) ) {
                    overlaps.add(range);
                    LOGGER.fine("overlap found!! ");
                }
            }
            return overlaps;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for(SvtBiasMyaRange range : this) {
                sb.append(range.toString() + "\n");
            }
            return sb.toString();
        }
        
        public boolean includes(Date date) {
            for(SvtBiasMyaRange r : this) {
                if(r.includes(date)) return true;
            }
            return false;
        }
        
    }
    
    public static class SvtBiasMyaRange {
        private SvtBiasMyaEntry start;
        private SvtBiasMyaEntry end;
        public SvtBiasMyaRange() {}
        public Date getStartDate() {
            return getStart().getDate();
        }
        public Date getEndDate() {
            return getEnd().getDate();
        }
        public boolean overlap(Date date_start, Date date_end) {
            if( date_end.before(getStartDate()) ) {
                return false;
            } else if ( date_start.after(getEndDate())) {
                return false;
            } 
            return true;
        }
        public SvtBiasMyaRange(SvtBiasMyaEntry start) {
            this.start = start;
        }
        public SvtBiasMyaEntry getEnd() {
            return end;
        }
        public void setEnd(SvtBiasMyaEntry end) {
            this.end = end;
        }
        public SvtBiasMyaEntry getStart() {
            return start;
        }
        public void setStart(SvtBiasMyaEntry start) {
            this.start = start;
        }
        public String toString() {
            return "START: " + start.toString() + "   END: " + end.toString();
        }
        public boolean includes(Date date) {
            if( date.before(getStartDate()) || date.after(getEndDate()) ) {
                return false;
            } else {
                return true;
            }
        }
    }
    
    public static final class SvtBiasRunRange {
        private RunData run;
        private SvtBiasMyaRanges ranges;
        public SvtBiasRunRange(RunData run, SvtBiasMyaRanges ranges) {
            setRun(run);
            setRanges(ranges);
        }
        public RunData getRun() {
            return run;
        }
        public void setRun(RunData run) {
            this.run = run;
        }
        public SvtBiasMyaRanges getRanges() {
            return ranges;
        }
        public void setRanges(SvtBiasMyaRanges ranges) {
            this.ranges = ranges;
        }
        public String toString() {
            StringBuffer sb  = new StringBuffer();
            sb.append("\nRun " + run.toString() + ":");
            for (SvtBiasMyaRange r : ranges) {
                sb.append("\n" + r.toString());
            }
            return sb.toString();
        }
    }
    

}
