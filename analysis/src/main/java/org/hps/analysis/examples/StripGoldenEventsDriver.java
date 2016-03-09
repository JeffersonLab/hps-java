package org.hps.analysis.examples;

import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;

/**
 * Class to strip off "golden events"
 * Currently defined as e+ e- with six-hit tracks and nothing else in the event
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripGoldenEventsDriver extends Driver
{

    private int _numberOfEventsWritten = 0;

    @Override
    protected void process(EventHeader event)
    {
        // select "golden" events with two tracks, two clusters and e+ e-
        boolean skipEvent = false;
        if (event.hasCollection(ReconstructedParticle.class, "UnconstrainedV0Candidates")) {
            List<ReconstructedParticle> vertices = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        //System.out.println("Thete are: "+vertices.size()+" Unconstrained V0 candidates");
            if (vertices.size() > 1 || vertices.isEmpty()) {
                skipEvent = true;
            } else {
                ReconstructedParticle vtx = vertices.get(0);
                //this always has 2 tracks. 
                List<ReconstructedParticle> rps = vtx.getParticles();
                for (ReconstructedParticle rp : rps) {
                    List<Track> trks = rp.getTracks();
                    // require each track to have six hits
                    if (trks.get(0).getTrackerHits().size() != 6) {
            //System.out.println("Thete are: "+trks.get(0).getTrackerHits().size()+" hits on Track");
                        skipEvent = true;
                    }
                }
                // require no other tracks in the event
                if (event.get(Track.class, "MatchedTracks").size() > 2) {
                    skipEvent = true;
            //System.out.println("Thete are: "+event.get(Track.class, "MatchedTracks").size()+" Matched tracks");
                }
                // require no other clusters in the event
                if (event.get(Cluster.class, "EcalClustersGTP").size() > 2) {
                    skipEvent = true;
                }
            }
        } else {
            skipEvent = true;
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            System.out.println(event.getRunNumber() + " " + event.getEventNumber());
            _numberOfEventsWritten++;
        }
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }
}
