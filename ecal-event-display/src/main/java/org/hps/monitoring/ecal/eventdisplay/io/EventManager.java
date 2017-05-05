package org.hps.monitoring.ecal.eventdisplay.io;

import java.io.IOException;
import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * Interface <code>EventManager</code> is responsible for traversing
 * an event data file and extracting lists of calorimeter hits and
 * hit clusters from it to be passed to a <code>FileViewer</code>.
 */
public interface EventManager {
    /**
     * <b>close</b><br/><br/>
     * <code>public void <b>close</b>()</code><br/><br/>
     * Closes the event manager. Once this is performed, no additional events
     * may be read.
     * @throws IOException Occurs if there is an error closing the file stream.
     **/
    public void close() throws IOException;
    
    /**
     * <b>getEventNumber</b><br/><br/>
     * <code>public int <b>getEventNumber</b>()</code><br/><br/>
     * Gets the ordinal number for the currently displayed event.
     * @return Returns the current event's ordinal number.
     */
    public int getEventNumber();
    
    /**
     * <b>getClusters</b><br/><br/>
     * <code>public ArrayList<Cluster> <b>getClusters</b>()</code><br/><br/>
     * Allows access to the current event's list of clusters.
     * @return Returns the current clusters as an <code>ArrayList
     * </code> object.
     **/
    public List<Cluster> getClusters();
    
    /**
     * <b>getHits</b><br/><br/>
     * <code>public ArrayList<EcalHit> <b>getHits</b>()</code><br/><br/>
     * Allows access to the current event's list of hits.
     * @return Returns the current hits as an <code>ArrayList</code> object.
     **/
    public List<EcalHit> getHits();
    
    /**
     * <b>nextEvent</b><br/><br/>
     * <code>public boolean <b>nextEvent</b>()</code><br/><br/>
     * Populates the event manager with hits and clusters from the next event.
     * @return Returns <code>true</code> if an event was read and <code>false
     * </code> if it was not.
     * @throws IOException Occurs if there was a file read error.
     **/
    public boolean nextEvent() throws IOException;
    
    /**
     * <b>previousEvent</b><br/><br/>
     * <code>public boolean <b>previousEvent</b>()</code><br/><br/>
     * Populates the event manager with hits and clusters from the previous event.
     * @return Returns <code>true</code> if an event was read and <code>false
     * </code> if it was not.
     * @throws IOException Occurs if there was a file read error.
     **/
    public boolean previousEvent() throws IOException;
}
