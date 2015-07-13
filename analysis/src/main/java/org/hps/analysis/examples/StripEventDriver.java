package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripEventDriver extends Driver
{

    private int _minNumberOfTracks = 0;
    private int _minNumberOfHitsOnTrack = 0;
    private int _numberOfEventsWritten = 0;
    private int _minNumberOfUnconstrainedV0Vertices = 0;
    private int _minNumberOfStripHits = 0;
    private int _maxNumberOfStripHits = Integer.MAX_VALUE;

    @Override
    protected void process(EventHeader event)
    {
        boolean skipEvent = false;
        int nTracks = 0;

        if (event.hasCollection(Track.class, "MatchedTracks")) {
            nTracks = event.get(Track.class, "MatchedTracks").size();
            if (nTracks >= _minNumberOfTracks) {
                List<Track> tracks = event.get(Track.class, "MatchedTracks");
                for (Track t : tracks) {
                    int nhits = t.getTrackerHits().size();
                    if (nhits < _minNumberOfHitsOnTrack) {
                        skipEvent = true;
                    }
                }
            } else {
                skipEvent = true;
            }
        }
        if (event.hasCollection(Vertex.class, "UnconstrainedV0Vertices")) {
            int nVertices = event.get(Vertex.class, "UnconstrainedV0Vertices").size();
            if (nVertices < _minNumberOfUnconstrainedV0Vertices) {
                skipEvent = true;
            }
        }
        if(event.hasCollection(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D")) {
            int nHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D").size();
            if (nHits < _minNumberOfStripHits) {
                skipEvent = true;
            }
            if (nHits > _maxNumberOfStripHits) {
                skipEvent = true;
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

    public void setMinNumberOfTracks(int nTracks)
    {
        _minNumberOfTracks = nTracks;
    }

    public void setMinNumberOfHitsOnTrack(int nHits)
    {
        _minNumberOfHitsOnTrack = nHits;
    }

    public void setMinNumberOfUnconstrainedV0Vertices(int nVertices)
    {
        _minNumberOfUnconstrainedV0Vertices = nVertices;
    }
    
    public void setMinNumberOfStripHits(int n)
    {
        _minNumberOfStripHits = n;
    }
    
    public void setMaxNumberOfStripHits(int n)
    {
        _maxNumberOfStripHits = n;
    }

}
