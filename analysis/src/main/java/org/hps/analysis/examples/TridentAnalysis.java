package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis of events containing a V0 and one other FinalStateParticle See if we
 * can find the recoil Three particles should sum to the beam four-vector
 *
 * @author Norman A. Graf
 */
public class TridentAnalysis extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String vertexCollectionName = "UnconstrainedV0Vertices";
    String reconstructedParticleCollectionName = "FinalStateParticles";
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();
    BilliorVertexer vtxFitter;
    private int _numberOfEventsSelected = 0;

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        double bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., -4.3)).y();
        vtxFitter = new BilliorVertexer(bfield);
        System.out.println("using bfield " + bfield);
    }

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        aida.histogram1D("Number of V0s in the event", 10, -0.5, 9.5).fill(V0List.size());
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, reconstructedParticleCollectionName);
        if (rpList.size() != 3) { // note that requring three or less FSPs means we can have at most two V0s in the event
            return;
        }
        aida.tree().mkdirs(V0List.size() + " event V0s");
        aida.tree().cd(V0List.size() + " event V0s");
//        if (V0List.size() != 1) {
//            return;
//        }
        for (ReconstructedParticle V0 : V0List) {
            List<ReconstructedParticle> trks = V0.getParticles();
            ReconstructedParticle ele = trks.get(0);
            ReconstructedParticle pos = trks.get(1);
            ReconstructedParticle third = null;
            String type;
            for (ReconstructedParticle rp : rpList) {

                if (rp == ele) {
                    continue;
                }
                if (rp == pos) {
                    continue;
                }
                third = rp;
            }
            int pdgId = third.getParticleIDUsed().getPDG();

            switch (pdgId) {
                case 11:
                    type = "electron";
                    break;
                case 22:
                    type = "photon";
                    break;
                case -11:
                    type = "positron";
                    break;
                default:
                    type = "unknown";
                    break;
            }

            HepLorentzVector beam = VecOp.add(V0.asFourVector(), third.asFourVector());
            Hep3Vector beamVec = VecOp.mult(beamAxisRotation, beam.v3());
            double beamEnergy = beam.t();

            aida.histogram1D("V0 + " + type + " energy", 100, 0.5, 5.5).fill(beamEnergy);
            aida.histogram1D("V0 + " + type + " pX", 100, -0.2, 0.2).fill(beamVec.x());
            aida.histogram1D("V0 + " + type + " pY", 100, -0.2, 0.2).fill(beamVec.y());
            aida.histogram1D("V0 + " + type + " pZ", 100, 0.5, 5.5).fill(beamVec.z());

            double deltaPz = 0.3;
            double beamPz = 2.3;
            if (abs(beamPz - beamVec.z()) < deltaPz) {
                aida.histogram1D("V0 + " + type + " energy after pZ cut", 100, 0.5, 5.5).fill(beamEnergy);
                aida.histogram1D("V0 + " + type + " pX after pZ cut", 100, -0.2, 0.2).fill(beamVec.x());
                aida.histogram1D("V0 + " + type + " pY after pZ cut", 100, -0.2, 0.2).fill(beamVec.y());
                aida.histogram1D("V0 + " + type + " pZ after pZ cut", 100, 0.5, 5.5).fill(beamVec.z());
            }

            // try fitting all three tracks...
            if (type.equals("electron")) {
                List<BilliorTrack> tracksToVertex = new ArrayList<>();
                tracksToVertex.add(new BilliorTrack(ele.getTracks().get(0)));
                tracksToVertex.add(new BilliorTrack(pos.getTracks().get(0)));
                tracksToVertex.add(new BilliorTrack(third.getTracks().get(0)));
                BilliorVertex vtx = vtxFitter.fitVertex(tracksToVertex);
                Hep3Vector v0Pos = V0.getReferencePoint();
                Hep3Vector tridentPos = vtx.getPosition();
                aida.histogram1D("V0 x", 100, -2., 2.).fill(v0Pos.x());
                aida.histogram1D("V0 y", 100, -2., 2.).fill(v0Pos.y());
                aida.histogram1D("V0 z", 100, -20., 20.).fill(v0Pos.z());
                aida.histogram1D("Trident x", 100, -2., 2.).fill(tridentPos.x());
                aida.histogram1D("Trident y", 100, -2., 2.).fill(tridentPos.y());
                aida.histogram1D("Trident z", 100, -20., 20.).fill(tridentPos.z());
                // good event, let's strip it, but only in single V0 events
                if (V0List.size() == 1) {
                    skipEvent = false;
                }
            }// end of electron trident
        }//end of loop over V0s
        aida.tree().cd("..");
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    @Override
    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }
}
