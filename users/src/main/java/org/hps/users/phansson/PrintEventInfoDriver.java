/**
 * 
 */
package org.hps.users.phansson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * 
 * Driver class that prints event info for specific events
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class PrintEventInfoDriver extends Driver {

    private static final Logger logger = Logger.getLogger(PrintEventInfoDriver.class.getSimpleName());
    
    // Filename for the list of events to print
    private String eventListFileName = "eventlist.dat";
    // Filename for the output filename
    private String eventInfoFileName = "eventinfo.dat";
    // Map to store the run-events to print to file
    Map<Integer,List<Integer>> eventList;
    PrintWriter pWriter = null;
    


    /**
     * Default constructor.
     */
    public PrintEventInfoDriver() {
        
        logger.setLevel(Level.INFO);
 
        try {
            pWriter = new PrintWriter(eventInfoFileName,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void detectorChanged(Detector detector) {

        // Read run,event number key-value pair from file
        File eventListFile = new File(eventListFileName );
        eventList = new HashMap<Integer,List<Integer>>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(eventListFile));
            String line;
            while( (line = bufferedReader.readLine()) != null) {
                logger.finest("processing line \"" + line + "\"");
                String arr[] = line.split(" ");
                if(arr.length != 2) {
                    bufferedReader.close();
                    throw new RuntimeException("this line is not formatted correctly: " + line);
                }
                int run = Integer.parseInt( arr[0] );
                int event = Integer.parseInt(arr[1]);
                List<Integer> events = null;
                if(!eventList.containsKey(run)) {
                    events = new ArrayList<Integer>();
                    eventList.put(run, events);
                } else 
                    events = eventList.get(run);
                events.add(event);
            }
            bufferedReader.close();
            
        }                    
        catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        
        if(eventList.size() == 0)
            throw new RuntimeException("There are no runs to check!?");
        
        logger.info("Read " + eventList.size() + "runs into list to check");
        for(Entry<Integer, List<Integer>> e : eventList.entrySet())
            logger.info(e.getKey() + " " + e.getValue());
        
        
        

    }
    
    @Override
    protected void process(EventHeader event) {
    
        // find this run in the list
        int run = event.getRunNumber();
        
        if( !eventList.containsKey(run)){
            logger.fine("Skip run " + run);
            return;
        }
        
        int eventNumber = event.getEventNumber();

        if( !eventList.get(run).contains(eventNumber) ) {
            logger.fine("Skip event " + run);
            return;
        }
        
        // Found the event in the list 
        logger.fine("Found run " + run + " and event " + eventNumber);
        
        // create the string to print
        StringBuffer sb = new StringBuffer();
        sb.append("run ");
        sb.append(run);
        sb.append(" event ");
        sb.append(eventNumber);
        sb.append(" eventtime ");
        sb.append(event.getTimeStamp());
        sb.append(" eventdate ");
        sb.append(new Date( (long)Math.floor(event.getTimeStamp()/1.0e6)   ));
        
        logger.fine(sb.toString());
        
        // write to file
        pWriter.println(sb.toString());
        
        
    }
    
    @Override
    protected void endOfData() {
        if(pWriter != null)
            pWriter.close();
    }
    

}
