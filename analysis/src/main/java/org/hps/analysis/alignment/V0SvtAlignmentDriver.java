package org.hps.analysis.alignment;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.IOException;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.lcsim.geometry.FieldMap;
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

    FieldMap _fieldmap;

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
        Hep3Vector ip = new BasicHep3Vector(0., 0., 0.0);
//        _fieldmap = detector.getFieldMap();
//        double[] pos = new double[3];
//        double[] field = new double[3];
//        for (int i = 0; i < 2000; ++i) {
//            pos[2] = -50. + i;
//            _fieldmap.getField(pos, field);
//            System.out.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+field[0]+" "+field[1]+" "+field[2]);
//            aida.cloud1D("bx").fill(pos[2], field[0]);
//            aida.cloud1D("by").fill(pos[2], field[1]);
//            aida.cloud1D("bz").fill(pos[2], field[2]);
//        }
        bField = detector.getFieldMap().getField(ip).y();
        if (bField < 0) {
            flipSign = -1;
        }
    }

    protected void process(EventHeader event) {

        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
        }

        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        for (ReconstructedParticle rp : rpList) {

            if (!TrackType.isGBL(rp.getType())) {
                continue;
            }
            // require both track and cluster
            if (rp.getClusters().size() != 1) {
                continue;
            }
            if (rp.getTracks().size() != 1) {
                continue;
            }

            Track t = rp.getTracks().get(0);
            Cluster c = rp.getClusters().get(0);

            int tNhits = t.getTrackerHits().size();
            if (tNhits != 6) {
                continue;
            }

            String type = (rp.getParticleIDUsed().getPDG() == 11) ? " electron " : " positron ";
            String side = (c.getPosition()[1] > 0) ? " top " : " bottom ";
            String s = "allTracks " + type + side;
            Hep3Vector pmom = rp.getMomentum();
            double p = pmom.magnitude();
            TrackState ts = t.getTrackStates().get(0);
            double z0 = ts.getZ0();
            double[] refPoint = ts.getReferencePoint();
            double thetaY = Math.asin(pmom.y() / pmom.magnitude());
            aida.histogram1D(s + " z0", 100, -1.5, 1.5).fill(z0);
            aida.histogram1D(s + " thetaY", 100, .015, .065).fill(abs(thetaY));
            aida.profile1D(s + " track thetaY vs z0 profile", 30, 0.02, 0.05).fill(abs(thetaY), z0);
            aida.histogram2D(s + " track thetaY vs z0", 30, .02, 0.05, 100, -1.5, 1.5).fill(abs(thetaY), z0);
            aida.histogram1D(s + " track momentum", 150, 0., 1.5).fill(rp.getMomentum().magnitude());

//            aida.cloud1D(s + " refPoint x").fill(refPoint[0]);
//            aida.cloud1D(s + " refPoint y").fill(refPoint[1]);
//            aida.cloud1D(s + " refPoint z").fill(refPoint[2]);

            if (p > .9 && p < 1.2) {
                aida.histogram1D(s + " high mom track momentum", 150, 0., 1.5).fill(rp.getMomentum().magnitude());
                aida.histogram2D(s + " high mom track thetaY vs z0", 30, .02, 0.05, 100, -1.5, 1.5).fill(abs(thetaY), z0);
                aida.profile1D(s + " high mom track thetaY vs z0 profile", 30, 0.02, 0.05).fill(abs(thetaY), z0);
            }
            if (p > .2 && p < .5) {
                aida.histogram1D(s + " low mom track momentum", 150, 0., 1.5).fill(rp.getMomentum().magnitude());
                aida.histogram2D(s + " low mom track thetaY vs z0", 30, .02, 0.05, 100, -1.5, 1.5).fill(abs(thetaY), z0);
                aida.profile1D(s + " low mom track thetaY vs z0 profile", 30, 0.02, 0.05).fill(abs(thetaY), z0);
            }

            // step in momentum
            int nSteps = 10;
            double pMin = 0.25;
            double dP = 0.1;

            for (int i = 0; i < nSteps; ++i) {
                double pBin = pMin + i * dP;
                BigDecimal bd = new BigDecimal(Double.toString(pBin));
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                double binLabel = bd.doubleValue();

                if (abs(p - pBin) < dP / 2.) {
                    aida.histogram1D(s + binLabel + " track momentum", 150, 0., 1.5).fill(p);
                    aida.histogram2D(s + binLabel + " track thetaY vs z0", 100, .015, 0.065, 100, -1.5, 1.5).fill(abs(thetaY), z0);
                    aida.profile1D(s + binLabel + " track thetaY vs z0 profile", 10, 0.024, 0.054).fill(abs(thetaY), z0);
                }
            }

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
                aida.histogram1D("vertex x position", 100, -3., 3.).fill(vPos.x());
                aida.histogram1D("vertex y position", 100, -2., 2.).fill(vPos.y());
                aida.histogram2D("vertex x vs y position",100, -2., 2., 100, -2., 2.).fill(vPos.x(),vPos.y());
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
    protected void endOfData() {
        try {
            //        System.out.println("end of data");
//        IAnalysisFactory af = IAnalysisFactory.create();
//        IHistogramFactory hf = af.createHistogramFactory(aida.tree());
//        IDataPointSetFactory dpsf = af.createDataPointSetFactory(aida.tree());
//        IFunctionFactory functionFactory = af.createFunctionFactory(aida.tree());
//        IFitFactory fitFactory = af.createFitFactory();
//        IFitter fitter = fitFactory.createFitter("Chi2", "jminuit");
//
//        IHistogram2D bottomHiPPlot = (IHistogram2D) aida.tree().find("allTracks  electron  bottom  high mom track thetaY vs z0");
//        IHistogram2D bottomLoPPlot = (IHistogram2D) aida.tree().find("allTracks  electron  bottom  low mom track thetaY vs z0");
//        IHistogram2D topHiPPlot = (IHistogram2D) aida.tree().find("allTracks  electron  top  high mom track thetaY vs z0");
//        IHistogram2D topLoPPlot = (IHistogram2D) aida.tree().find("allTracks  electron  top  low mom track thetaY vs z0");
//
//        IDataPointSet bottomHiPdataPointSet = dpsf.create("dataPointSet", "bottom high mom", 2);
//        IDataPointSet bottomLoPdataPointSet = dpsf.create("dataPointSet", "bottom low mom", 2);
//        IDataPointSet topHiPdataPointSet = dpsf.create("dataPointSet", "top high mom", 2);
//        IDataPointSet topLoPdataPointSet = dpsf.create("dataPointSet", "top low mom", 2);
//
//        sliceAndFit(bottomHiPPlot, bottomHiPdataPointSet, hf);
//        sliceAndFit(bottomLoPPlot, bottomLoPdataPointSet, hf);
//        sliceAndFit(topHiPPlot, topHiPdataPointSet, hf);
//        sliceAndFit(topLoPPlot, topLoPdataPointSet, hf);
//
////        System.out.println("successfully fit "+nDataPoints+" slices");
////        IPlotter plotter = af.createPlotterFactory().create("slice gaussian fits");
////        plotter.setParameter("plotterWidth", "1600");
////        plotter.setParameter("plotterHeight", "900");
////        plotter.region(0).plot(bottomHiPdataPointSet);
////
////        IFunction line = functionFactory.createFunctionByName("line", "p1");
////
////        IFitResult bottomHiPLineFitresult = fitter.fit(bottomHiPdataPointSet, line);
////        plotter.region(0).plot(bottomHiPLineFitresult.fittedFunction());
////
////        plotter.show();
////
////        System.out.println("found " + _topElectrons.size() + " top electrons");
////        System.out.println("found " + _topPositrons.size() + " top positrons");
////        System.out.println("found " + _bottomElectrons.size() + " bottom electrons");
////        System.out.println("found " + _bottomPositrons.size() + " bottom positrons");
////
////        //vertex top positrons and electrons
////        int nTop = min(_topElectrons.size(), _topPositrons.size());
////        for (int i = 0; i < nTop; ++i) {
////            BilliorVertex vtx = fitVertex(_topElectrons.get(i), _topPositrons.get(i));
////            aida.histogram1D("top vertex x position").fill(vtx.getPosition().x());
////            aida.histogram1D("top vertex y position").fill(vtx.getPosition().y());
////            aida.histogram1D("top vertex z position").fill(vtx.getPosition().z());
////        }
////        //vertex bottom electrons and positrons
////        int nBottom = min(_bottomElectrons.size(), _bottomPositrons.size());
////        for (int i = 0; i < nBottom; ++i) {
////            BilliorVertex vtx = fitVertex(_bottomElectrons.get(i), _bottomPositrons.get(i));
////            aida.histogram1D("bottom vertex x position").fill(vtx.getPosition().x());
////            aida.histogram1D("bottom vertex y position").fill(vtx.getPosition().y());
////            aida.histogram1D("bottom vertex z position").fill(vtx.getPosition().z());
////        }
////
            aida.saveAs("V0SvtAlignmentDriverPlots_" + myDate() + ".root");
            aida.saveAs("V0SvtAlignmentDriverPlots_" + myDate() + ".aida");
        } catch (IOException ex) {
            Logger.getLogger(V0SvtAlignmentDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static private String myDate() {
        Calendar cal = new GregorianCalendar();
        Date date = new Date();
        cal.setTime(date);
        DecimalFormat formatter = new DecimalFormat("00");
        String day = formatter.format(cal.get(Calendar.DAY_OF_MONTH));
        String month = formatter.format(cal.get(Calendar.MONTH) + 1);
        return cal.get(Calendar.YEAR) + month + day;
    }
    //
    //    private void sliceAndFit(IHistogram2D hist2D, IDataPointSet dataPointSet, IHistogramFactory hf) {
    //        IAxis xAxis = hist2D.xAxis();
    //        int nBins = xAxis.bins();
    //        IHistogram1D[] bottomSlices = new IHistogram1D[nBins];
    //        IDataPoint dp;
    //        int nDataPoints = 0;
    //        for (int i = 0; i < nBins; ++i) { // stepping through x axis bins
    //            bottomSlices[i] = hf.sliceY("/bottom slice " + i, hist2D, i);
    //            System.out.println("bottom slice " + i + " has " + bottomSlices[i].allEntries() + " entries");
    //            if (bottomSlices[i].entries() > 100.) {
    //                IFitResult fr = performGaussianFit(bottomSlices[i]);
    //                System.out.println(" fit status: " + fr.fitStatus());
    //                double[] frPars = fr.fittedParameters();
    //                double[] frParErrors = fr.errors();
    //                String[] frParNames = fr.fittedParameterNames();
    //                System.out.println(" Energy Resolution Fit: ");
    //                for (int jj = 0; jj < frPars.length; ++jj) {
    //                    System.out.println(frParNames[jj] + " : " + frPars[jj] + " +/- " + frParErrors[jj]);
    //                }
    //                // create a datapoint
    //                dataPointSet.addPoint();
    //                dp = dataPointSet.point(nDataPoints++);
    //                dp.coordinate(0).setValue(xAxis.binCenter(i));
    //                dp.coordinate(1).setValue(frPars[1]); // gaussian mean
    //                dp.coordinate(1).setErrorPlus(frParErrors[1]);
    //                dp.coordinate(1).setErrorMinus(frParErrors[1]);
    //            }
    //        }
    //        try {
    //            aida.tree().commit();
    //        } catch (IOException ex) {
    //            Logger.getLogger(V0SvtAlignmentDriver.class.getName()).log(Level.SEVERE, null, ex);
    //        }
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

    private IFitResult performGaussianFit(IHistogram1D histogram) {
        IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
        IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
        IFunction function = functionFactory.createFunctionByName("Gaussian Fit", "G");
        IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
        double[] parameters = new double[3];
        parameters[0] = histogram.maxBinHeight();
        parameters[1] = histogram.mean();
        parameters[2] = 0.1; // histogram.rms();// why is the rms of a slice wrong?
        function.setParameters(parameters);
        IFitResult fitResult = null;
        Logger minuitLogger = Logger.getLogger("org.freehep.math.minuit");
        minuitLogger.setLevel(Level.OFF);
        minuitLogger.info("minuit logger test");

        try {
            fitResult = fitter.fit(histogram, function);
        } catch (RuntimeException e) {
            System.out.println("fit failed.");
        }
        return fitResult;
    }

}
