package org.hps.analysis.alignment;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class V0SvtAlignmentDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    String vertexCollectionName = "UnconstrainedV0Vertices";
    private Double _beamEnergy = 1.056;
    // Beam position variables.
    // The beamPosition array is in the tracking frame
    /* TODO get the beam position from the conditions db */
    protected double[] beamPosition = {0.0, 0.0, 0.0}; //
    protected double bField;
    // flipSign is a kludge...
    // HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    // so we set the B-field in +iveZ and flip signs of fitted tracks
    //
    // Note: This should be -1 for test run configurations and +1 for
    // prop-2014 configurations
    private int flipSign = 1;

    List<ReconstructedParticle> _topElectrons = new ArrayList<ReconstructedParticle>();
    List<ReconstructedParticle> _bottomElectrons = new ArrayList<ReconstructedParticle>();
    List<ReconstructedParticle> _topPositrons = new ArrayList<ReconstructedParticle>();
    List<ReconstructedParticle> _bottomPositrons = new ArrayList<ReconstructedParticle>();

    /**
     * Updates the magnetic field parameters to match the appropriate values for
     * the current detector settings.
     */
    @Override
    protected void detectorChanged(Detector detector) {

        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0) {
            flipSign = -1;
        }
    }

    protected void process(EventHeader event) {

        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
        }

        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        if (vertices.size() != 1) {
            return;
        }

        for (Vertex v : vertices) {
            ReconstructedParticle electron = null;
            ReconstructedParticle positron = null;
            ReconstructedParticle rp = v.getAssociatedParticle();
            int type = rp.getType();
            boolean isGbl = TrackType.isGBL(type);
            // require GBL tracks in vertex
            if (isGbl) {
                List<ReconstructedParticle> parts = rp.getParticles();
                ReconstructedParticle rp1 = parts.get(0);
                ReconstructedParticle rp2 = parts.get(1);
                // basic sanity check here, remove full energy electrons (fee)
                if (rp1.getMomentum().magnitude() > .85 * _beamEnergy || rp2.getMomentum().magnitude() > .85 * _beamEnergy) {
                    continue;
                }
                // require both reconstructed particles to have a track

                if (rp1.getTracks().size() != 1) {
                    continue;
                }
                if (rp2.getTracks().size() != 1) {
                    continue;
                }
                Track t1 = rp1.getTracks().get(0);
                Track t2 = rp2.getTracks().get(0);
                // only analyze 6-hit tracks (for now)
                int t1Nhits = t1.getTrackerHits().size();
                if (t1Nhits != 6) {
                    continue;
                }
                int t2Nhits = t2.getTrackerHits().size();
                if (t2Nhits != 6) {
                    continue;
                }
                // require both reconstructed particles to have a cluster
                if (rp1.getClusters().size() != 1) {
                    continue;
                }
                if (rp2.getClusters().size() != 1) {
                    continue;
                }
                Cluster c1 = rp1.getClusters().get(0);
                Cluster c2 = rp2.getClusters().get(0);
                double deltaT = ClusterUtilities.getSeedHitTime(c1) - ClusterUtilities.getSeedHitTime(c2);
                // require cluster times to be coincident within 2 ns
                if (abs(deltaT) > 2.0) {
                    continue;
                }
                // should be good to go here
                Hep3Vector vPos = v.getPosition();
                aida.histogram1D("vertex x position", 100, -10., 10.).fill(vPos.x());
                aida.histogram1D("vertex y position", 100, -10., 10.).fill(vPos.y());
                aida.histogram1D("vertex z position", 200, -50., 50.).fill(vPos.z());
                electron = (rp1.getParticleIDUsed().getPDG() == 11) ? rp1 : rp2;
                positron = (rp2.getParticleIDUsed().getPDG() == -11) ? rp2 : rp1;

                aida.histogram1D("electron momentum", 100, 0., 1.).fill(electron.getMomentum().magnitude());
                aida.histogram1D("positron momentum", 100, 0., 1.).fill(positron.getMomentum().magnitude());

                if (electron.getClusters().get(0).getPosition()[1] > 0) {
                    _topElectrons.add(electron);
                    plotit(electron, "top electron");
                } else {
                    _bottomElectrons.add(electron);
                    plotit(electron, "bottom electron");
                }

                if (positron.getClusters().get(0).getPosition()[1] > 0) {
                    _topPositrons.add(positron);
                    plotit(positron, "top positron");
                } else {
                    _bottomPositrons.add(positron);
                    plotit(positron, "bottom positron");
                }
            }
        }
        // can't accumulate over all the data as we run out of memory
        // stop every once in a while and process what we have, then release the memory
        if (_topElectrons.size() * _topPositrons.size() != 0) {
            //vertex top positrons and electrons
            int nTop = min(_topElectrons.size(), _topPositrons.size());
            for (int i = 0; i < nTop; ++i) {
                BilliorVertex vtx = fitVertex(_topElectrons.get(i), _topPositrons.get(i));
                aida.histogram1D("top vertex x position", 100, -10., 10.).fill(vtx.getPosition().x());
                aida.histogram1D("top vertex y position", 100, -10., 10.).fill(vtx.getPosition().y());
                aida.histogram1D("top vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
                _topElectrons.remove(i);
                _topPositrons.remove(i);
            }
        }
        if (_bottomElectrons.size() * _bottomPositrons.size() != 0) {
            //vertex bottom electrons and positrons
            int nBottom = min(_bottomElectrons.size(), _bottomPositrons.size());
            for (int i = 0; i < nBottom; ++i) {
                BilliorVertex vtx = fitVertex(_bottomElectrons.get(i), _bottomPositrons.get(i));
                aida.histogram1D("bottom vertex x position", 100, -10., 10.).fill(vtx.getPosition().x());
                aida.histogram1D("bottom vertex y position", 100, -10., 10.).fill(vtx.getPosition().y());
                aida.histogram1D("bottom vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
                _bottomElectrons.remove(i);
                _bottomPositrons.remove(i);
            }
        }
    }

    private void plotit(ReconstructedParticle rp, String s) {
        Track t = rp.getTracks().get(0);
        Hep3Vector pmom = rp.getMomentum();
        double z0 = t.getTrackStates().get(0).getZ0();
        double thetaY = Math.asin(pmom.y() / pmom.magnitude());
        aida.histogram1D(s + " z0", 100, -1.5, 1.5).fill(z0);
        aida.histogram1D(s + " thetaY", 100, .015, .065).fill(abs(thetaY));
        aida.profile1D(s + " track thetaY vs z0 profile", 50, 0.02, 0.04).fill(abs(thetaY), z0);
        aida.histogram2D(s + " track thetaY vs z0", 100, .015, 0.065, 100, -1.5, 1.5).fill(abs(thetaY), z0);
        aida.histogram1D(s + " track momentum", 100, 0., 1.).fill(rp.getMomentum().magnitude());
//        TrackState ts = t.getTrackStates().get(0);
//        double tsD0 = ts.getD0();
//        double tsZ0 = ts.getZ0();
//        double tsTanL = ts.getTanLambda();
//        aida.profile1D(s + " trackState tanLambda vs trackstate z0 profile", 50, 0.02, 0.04).fill(abs(tsTanL), tsZ0);
//        aida.histogram2D(s + " trackState tanLambda vs trackstate z0", 100, .015, 0.065, 100, -1.5, 1.5).fill(abs(tsTanL), tsZ0);
    }

//    @Override
//    protected void endOfData() {
//
//        System.out.println("found " + _topElectrons.size() + " top electrons");
//        System.out.println("found " + _topPositrons.size() + " top positrons");
//        System.out.println("found " + _bottomElectrons.size() + " bottom electrons");
//        System.out.println("found " + _bottomPositrons.size() + " bottom positrons");
//
//        //vertex top positrons and electrons
//        int nTop = min(_topElectrons.size(), _topPositrons.size());
//        for (int i = 0; i < nTop; ++i) {
//            BilliorVertex vtx = fitVertex(_topElectrons.get(i), _topPositrons.get(i));
//            aida.histogram1D("top vertex x position").fill(vtx.getPosition().x());
//            aida.histogram1D("top vertex y position").fill(vtx.getPosition().y());
//            aida.histogram1D("top vertex z position").fill(vtx.getPosition().z());
//        }
//        //vertex bottom electrons and positrons
//        int nBottom = min(_bottomElectrons.size(), _bottomPositrons.size());
//        for (int i = 0; i < nBottom; ++i) {
//            BilliorVertex vtx = fitVertex(_bottomElectrons.get(i), _bottomPositrons.get(i));
//            aida.histogram1D("bottom vertex x position").fill(vtx.getPosition().x());
//            aida.histogram1D("bottom vertex y position").fill(vtx.getPosition().y());
//            aida.histogram1D("bottom vertex z position").fill(vtx.getPosition().z());
//        }
//
//    }
    /**
     * Fits a vertex from an electron/positron track pair using the indicated
     * constraint.
     *
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object. mg--8/14/17--add the displaced vertex refit for the
     * UNCONSTRAINED and BS_CONSTRAINED fits
     */
    private BilliorVertex fitVertex(ReconstructedParticle electron, ReconstructedParticle positron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));

        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.

        // Perform the vertexing
        vtxFitter.doBeamSpotConstraint(false);

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
        billiorTracks.add(electronBTrack);
        billiorTracks.add(positronBTrack);

        // Find a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);

        // mg 8/14/17 
        // if this is an unconstrained or BS constrained vertex, propogate the 
        // tracks to the vertex found in previous fit and do fit again
        //  ...  this is required because the vertex fit assumes trajectories 
        // change linearly about the reference point (which we initially guess to be 
        // (0,0,0) while for long-lived decays there is significant curvature
        List<ReconstructedParticle> recoList = new ArrayList<ReconstructedParticle>();
        recoList.add(electron);
        recoList.add(positron);
        List<BilliorTrack> shiftedTracks = shiftTracksToVertex(recoList, vtx.getPosition());

        BilliorVertex vtxNew = vtxFitter.fitVertex(shiftedTracks);
        Hep3Vector vtxPosNew = VecOp.add(vtx.getPosition(), vtxNew.getPosition());//the refit vertex is measured wrt the original vertex position
        vtxNew.setPosition(vtxPosNew);//just change the position...the errors and momenta are correct in re-fit
        return vtxNew;

    }

    private List<BilliorTrack> shiftTracksToVertex(List<ReconstructedParticle> particles, Hep3Vector vtxPos) {
        ///     Ok...shift the reference point....        
        double[] newRef = {vtxPos.z(), vtxPos.x(), 0.0};//the  TrackUtils.getParametersAtNewRefPoint method only shifts in xy tracking frame
        List<BilliorTrack> newTrks = new ArrayList<BilliorTrack>();
        for (ReconstructedParticle part : particles) {
            BaseTrackState oldTS = (BaseTrackState) part.getTracks().get(0).getTrackStates().get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTS);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTS.getReferencePoint(), oldTS.getParameters(), new SymmetricMatrix(5, oldTS.getCovMatrix(), true));
            //mg...I don't like this re-casting, but toBilliorTrack only takes Track as input
            BaseTrackState newTS = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtIP, bField);
            BilliorTrack electronBTrackShift = this.toBilliorTrack(newTS);
            newTrks.add(electronBTrackShift);
        }
        return newTrks;
    }

    /**
     * Converts a <code>Track</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track.
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(Track track) {
        // Generate and return the billior track.
        return new BilliorTrack(track);
    }

    /**
     * Converts a <code>TrackState</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track state
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(TrackState trackstate) {
        // Generate and return the billior track.
        return new BilliorTrack(trackstate, 0, 0); // track state doesn't store chi^2 info (stored in the Track object)
    }

}
