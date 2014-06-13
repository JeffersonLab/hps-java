package org.srs.datacat.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioContentChecker implements ContentChecker {

    static final int PRESTART_EVENT_TAG = 17;
    static final int GO_EVENT_TAG = 18;
    static final int END_EVENT_TAG = 20;
    static final int PHYSICS_EVENT_TAG = 1;
    
    long minRunNumber;
    long maxRunNumber;
    long nevents;
    String status;
    Map<String, Object> metadata;

    @Override
    public void setLocation(long arg0, URL url) throws IOException {

        minRunNumber = Integer.MAX_VALUE;
        maxRunNumber = Integer.MIN_VALUE;
        status = "OK";
        metadata = new HashMap<String, Object>();

        if (!url.getProtocol().equals("file")) {
            throw new IOException("Only file protocol is supported.");
        }
        EvioReader reader = null;
        try {
            reader = new EvioReader(new File(url.getPath()));
            EvioEvent event = reader.nextEvent();
            while (event != null) {
                int[] data = event.getIntData();
                if (isPreStartEvent(event)) {
                    int runNumber = data[1];
                    if (runNumber < minRunNumber) {
                        minRunNumber = runNumber;
                    }
                    if (runNumber > maxRunNumber) {
                        maxRunNumber = runNumber;
                    }
                } else if (isGoEvent(event)) {
                    int seconds = data[0];
                    long time = ((long) seconds) * 1000000000;
                    if (!metadata.containsKey("tStart")) {
                        metadata.put("tStart", new Date(((long) seconds) * 1000));
                    } else {
                        throw new RuntimeException("More than one Go record was found!");
                    }
                } else if (isEndEvent(event)) {
                    int seconds = data[0];
                    long time = ((long) seconds) * 1000000000;
                    if (!metadata.containsKey("tEnd")) {
                        metadata.put("tEnd", new Date(((long) seconds) * 1000));
                    } else {
                        throw new RuntimeException("More than one End record was found!");
                    }
                } else if (isPhysicsEvent(event)) {
                    ++nevents;
                }
                event = reader.nextEvent();
            }
            
            metadata.put("nRun", minRunNumber);
            
        } catch (EvioException e) {
            throw new IOException(e);
        } finally {
            if (reader != null)
                reader.close();    
        }       
    }

    @Override
    public long getEventCount() throws IOException {
        return nevents;
    }

    @Override
    public Map<String, Object> getMetaData() throws IOException {
        return metadata;
    }

    @Override
    public long getRunMax() throws IOException {
        return maxRunNumber;
    }

    @Override
    public long getRunMin() throws IOException {
        return minRunNumber;
    }

    @Override
    public String getStatus() throws IOException {
        return status;
    }

    @Override
    public void close() throws IOException {
    }
    
    static boolean isPreStartEvent(EvioEvent event) {
        return event.getHeader().getTag() == PRESTART_EVENT_TAG;
    }

    static boolean isGoEvent(EvioEvent event) {
        return event.getHeader().getTag() == GO_EVENT_TAG;
    }
    
    static boolean isEndEvent(EvioEvent event) {
        return event.getHeader().getTag() == END_EVENT_TAG;
    }
    
    static boolean isPhysicsEvent(EvioEvent event) {
        return event.getHeader().getTag() == PHYSICS_EVENT_TAG;
    }

}
