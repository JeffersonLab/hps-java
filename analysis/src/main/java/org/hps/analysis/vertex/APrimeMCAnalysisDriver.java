package org.hps.analysis.vertex;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author Norman A. Graf
 */
public class APrimeMCAnalysisDriver extends Driver {


    String outputFile = "APrimeMCAnalysisDriver_" + myDate() + ".aida";

    String vertexCollectionName = "UnconstrainedV0Vertices";
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[4];
    double[] p2 = new double[4];
    double emass = 0.000511;
    double mass2 = emass * emass;

    double _bfield;

    private static final RotationPassiveXYZ rotMat = new RotationPassiveXYZ(0., 0.01525, 0.);
    private static final Translation3D transMat = new Translation3D(0., 0., -0.5);
    private static final Transform3D xformMat = new Transform3D(transMat, rotMat);

    boolean debug = true;

    private AIDA aida = AIDA.defaultInstance();

    private IHistogram1D vtx_xMC = aida.histogram1D("vertex x MC", 100, -10., 10.);
    private IHistogram1D vtx_yMC = aida.histogram1D("vertex y MC", 100, -10., 10.);
    private IHistogram1D vtx_zMC = aida.histogram1D("vertex z MC", 200, -100., 200.);
    private IHistogram1D vtx_x = aida.histogram1D("vertex x", 100, -10., 10.);
    private IHistogram1D vtx_y = aida.histogram1D("vertex y", 100, -10., 10.);
    private IHistogram1D vtx_z = aida.histogram1D("vertex z", 200, -100., 200.);
    private IHistogram1D vtx_xRes = aida.histogram1D("vertex x residual", 100, -10., 10.);
    private IHistogram1D vtx_yRes = aida.histogram1D("vertex y residual", 100, -10., 10.);
    private IHistogram1D vtx_zRes = aida.histogram1D("vertex z residual", 200, -100., 100.);
    private IHistogram1D vtx_xPull = aida.histogram1D("vertex x pull", 100, -5., 5.);
    private IHistogram1D vtx_yPull = aida.histogram1D("vertex y pull", 100, -5., 5.);
    private IHistogram1D vtx_zPull = aida.histogram1D("vertex z pull", 200, -5., 5.);

    private IHistogram1D vtxrf_zRes = aida.histogram1D("vertex refit z residual", 200, -100., 100.);
    private IHistogram1D vtxrf_mRes = aida.histogram1D("vertex refit mass residual", 100, -0.05, 0.05);
    private IHistogram1D vtxrf_corrmRes = aida.histogram1D("vertex refit corrected mass residual", 100, -0.05, 0.05);
    private IHistogram2D vtxcorrMassResvsMCz = aida.histogram2D("vertex refit corrected mass - MC mass vs MC z", 200, -100., 200., 100, -0.05, 0.05);

    private IHistogram1D vtxMass = aida.histogram1D("vertex mass", 100, 0., 0.1);
    private IHistogram1D vtxMassRes = aida.histogram1D("vertex fitted mass - MC mass", 100, -0.05, 0.05);
    private IHistogram2D vtxMassResvsMCz = aida.histogram2D("vertex fitted mass - MC mass vs MC z", 200, -100., 200., 100, -0.05, 0.05);

