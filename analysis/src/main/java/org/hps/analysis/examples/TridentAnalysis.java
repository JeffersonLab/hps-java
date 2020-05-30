package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
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
    double emass = 0.000511;
    double emass2 = emass * emass;
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[3];
    double[] p2 = new double[3];
    double[] p3 = new double[3];
    Map<Double, String> layerCodes = new HashMap<>();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        double bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., -4.3)).y();
        vtxFitter = new BilliorVertexer(bfield);
        System.out.println("using bfield " + bfield);
        for (double d = 0.; d < 14; ++d) {
            layerCodes.put(2., "other");
        }
        layerCodes.put(2., "L1L1");
        layerCodes.put(3., "L1L2");
        layerCodes.put(4., "L2L2");
    }

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        aida.histogram1D("Number of V0s in the event", 10, -0.5, 9.5).fill(V0List.size());
        List<ReconstructedParticle> VCList = event.get(ReconstructedParticle.class, "UnconstrainedVcCandidates");
        aida.histogram1D("Number of VCs in the event", 10, -0.5, 9.5).fill(VCList.size());
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, reconstructedParticleCollectionName);
        if (rpList.size() == 3) { // note that requring three or less FSPs means we can have at most two V0s in the event

            aida.tree().mkdirs("run " + event.getRunNumber() + " " + V0List.size() + " event V0s");
            aida.tree().cd("run " + event.getRunNumber() + " " + V0List.size() + " event V0s");
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
//                System.out.println("V0 as four vector " + V0.asFourVector());
//                System.out.println("third as fourvector " + third.asFourVector());
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
                            // the method asFourVector is not reliable, as it uses the ECal cluster energy
                            // for the energy. For unassociated electron tracks this is zero!
                            // recalculate all myself using only tracking information.
                            Vertex v = event.get(Vertex.class, "UnconstrainedV0Vertices").get(0);
                            String vLayerCode = layerCodes.get(v.getParameters().get("layerCode"));
                            Map<String, Double> vals = v.getParameters();
                            // V0PzErr
                            // invMass  
                            // V0Pz  
                            // vXErr 
                            // V0Py  
                            // V0Px  
                            // V0PErr  
                            // V0TargProjY  
                            // vZErr  
                            // V0TargProjXErr 
                            // vYErr  
                            // V0TargProjYErr  
                            // invMassError  
                            // p1X  
                            // p2Y  
                            // p2X  
                            // V0P  
                            // p1Z  
                            // p1Y  
                            // p2Z  
                            // V0TargProjX  
                            // layerCode 
                            // V0PxErr  
                            // V0PyErr                              
//                            for (String s : vals.keySet()) {
//                                System.out.println("Vertex key " + s + " val: " + vals.get(s));
//                            }
                            p1[0] = vals.get("p1X");
                            p1[1] = vals.get("p1Y");
                            p1[2] = vals.get("p1Z");
                            HepLorentzVector fourVec1 = makeFourVector(p1, emass);
                            p2[0] = vals.get("p2X");
                            p2[1] = vals.get("p2Y");
                            p2[2] = vals.get("p2Z");
                            HepLorentzVector fourVec2 = makeFourVector(p2, emass);
                            p3[0] = third.getMomentum().x();
                            p3[1] = third.getMomentum().y();
                            p3[2] = third.getMomentum().z();
                            HepLorentzVector fourVec3 = makeFourVector(p3, emass);

                            HepLorentzVector trident = VecOp.add(fourVec1, fourVec2);
//                            System.out.println("V0 as four vector " + V0.asFourVector());
//                            System.out.println("my V0 as four vector " + trident);

                            trident = VecOp.add(trident, fourVec3);
                            Hep3Vector tridentVec = VecOp.mult(beamAxisRotation, trident.v3());
//                            System.out.println("my trident vector " + tridentVec);

                            aida.histogram1D("trident energy", 100, 2.0, 2.6).fill(trident.t());
                            aida.histogram1D("trident pX", 100, -0.2, 0.2).fill(tridentVec.x());
                            aida.histogram1D("trident pY", 100, -0.2, 0.2).fill(tridentVec.y());
                            aida.histogram1D("trident pZ", 100, 2.0, 2.6).fill(tridentVec.z());
                            //let's analyze by number of hits on tracks
                            int nHitsOnElectron = ele.getTracks().get(0).getTrackerHits().size();
                            int nHitsOnPositron = pos.getTracks().get(0).getTrackerHits().size();
                            int nHitsOnRecoil = third.getTracks().get(0).getTrackerHits().size();

                            aida.histogram1D("Trident x " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, -2., 2.).fill(tridentPos.x());
                            aida.histogram1D("Trident y " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, -2., 2.).fill(tridentPos.y());
                            aida.histogram1D("Trident z " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, -20., 20.).fill(tridentPos.z());
                            aida.histogram1D("trident pX " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, -0.2, 0.2).fill(tridentVec.x());
                            aida.histogram1D("trident pY " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, -0.2, 0.2).fill(tridentVec.y());
                            aida.histogram1D("trident pZ " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, 2.0, 2.6).fill(tridentVec.z());
                            aida.histogram1D("trident energy " + vLayerCode + " " + nHitsOnElectron + " " + nHitsOnPositron + " " + nHitsOnRecoil, 100, 2.0, 2.6).fill(trident.t());

                            // separate out whether both electrons are in the same hemisphere or not
                            // if e+ and recoil electron are both in the top or both in the bottom, might be converted WAB
                            // could also check for UnconstrainedVcVertices in the event...
                            aida.histogram1D("Number of VCs in clean event", 10, -0.5, 9.5).fill(VCList.size());
                            if (VCList.size() == 1 && event.get(Vertex.class, "UnconstrainedVcVertices").size() == 1) // found e+ e- in same hemisphere, possible WAB photon conversion
                            {
                                Vertex vc = event.get(Vertex.class, "UnconstrainedVcVertices").get(0);
                                String vcLayerCode = layerCodes.get(vc.getParameters().get("layerCode"));
                                ReconstructedParticle conv = VCList.get(0);
                                Hep3Vector convPos = conv.getReferencePoint();
                                aida.histogram1D("VC x", 100, -5., 15.).fill(convPos.x());
                                aida.histogram1D("VC y", 100, -10., 10.).fill(convPos.y());
                                aida.histogram2D("VC x vs y", 300, -5., 15., 300, -10., 10.).fill(convPos.x(), convPos.y());
                                aida.histogram2D("VC x vs z", 300, -50., 200., 300, -5., 15.).fill(convPos.z(), convPos.x());
                                aida.histogram2D("VC y vs z", 300, -50., 200., 300, -10., 10.).fill(convPos.z(), convPos.y());
                                //
                                aida.histogram1D("VC x " + vcLayerCode, 100, -5., 15.).fill(convPos.x());
                                aida.histogram1D("VC y " + vcLayerCode, 100, -10., 10.).fill(convPos.y());
                                aida.histogram2D("VC x vs y " + vcLayerCode, 300, -5., 15., 300, -10., 10.).fill(convPos.x(), convPos.y());
                                aida.histogram2D("VC x vs z " + vcLayerCode, 300, -50., 200., 300, -5., 15.).fill(convPos.z(), convPos.x());
                                aida.histogram2D("VC y vs z " + vcLayerCode, 300, -50., 200., 300, -10., 10.).fill(convPos.z(), convPos.y());
                                //
                                if (convPos.y() > 0) {
                                    aida.histogram1D("VC z top", 300, -50., 200.).fill(convPos.z());
                                    aida.histogram1D("VC z top " + vcLayerCode + " " + nHitsOnPositron + " " + nHitsOnRecoil, 300, -50., 200.).fill(convPos.z());
                                } else {
                                    aida.histogram1D("VC z bottom", 300, -50., 200.).fill(convPos.z());
                                    aida.histogram1D("VC z bottom " + vcLayerCode + " " + nHitsOnPositron + " " + nHitsOnRecoil, 300, -50., 200.).fill(convPos.z());
                                }
                            }
                        }

                    }// end of electron trident
                }
            }//end of loop over V0s
            aida.tree().cd("..");
        }//end of check on three FSPs
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

    private BasicHepLorentzVector makeFourVector(double[] mom, double mass) {
        double emass2 = emass * emass;
        double e = emass2;
        for (int i = 0; i < 3; ++i) {
            e += mom[i] * mom[i];
        }
        e = sqrt(e);
        return new BasicHepLorentzVector(e, mom);
    }
}
