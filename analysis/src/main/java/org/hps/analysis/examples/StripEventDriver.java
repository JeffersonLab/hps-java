package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.Cluster;
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
    private int _minNumberOfClusters = 0;
    private int _maxNumberOfClusters = Integer.MAX_VALUE;
    private double _minClusterEnergy = 0.;
    private double _maxClusterEnergy = 12.;

    private String _clusterCollectionName = "EcalClusters";

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
        if (event.hasCollection(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D")) {
            int nHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D").size();
            if (nHits < _minNumberOfStripHits) {
                skipEvent = true;
            }
            if (nHits > _maxNumberOfStripHits) {
                skipEvent = true;
            }
        }
        if (event.hasCollection(Cluster.class, _clusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, _clusterCollectionName);
            int nclusters = clusters.size();
            if (nclusters < _minNumberOfClusters) {
                skipEvent = true;
            }
            if (nclusters > _maxNumberOfClusters) {
                skipEvent = true;
            }
            for (Cluster clus : clusters) {
                double e = clus.getEnergy();
                if (e < _minClusterEnergy) {
                    skipEvent = true;
                }
                if (e > _maxClusterEnergy) {
                    skipEvent = true;
                }
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

    public void setMinNumberOfClusters(int n)
    {
        _minNumberOfClusters = n;
    }

    public void setMaxNumberOfClusters(int n)
    {
        _maxNumberOfClusters = n;
    }

    public void setMinClusterEnergy(double e)
    {
        _minClusterEnergy = e;
    }

    public void setMaxClusterEnergy(double e)
    {
        _maxClusterEnergy = e;
    }

    public void setClusterCollectionName(String s)
    {
        _clusterCollectionName = s;
    }

}
