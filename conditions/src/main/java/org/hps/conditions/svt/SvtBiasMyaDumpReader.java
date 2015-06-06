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



public class SvtBiasMyaDumpReader {

    
    public static void main(String[] args) {
        
        SvtBiasMyaDumpReader dumpReader = new SvtBiasMyaDumpReader();
        
        for( int i=0; i<args.length; ++i) {
            dumpReader.addEntries(readMyaDump(new File(args[i])));
        }
       System.out.println("Got " + dumpReader.getAllEntries().size() + " entries");
       
       dumpReader.buildRanges();
       
       dumpReader.printRanges();
      
        
    }
    
    
    
    



    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final double BIASVALUEON = 178.0;
    private List<SvtBiasMyaEntry> myaEntries = new ArrayList<SvtBiasMyaEntry>();
    private List<SvtBiasMyaRange> myaBiasOnRanges = new ArrayList<SvtBiasMyaRange>();
    
    public SvtBiasMyaDumpReader() {
        // TODO Auto-generated constructor stub
    }

    public void addEntry(SvtBiasMyaEntry e) {
        this.myaEntries.add(e);
    }

    public void addEntries(List<SvtBiasMyaEntry> e) {
        this.myaEntries.addAll(e);
    }

    public List<SvtBiasMyaEntry> getAllEntries() {
        return this.myaEntries;
    }

    private void printRanges() {
        for( SvtBiasMyaRange r : myaBiasOnRanges) {
            System.out.println(r.toString());
        }
     }
    
    
    private static List<SvtBiasMyaEntry> readMyaDump(File file) {

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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            br.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
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
                    throw new RuntimeException("date list is not ordered.");
                }
            }
            
            if( e.getValue() > BIASVALUEON) {
                if (range==null) {
                    System.out.println("BIAS ON: " + e.toString());
                    range = new SvtBiasMyaRange();
                    range.setStart(e);
                } 
            } else {
                //close it
                if (range!=null) {
                    System.out.println("BIAS TURNED OFF: " + e.toString());
                    range.setEnd(eprev);
                    this.myaBiasOnRanges.add(range);
                    range = null;
                }
            }            
            eprev = e;
        }
        System.out.println("Built " + this.myaBiasOnRanges.size() + " ranges");
        
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
            return name + " " + date.toString() + " value " + value;
        }
    }

   
    
    public static final class SvtBiasMyaRange {
        private SvtBiasMyaEntry start;
        private SvtBiasMyaEntry end;
        public SvtBiasMyaRange() {}
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
    }

}