    private IHistogram2D vtx_zvsMCz = aida.histogram2D("vertex z vs MCz", 200, -100., 200., 200, -100., 200.);

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        _bfield = TrackUtils.getBField(detector).magnitude();
    }

    protected void process(EventHeader event) {
        List<MCParticle> mcparts = event.getMCParticles();
        MCParticle aprime = null;
        MCParticle recoilMC = mcparts.get(0);
        MCParticle positronMC = mcparts.get(1);
        MCParticle electronMC = mcparts.get(2);
        if (debug) {
            System.out.println("recoil " + recoilMC);
            System.out.println("electron " + electronMC);
            System.out.println("positron " + positronMC);

        }

        Hep3Vector ip = recoilMC.getOrigin();
        Hep3Vector aprimeVtx = electronMC.getOrigin();
        Lorentz4Vector l4v = lorentzVectorSum(electronMC, positronMC);
        double aprimeMass = l4v.mass();
        System.out.println("aprime 4vec: " + l4v);
        System.out.println("aprime vertex: " + aprimeVtx);

        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        aida.cloud1D("number of Vertices)").fill(vertices.size());
        if (vertices.size() == 2) {
            for (Vertex v : vertices) {
                aida.tree().cd("/");

                ReconstructedParticle rp = v.getAssociatedParticle();
                int type = rp.getType();
                if (TrackType.isGBL(type)) {
                    SymmetricMatrix cov = v.getCovMatrix();
                    Hep3Vector pos = v.getPosition();

                    // try some transforms here...
//            rotMat.rotate(cov);
//            rotMat.rotate(pos);
//            xformMat.transform(aprimeVtx);
                    if (debug) {
                        System.out.println("cov matrix: \n" + cov);
                        System.out.println("vertex pos: \n" + pos);
                    }
                    Map<String, Double> vals = v.getParameters();
                    //System.out.println(vals);
                    p1[0] = vals.get("p1X");
                    p1[1] = vals.get("p1Y");
                    p1[2] = vals.get("p1Z");
                    p2[0] = vals.get("p2X");
                    p2[1] = vals.get("p2Y");
                    p2[2] = vals.get("p2Z");
                    double e1 = 0;
                    double e2 = 0.;
                    for (int i = 0; i < 3; ++i) {
                        e1 += p1[i] * p1[i];
                        e2 += p2[i] * p2[i];
                    }
                    e1 = sqrt(e1 + mass2);
                    e2 = sqrt(e2 + mass2);
                    Momentum4Vector vec1 = new Momentum4Vector(p1[0], p1[1], p1[2], e1);
                    Momentum4Vector vec2 = new Momentum4Vector(p2[0], p2[1], p2[2], e2);
                    Lorentz4Vector sum = vec1.plus(vec2);
                    double mass = sum.mass();
                    double invMass = vals.get("invMass");
                    if (debug) {
                        System.out.println("mass: " + mass + " invMass: " + invMass + " delta: " + (invMass - mass));
                    }

                    analyzeVertex(v, electronMC, positronMC);

                    aida.cloud1D("type").fill(type);
                    vtx_x.fill(pos.x());
                    aida.cloud2D("vertex x vs z").fill(pos.z(), pos.x());

                    vtx_xMC.fill(aprimeVtx.x());
                    aida.cloud2D("vertex MC x vs MC z").fill(aprimeVtx.z(), aprimeVtx.x());
                    aida.cloud2D("vertex MC x vs MC y").fill(aprimeVtx.x(), aprimeVtx.y());
                    vtx_xRes.fill(pos.x() - aprimeVtx.x());
                    double xPull = (pos.x() - aprimeVtx.x()) / sqrt(cov.diagonal(0));
                    vtx_xPull.fill(xPull);

                    vtx_y.fill(pos.y());
                    aida.cloud2D("vertex y vs z").fill(pos.z(), pos.y());
                    vtx_yMC.fill(aprimeVtx.y());
                    aida.cloud2D("vertex MC y vs MC z").fill(aprimeVtx.z(), aprimeVtx.y());
                    vtx_yRes.fill(pos.y() - aprimeVtx.y());
                    double yPull = (pos.y() - aprimeVtx.y()) / sqrt(cov.diagonal(1));

                    vtx_yPull.fill(yPull);

                    vtx_z.fill(pos.z());
                    vtx_zMC.fill(aprimeVtx.z());
                    vtx_zRes.fill(pos.z() - aprimeVtx.z());
                    double zPull = (pos.z() - aprimeVtx.z()) / sqrt(cov.diagonal(2));
                    if (abs(zPull) < 10.) {
                        vtx_zPull.fill(zPull);
                        aida.cloud2D("vertex z pull vs MC z").fill(zPull, aprimeVtx.z());
                        aida.cloud1D("vertex z significance pull lt 10").fill(pos.z() / zPull);
                    }

                    vtx_zvsMCz.fill(aprimeVtx.z(), pos.z());

                    aida.cloud1D("vertex chisq").fill(v.getChi2());
                    aida.cloud1D("vertex chi2 prob").fill(v.getProbability());
                    vtxMass.fill(rp.getMass());
                    vtxMassRes.fill(rp.getMass() - aprimeMass);
                    vtxMassResvsMCz.fill(aprimeVtx.z(), rp.getMass() - aprimeMass);
                    List<ReconstructedParticle> parts = rp.getParticles();
                    ReconstructedParticle electron = null;
                    ReconstructedParticle positron = null;
                    for (ReconstructedParticle part : parts) {
                        if (part.getCharge() < 0) {
                            electron = part;
                        }
                        if (part.getCharge() > 0) {
                            positron = part;
                        }
                    }
                    analyzeParticle("electron", electron);
                    analyzeParticle("positron", positron);

                    BilliorVertex bvRefit = reVertex(v);
                    System.out.println("aprime vertex: " + aprimeVtx);
                    System.out.println("vertex pos: " + pos);
                    System.out.println("bvRefit: " + bvRefit);

                    vtxrf_zRes.fill(bvRefit.getPosition().z() - aprimeVtx.z());
                    // check mass correction here...
                    // corrM = events["uncM"]-0.15e-3*(events["elePX"]/events["eleP"]-events["posPX"]/events["posP"])*events["uncVZ"]/events["uncM"]
                    double uncM = bvRefit.getParameters().get("invMass");
                    double uncVZ = bvRefit.getPosition().z();
                    double elePX = bvRefit.getParameters().get("p1Y");
                    double elePY = bvRefit.getParameters().get("p1Z");
                    double elePZ = bvRefit.getParameters().get("p1X");
                    double posPX = bvRefit.getParameters().get("p2Y");
                    double posPY = bvRefit.getParameters().get("p2Z");
                    double posPZ = bvRefit.getParameters().get("p2X");
                    double eleP = sqrt(elePX * elePX + elePY * elePY + elePZ * elePZ);
                    double posP = sqrt(posPX * posPX + posPY * posPY + posPZ * posPZ);
                    double corrM = uncM - 0.15e-3 * (elePX / eleP - posPX / posP) * uncVZ / uncM;
                    System.out.println("uncM " + uncM + " corrM " + corrM);
                    vtxrf_mRes.fill(uncM - aprimeMass);
                    vtxrf_corrmRes.fill(corrM - aprimeMass);
                    vtxcorrMassResvsMCz.fill(aprimeVtx.z(), corrM - aprimeMass);
                    aida.tree().cd("/");
                }// only analyze GBL tracks
            }
        }//only analyze events with one vertex (2 in collection because of multiple track collections
    }

    private void analyzeParticle(String dir, ReconstructedParticle p) {
        aida.tree().mkdirs(dir);
        aida.tree().cd(dir);
        //
        Track t = p.getTracks().get(0);
        double chiSquared = t.getChi2();
        int ndf = t.getNDF();
        double chisqProb = ChisqProb.gammp(ndf, chiSquared);
        aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
        aida.cloud1D("Track chisq prob").fill(chisqProb);
        aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());
        aida.tree().cd("..");
    }

    private BilliorVertex reVertex(Vertex v0) {
        double zVtx = v0.getPosition().z();
        ReconstructedParticle rp = v0.getAssociatedParticle();
        List<ReconstructedParticle> parts = rp.getParticles();
        ReconstructedParticle electron = null;
        ReconstructedParticle positron = null;
        for (ReconstructedParticle part : parts) {
            if (part.getCharge() < 0) {
                electron = part;
            }
            if (part.getCharge() > 0) {
                positron = part;
            }
        }
        Track eletrk = electron.getTracks().get(0);
        Track postrk = positron.getTracks().get(0);
//        //propagate track to the vertex position...
//        Hep3Vector elexcpt = TrackUtils.extrapolateTrack(eletrk,zVtx);
//        Hep3Vector posxcpt = TrackUtils.extrapolateTrack(postrk,zVtx);
        // first let's see if we can recreate this vertex...
        SeedTrack stEle1 = TrackUtils.makeSeedTrackFromBaseTrack(eletrk);
        SeedTrack stEle2 = TrackUtils.makeSeedTrackFromBaseTrack(postrk);
        BilliorTrack eleBilliorTrack = new BilliorTrack(stEle1.getSeedCandidate().getHelix());
        BilliorTrack posBilliorTrack = new BilliorTrack(stEle2.getSeedCandidate().getHelix());
        BilliorVertex bv = fitVertex(eleBilliorTrack, posBilliorTrack, _bfield);
        double invMass = bv.getParameters().get("invMass");
        Hep3Vector vtxpos = bv.getPosition();
        aida.cloud1D("vtx x diff").fill(vtxpos.x() - v0.getPosition().x());
        aida.cloud1D("vtx y diff").fill(vtxpos.y() - v0.getPosition().y());
        aida.cloud1D("vtx z diff").fill(vtxpos.z() - v0.getPosition().z());
        System.out.println(bv);
        // this looks good.
        // quick check of track states...
        List<TrackState> eleTrackStates = eletrk.getTrackStates();
        System.out.println("electron track states:");
        for (TrackState ts : eleTrackStates) {
            System.out.println(ts);
        }
        System.out.println("positron track states:");
        List<TrackState> posTrackStates = postrk.getTrackStates();
        for (TrackState ts : posTrackStates) {
            System.out.println(ts);
        }
        // now, transport track state to vtx position, revertex...
        // TODO transport covariance matrix as well...
        // TODO determine whether to iterate
        TrackState eleTrackState = eletrk.getTrackStates().get(0);
        TrackState posTrackState = postrk.getTrackStates().get(0);
        System.out.println("eleTrackState: " + eleTrackState);
        System.out.println("posTrackState: " + posTrackState);
        double[] eleParams = eleTrackState.getParameters();
        double[] eleCovMat = eleTrackState.getCovMatrix();
        double[] eleParamsXtrap = TrackUtils.getParametersAtNewRefPoint(vtxpos.v(), eleTrackState.getReferencePoint(), eleParams);
        double[] posParams = posTrackState.getParameters();
        double[] posCovMat = posTrackState.getCovMatrix();
        double[] posParamsXtrap = TrackUtils.getParametersAtNewRefPoint(vtxpos.v(), posTrackState.getReferencePoint(), posParams);
        System.out.println("ele before: " + Arrays.toString(eleParams));
        System.out.println("ele after: " + Arrays.toString(eleParamsXtrap));
        System.out.println("pos before: " + Arrays.toString(posParams));
        System.out.println("pos after: " + Arrays.toString(posParamsXtrap));

        // convert new parameters to a trackstate
        TrackState eleTsAtVtx = new BaseTrackState(eleParamsXtrap, eleCovMat, vtxpos.v(), 5);
        TrackState posTsAtVtx = new BaseTrackState(posParamsXtrap, posCovMat, vtxpos.v(), 5);

        // create BilliorTracks from trackstate constructor
        BilliorTrack eleBtAtVtx = new BilliorTrack(eleTsAtVtx, eletrk.getChi2(), eletrk.getNDF());
        BilliorTrack posBtAtVtx = new BilliorTrack(posTsAtVtx, postrk.getChi2(), postrk.getNDF());
        // revertex
        BilliorVertex bvRefit = fitVertex(eleBtAtVtx, posBtAtVtx, _bfield);

        return bvRefit;
    }

    private BilliorVertex fitVertex(BilliorTrack electron, BilliorTrack positron, double bField) {
        // Create a vertex fitter from the magnetic field.
        double[] beamSize = {0.001, 0.2, 0.02};
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);

        // Perform the vertexing based on the specified constraint.
        vtxFitter.doBeamSpotConstraint(false);

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();

        billiorTracks.add(electron);

        billiorTracks.add(positron);

        // Find and return a vertex based on the tracks.
        return vtxFitter.fitVertex(billiorTracks);
    }

    @Override
    protected void endOfData() {
        try {
            aida.saveAs(outputFile);
        } catch (IOException ex) {
            ex.printStackTrace();
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

    private Lorentz4Vector lorentzVectorSum(MCParticle p1, MCParticle p2) {
        Momentum4Vector vec1 = new Momentum4Vector(p1.getPX(), p1.getPY(), p1.getPZ(), p1.getEnergy());
        Momentum4Vector vec2 = new Momentum4Vector(p2.getPX(), p2.getPY(), p2.getPZ(), p2.getEnergy());
        return vec1.plus(vec2);
    }

    private void analyzeVertex(Vertex v, MCParticle e, MCParticle p) {
        double[] ep = new double[4];
        double[] pp = new double[4];
        Map<String, Double> vals = v.getParameters();
        //System.out.println(vals);
        ep[0] = vals.get("p1X");
        ep[1] = vals.get("p1Y");
        ep[2] = vals.get("p1Z");
        pp[0] = vals.get("p2X");
        pp[1] = vals.get("p2Y");
        pp[2] = vals.get("p2Z");
        Hep3Vector epmc = e.getMomentum();
        Hep3Vector ppmc = p.getMomentum();
        System.out.println("ep: "+Arrays.toString(ep));
        System.out.println("pp: "+Arrays.toString(pp));
        System.out.println("epmc: "+epmc);
        System.out.println("ppmc: "+ppmc);

    }
}
