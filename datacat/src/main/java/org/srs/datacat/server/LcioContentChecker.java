package org.srs.datacat.server;

import hep.lcio.event.LCEvent;
import hep.lcio.implementation.io.LCFactory;
import hep.lcio.io.LCReader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LcioContentChecker implements ContentChecker {
    
    //private long minRunNumber;
    //private long maxRunNumber;
    private long runNumber;
    private long nevents;
    private String status;
    private HashSet<String> uniqueCollections;
    //private HashSet<String> uniqueDetectors;
    String detectorName;
    Map<String, Object> metaData;
        
    @Override   
    public void setLocation(long datasetVersion, URL url) throws IOException {
                
        // Initialize variables for this file.
        //minRunNumber = Integer.MAX_VALUE;
        //maxRunNumber = Integer.MIN_VALUE;
        runNumber = -1;
        nevents = 0;
        status = "OK";
        uniqueCollections = new HashSet<String>();
        //uniqueDetectors = new HashSet<String>();
        detectorName = null;
        metaData = new HashMap<String, Object>();
        
        // Open file from URL.
        if (!url.getProtocol().equals("file")) {
            throw new IOException("Only file protocol is supported.");
        }
        LCReader reader = LCFactory.getInstance().createLCReader();        
        reader.open(url.getPath());
        
        // Get the first event.
        LCEvent event = reader.readNextEvent();
        
        if (event == null)
            throw new IllegalArgumentException("The event file has zero events.");
        
        //while (event != null) {
        
        // Run number setting.
        //int thisRunNumber 
        runNumber = event.getRunNumber();    
        metaData.put("nRun", runNumber);
        /*
            if (thisRunNumber < minRunNumber)
                minRunNumber = thisRunNumber;
            if (thisRunNumber > maxRunNumber)
                maxRunNumber = thisRunNumber;
                */
            
        // Collection names.
        uniqueCollections.addAll(Arrays.asList(event.getCollectionNames()));
            
        // Detectors.
        //uniqueDetectors.add(event.getDetectorName());
        detectorName = event.getDetectorName();
            
        // Number of events.
        //nevents++;
            
        // Get next event.
        //event = reader.readNextEvent();
        //}
        
        // Close the reader.
        reader.close();
        
        // Sort collection names and insert into meta data.
        List<String> sortedCollections = new ArrayList<String>(uniqueCollections);
        Collections.sort(sortedCollections);        
        StringBuffer buff = new StringBuffer();
        for (String collectionName : sortedCollections) {
            buff.append(collectionName);
            buff.append(',');
        }
        buff.setLength(buff.length() - 1);        
        metaData.put("sCollections", buff.toString());
        
        // Sort detector names and insert into meta data.
        /*
        List<String> sortedDetectors = new ArrayList<String>(uniqueDetectors);
        Collections.sort(sortedDetectors);
        buff = new StringBuffer();
        for (String detectorName : sortedDetectors) {
            buff.append(detectorName);
            buff.append(',');
        }
        buff.setLength(buff.length() - 1);
        */
        metaData.put("sDetectorName", detectorName);
        
        
    }
    
    @Override
    public long getEventCount() throws IOException {
        //return nevents;
        return 0;
    }
    
    @Override
    public long getRunMin() throws IOException {
        //return minRunNumber;
        return runNumber;
    }
    
    @Override
    public long getRunMax() throws IOException {
        //return maxRunNumber;
        return runNumber;
    }
    
    @Override
    public String getStatus() throws IOException {
        return status;
    }
    
    @Override
    public Map<String,Object> getMetaData() throws IOException {
        return metaData;
    }
    
    @Override
    public void close() throws IOException {
    }           
}