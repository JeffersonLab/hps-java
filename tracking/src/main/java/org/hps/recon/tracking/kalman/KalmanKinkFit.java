package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.apache.commons.math3.util.Pair;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.RelationalTable;
import java.util.Set;
import org.lcsim.event.GenericObject;


// Break a Kalman track into two halves and fit them separately.
// Find the kink angle between the two halves
public class KalmanKinkFit {
    private KalmanTrackFit2 innerTrack, outerTrack;
    private Vec innerP, outerP;
    private double angle;
    private double projAngle;
    private static final boolean debug = false;
    private EventHeader event;
    private KalmanInterface KI;
    private Track KalmanFullTrack;
    private double innerStereo, outerStereo;
    private int innerNhits, outerNhits;
    
    KalmanKinkFit(EventHeader event, KalmanInterface KI, Track KalmanFullTrack) {
        this.event = event;
        this.KI = KI;
        this.KalmanFullTrack = KalmanFullTrack;
    }
    
    public boolean doFits() {
        ArrayList<SiModule>  siMlist = KI.getSiModuleList();
        if (siMlist.size() < 1) {
            System.out.format("KalmanKinkFit: the SiModule instances have not yet been created in KalmanInterface.");
            return false;
        }
        String stripDataRelationsInputCollectionName = "KFGBLStripClusterDataRelations";
        if (!event.hasCollection(LCRelation.class, stripDataRelationsInputCollectionName)) {
            System.out.format("\nKalmanKinkFit: the data collection %s is missing. Abort.\n",stripDataRelationsInputCollectionName);
            return false;
        }
        TrackState stateIP = null;
        for (TrackState state : KalmanFullTrack.getTrackStates()) {
            if (state.getLocation() == TrackState.AtFirstHit) {
                stateIP = state;
                break;
            }
        }
        if (stateIP == null) {
            System.out.format("KalmanKinkFit event %d, trackstate at first hit is missing\n", event.getEventNumber());
            return false;
        }
        
        RelationalTable kfSCDsRT = null;
        List<LCRelation> kfSCDRelation = new ArrayList<LCRelation>();
        if (event.hasCollection(LCRelation.class, stripDataRelationsInputCollectionName)) { 
            kfSCDsRT = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            kfSCDRelation = event.get(LCRelation.class,stripDataRelationsInputCollectionName);
            for (LCRelation relation : kfSCDRelation) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) { 
                    kfSCDsRT.add(relation.getFrom(), relation.getTo());
                }
            }
        } else {
            System.out.println("null KFGBLStripCluster Data Relations.");
            return false; 
        }
        
        //Get the strip cluster data
        Set<GenericObject> kfSCDs = kfSCDsRT.allFrom(KalmanFullTrack);
        
        //Convert the set to a list for sorting it
        List<GenericObject> stripsOnTrack = new ArrayList<GenericObject>(kfSCDs);        
        
        if (debug) System.out.format("Event %d, Track chi^2=%9.3f, ndof=%d \n", event.getEventNumber(), KalmanFullTrack.getChi2(), KalmanFullTrack.getNDF());
        ArrayList<SiModule> innerList = new ArrayList<SiModule>(7);
        ArrayList<Integer> innerHits = new ArrayList<Integer>(7);
        ArrayList<SiModule> outerList = new ArrayList<SiModule>(7);
        ArrayList<Integer> outerHits = new ArrayList<Integer>(7);
        innerStereo = 0;
        outerStereo = 0;
        int lyrBreak = 7;

        for (GenericObject strpClrst_go : stripsOnTrack) {
            GBLStripClusterData strpClrst = new GBLStripClusterData(strpClrst_go);
            int ID = strpClrst.getId();
            int vol = strpClrst.getVolume();
            // Hack for the volume (upper or lower tracker) since it wasn't filled in the data tapes
            if (stateIP.getTanLambda() > 0.) vol = 1;
            else vol = 0;
            double v = strpClrst.getMeas();
            if (v < -990.) continue;
            double e = strpClrst.getMeasErr();
            Pair<Integer, Integer> IDdecode = TrackUtils.getLayerSide(vol, ID);
            if (debug) System.out.format("Strip cluster data ID = %d, vol=%d, v=%9.5f += %9.5f, layer=%d module=%d\n", 
                    ID, vol, v, e, IDdecode.getFirst()-1, IDdecode.getSecond());
            //KI.printGBLStripClusterData(strpClrst);
            for (SiModule mod : KI.getSiModuleList()) {
                if (mod.millipedeID == ID && mod.topBottom == vol) {
                    if (mod.Layer != IDdecode.getFirst()-1 || mod.detector != IDdecode.getSecond()) {
                        System.out.format("KalmanKinkFit event %d, layer or module mismatch for millipedeID=%d\n", event.getEventNumber(), ID);
                        System.out.format("      SiModule: layer = %d, detector = %d\n", mod.Layer, mod.detector);
                        System.out.format("      From ID:  layer = %d, detector = %d\n", IDdecode.getFirst()-1, IDdecode.getSecond());
                    }
                    // If hits have already been filled into the SiModule, look for the correct one
                    int iHit = -1;
                    if (mod.hits.size() > 0) {
                        for (Measurement m : mod.hits) {
                            //if (debug) System.out.format("   ID %d Vol %d, Comparing hit v=%10.6f with input v=%10.6f\n", ID, vol, m.v, v);
                            if (Math.abs(m.v - v) < 0.0001) {
                                iHit = mod.hits.indexOf(m);
                                break;
                            }
                        }
                        if (iHit < 0) {
                            //if (debug) {
                            //    System.out.format("KalmanKinkFit event %d, cannot find the hit for ID=%d Vol=%d\n",event.getEventNumber(), ID, vol);
                            //    for (Measurement m : mod.hits) {
                            //        System.out.format("   Layer %d, detector %d, hit v=%10.6f\n", mod.Layer, mod.detector, m.v);
                            //    }
                            //}
                        } else {
                            if (mod.Layer <= lyrBreak) {
                                innerList.add(mod);
                                innerHits.add(iHit);
                                if (mod.isStereo) innerStereo++;
                            } else {
                                outerList.add(mod);
                                outerHits.add(iHit);
                                if (mod.isStereo) outerStereo++;
                            }
                            //if (debug) System.out.format("KalmanKinkFit event %d, found the hit for ID=%d Vol=%d\n",event.getEventNumber(), ID, vol);
                        }
                    } 
                    if (iHit < 0) {
                        double xStrip = (mod.xExtent[0] + mod.xExtent[1])/2.;
                        Measurement m = new Measurement(strpClrst.getMeas(), xStrip, strpClrst.getMeasErr(), 0., 0.);
                        mod.hits.add(m);
                        iHit = mod.hits.indexOf(m);
                        if (mod.Layer <= lyrBreak) {
                            innerList.add(mod);
                            innerHits.add(iHit);
                            if (mod.isStereo) innerStereo++;
                        } else {
                            outerList.add(mod);
                            outerHits.add(iHit);
                            if (mod.isStereo) outerStereo++;
                        }
                    }
                    break;
                }
            }
        }
        if (debug) System.out.format("    %d/%d inner hits/stereo, %d/%d outer hits/stereo\n", 
                innerList.size(), innerStereo, outerList.size(), outerStereo);
        innerNhits = innerList.size();
        outerNhits = outerList.size();
        if (innerNhits >= 5 && outerNhits >= 5) {
            if (innerStereo >= 3 && outerStereo >= 3) {
                Vec pivot = new Vec(0.,0.,0.);
                double [] helixHPS = stateIP.getParameters();
                Vec helixKal = new Vec(5,KalmanInterface.unGetLCSimParams(helixHPS, KI.alphaCenter));
                if (debug) helixKal.print("starting guess");
                double [] covHPS = stateIP.getCovMatrix();
                DMatrixRMaj covKal = new DMatrixRMaj(KalmanInterface.ungetLCSimCov(covHPS, KI.alphaCenter));
                CommonOps_DDRM.scale(100., covKal);
                innerTrack = new KalmanTrackFit2(event.getEventNumber(), innerList, innerHits, 0, 2, pivot, helixKal, covKal, KI.kPar, KI.fM);
                outerTrack = new KalmanTrackFit2(event.getEventNumber(), outerList, outerHits, 0, 2, pivot, helixKal, covKal, KI.kPar, KI.fM);
                if (innerTrack.success && outerTrack.success) {
                    MeasurementSite siteInner = innerTrack.sites.get(innerTrack.finalSite);
                    HelixState helixInner = siteInner.aS.helix;
                    if (debug) {
                        helixInner.a.print("Inner helix parameters");
                        helixInner.X0.print("Inner helix pivot point");
                        helixInner.origin.print("Inner helix origin");
                    }
                    MeasurementSite siteOuter = outerTrack.sites.get(outerTrack.initialSite);
                    HelixState helixOuter = siteOuter.aS.helix;
                    if (debug) {
                        helixOuter.a.print("Outer helix parameters");
                        helixOuter.X0.print("Outer helix pivot point");
                        helixOuter.origin.print("Outer helix origin");
                    }
                    double yAvg = (siteInner.m.p.X().v[1] + siteOuter.m.p.X().v[1])/2.;
                    Plane pln = new Plane(new Vec(0.,yAvg,0.), new Vec(0.,1.,0.));
                    if (debug) pln.print("midway between helices");
                    double innerPhi = helixInner.planeIntersect(pln);
                    if (Double.isNaN(innerPhi)) {
                        System.out.format("KalmanKinkFit event %d, inner helix does not intersect plane!\n", event.getEventNumber());
                        return false;
                    }
                    double outerPhi = helixOuter.planeIntersect(pln);
                    if (Double.isNaN(outerPhi)) {
                        System.out.format("KalmanKinkFit event %d, outer helix does not intersect plane!\n", event.getEventNumber());
                        return false;
                    }
                    if (debug) System.out.format("   Extrapolation angles to plane: %10.6f %10.6f radians\n", innerPhi, outerPhi);
                    innerP = helixInner.Rot.inverseRotate(helixInner.getMom(innerPhi));
                    if (debug) innerP.print("inner helix momentum");
                    outerP = helixOuter.Rot.inverseRotate(helixOuter.getMom(outerPhi));
                    if (debug) outerP.print("outer helix momentum");
                    return true;
                }
            }         
        }
        return false;
    }
    public double innerChi2() {
        if (innerTrack == null) return 999.;
        return innerTrack.chi2s;
    }
    public int innerDOF() {
        if (innerTrack == null) return -1;
        return innerNhits-5;
    }
    public double outerChi2() {
        if (outerTrack == null) return 999.;
        return outerTrack.chi2s;
    }
    public int outerDOF() {
        if (outerTrack == null) return -1;
        return outerNhits-5;
    }
    public double [] innerHelix() {  // returns a helix with pivot at the origin, in HPS format
        if (innerTrack == null) {
            double [] nothing = {0.,0.,0.,0.,0.};
            return nothing;
        }
        MeasurementSite site = innerTrack.sites.get(innerTrack.initialSite);
        return KalmanInterface.toHPShelix(site.aS.helix, site.m.p, KI.alphaCenter, null, null);
    }
    public double [] outerHelix() {   // returns a helix with pivot at the origin, in HPS format
        if (outerTrack == null) {
            double [] nothing = {0.,0.,0.,0.,0.};
            return nothing;
        }
        MeasurementSite site = outerTrack.sites.get(outerTrack.initialSite);
        return KalmanInterface.toHPShelix(site.aS.helix, site.m.p, KI.alphaCenter, null, null);
    }
    public double [] innerMomentum() {
        if (innerP == null) {
            double [] nothing = {0.,0.,0.};
            return nothing;
        }
        return innerP.v;
    }
    public double [] outerMomentum() {
        if (outerP == null) {
            double [] nothing = {0.,0.,0.};
            return nothing;
        }
        return outerP.v;
    }
    public double scatteringAngle() {
        if (innerP == null || outerP == null) return 999.;
        if (innerP.mag() == 0. || outerP.mag() == 0.) return 999.;
        angle = Math.acos(innerP.dot(outerP)/innerP.mag()/outerP.mag());
        if (debug) System.out.format("Angle between momentum vectors = %10.6f radians\n", angle);
        return angle;
    }
    public double projectedAngle() {
        if (innerP == null || outerP == null) return 999.;
        if (innerP.mag() == 0. || outerP.mag() == 0.) return 999.;
        double lambdaInner = Math.atan(innerP.v[2]/innerP.mag());
        double lambdaOuter = Math.atan(outerP.v[2]/outerP.mag());
        projAngle = lambdaOuter-lambdaInner;
        if (debug) System.out.format("KalmanKinkFit: Inner lambda=%10.6f, Outer lambda=%10.6f, difference=%10.6f radians\n", lambdaInner, lambdaOuter, projAngle);
        return projAngle;    
    }
}
