package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;

/**
 * Class to strip off Moller candidates 
 * Currently defined as:
 * e- e- events 
 * non-full-energy electrons
 * momentum sum between 0.85 and 1.3 GeV 
 * six-hit tracks and 
 * nothing else in the event
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripMollerEventsDriver extends Driver
{

    private String finalStateParticlesColName = "FinalStateParticles";
    private int _numberOfEventsWritten = 0;

    private boolean debug;

    double molPSumMin = 0.85;
    double molPSumMax = 1.3;
    double beambeamCut = 0.85;

    @Override
    protected void process(EventHeader event)
    {
        boolean skipEvent = true;
        if (event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
            if (debug) {
                System.out.println("This events has " + finalStateParticles.size() + " final state particles");
            }
            // require two and only two ReconstructedParticles
            // require no other tracks in the event
            // require no other clusters in the event
            if (finalStateParticles.size() == 2
                    && event.get(Track.class, "MatchedTracks").size() == 2
                    && event.get(Cluster.class, "EcalClustersGTP").size() == 2) {
                // at this point we should have two and only two good particles in the event
                // let's see what they are...
                ReconstructedParticle ele1 = null;
                ReconstructedParticle ele2 = null;
                int sumCharge = 0;
                int numChargedParticles = 0;
                for (ReconstructedParticle fsPart : finalStateParticles) {
                    if (debug) {
                        System.out.println("PDGID = " + fsPart.getParticleIDUsed() + "; charge = " + fsPart.getCharge() + "; pz = " + fsPart.getMomentum().x());
                    }
                    double charge = fsPart.getCharge();
                    sumCharge += charge;
                    if (charge != 0) {
                        numChargedParticles++;
                        if (charge < 1) {
                            if (ele1 == null) {
                                ele1 = fsPart;
                            } else {
                                ele2 = fsPart;
                            }
                        }
                    }
                }

                if (ele1 != null && ele2 != null) {
                    Hep3Vector p1 = ele1.getMomentum();
                    Hep3Vector p2 = ele2.getMomentum();
                    Hep3Vector beamAxis = new BasicHep3Vector(Math.sin(0.0305), 0, Math.cos(0.0305));
                    double theta1 = Math.acos(VecOp.dot(p1, beamAxis) / p1.magnitude());
                    double theta2 = Math.acos(VecOp.dot(p2, beamAxis) / p2.magnitude());
                    //look at "Moller" events (if that's what they really are)
                    if (ele1.getMomentum().magnitude() + ele2.getMomentum().magnitude() > molPSumMin
                            && ele1.getMomentum().magnitude() + ele2.getMomentum().magnitude() < molPSumMax
                            && (p1.magnitude() < beambeamCut && p2.magnitude() < beambeamCut)) {

                        // require that both tracks have six hits
                        Track ele1trk = ele1.getTracks().get(0);
                        Track ele2trk = ele2.getTracks().get(0);
                        if (ele1trk.getTrackerHits().size() == 6
                                && ele2trk.getTrackerHits().size() == 6) {
                            // OK. we should be golden here
                            skipEvent = false;
                        }
                    }
            //System.out.println("Thete are: "+event.get(Track.class, "MatchedTracks").size()+" Matched tracks");
                }
            }
        }

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            System.out.println(event.getRunNumber() + " " + event.getEventNumber());
            _numberOfEventsWritten++;
        }
    }

    private double getMomentum(Track trk)
    {
        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }
}
