package org.hps.analysis.examples;

import java.util.List;

import org.hps.recon.tracking.StrategyType;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;

/**
 * Example analysis {@link Driver} showing how to use SVT track collections
 * and how to filter out unwanted ones. 
 * 
 * @author <a href="mailto:moreno1@ucsc.edu">Omar Moreno</a>
 */
public class SvtTrackAnalysis extends Driver {

    // Collections
    private String matchedTracksColName = "MatchedTracks";
    private String tracksS456ColName    = "Tracks_s345_c2_e16";
    private String tracksS123C4ColName  = "Tracks_s123_c4_e56";
    private String tracksS123C5ColName  = "Tracks_s123_c5_e46";
    
    @Override
    public void process(EventHeader event) { 
        
        // Check if an event has a specific collection and if it doesn't, 
        // skip it  The default track collection is still "MatchedTracks"
        // and refers to tracks found using the strategy seed 345, 
        // confirm 2, extend 16.  
        if (!event.hasCollection(Track.class, matchedTracksColName)) return;
        
        // Once the collection has been confirmed to exist in the event, 
        // you can retrieve a reference to the collection from the event
        // as follows.
        List<Track> tracks = event.get(Track.class, matchedTracksColName);
    
        // The track collection can be iterated over and each track can 
        // then be analyzed
        for (Track track : tracks) { 
           // Analysis Code goes here ... 
        }
        
        // The other collections can be retrieved in a similar manner
        
        if (!event.hasCollection(Track.class, tracksS456ColName)) return;
        tracks = event.get(Track.class, tracksS456ColName);
       
        // Analysis goes here ...
        
        if (!event.hasCollection(Track.class, tracksS123C4ColName)) return;
        tracks = event.get(Track.class, tracksS123C4ColName);

        // Analysis goes here ...
        
        if (!event.hasCollection(Track.class, tracksS123C5ColName)) return;
        tracks = event.get(Track.class, tracksS123C5ColName);
    
        // Analysis goes here ...
        
        //
        // If looking at all track collections, a filter can be used to look
        // at only specific tracks
        //
        
        // Check if the event has a LCIO Track collection to begin
        if (!event.hasCollection(Track.class));
        
        // Get all LCIO Track collections from an event
        List<List<Track>> trackCollections = event.get(Track.class);
    
        // Iterate over all track collections
        for (List<Track> trackCollection : trackCollections) { 
            
            // Check if the collection has tracks.  If it does not, skip it.
            if (trackCollection.isEmpty()) continue;
           
            // Use the track type to check if the tracks are from the
            // collections of interest
            
            if (TrackType.getType(StrategyType.MATCHED_TRACKS) != trackCollection.get(0).getType()) continue;
           
            for (Track track : trackCollection) { 
                // Analysis code goes here ...
            }
        }
    }
}
