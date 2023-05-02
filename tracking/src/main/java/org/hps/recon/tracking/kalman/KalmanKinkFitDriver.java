package org.hps.recon.tracking.kalman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackingReconstructionPlots;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;

/**
 * Driver to study kinks in Kalman tracks, measured by breaking the track into two halves
 *
 */
public class KalmanKinkFitDriver extends Driver {

    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private KalmanInterface KI;
    private KalmanParams kPar;
    private static final boolean debug = false;
    private AIDA aida;
    private IHistogram1D hProjAngBot, hAngBot, hProjAngTop, hAngTop, hChiIn, hChiOut, hDofIn, hDofOut;

    private void setupPlots() {
        if (aida == null)
            aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        hAngBot = aida.histogram1D("Kalman kink angle bottom", 100, 0., 0.15);
        hProjAngBot = aida.histogram1D("Kalman projected kink angle bottom", 100, -0.01, 0.01);
        hAngTop = aida.histogram1D("Kalman kink angle top", 100, 0., 0.15);
        hProjAngTop = aida.histogram1D("Kalman projected kink angle top", 100, -0.01, 0.01);
        hChiIn = aida.histogram1D("Inner helix chi^2",100,0.,100.);
        hDofIn = aida.histogram1D("Inner helix #dof",10,0.,10.);
        hChiOut = aida.histogram1D("Outer helix chi^2",100,0.,100.);
        hDofOut = aida.histogram1D("Outer helix #dof",10,0.,10.);
    }
    
    @Override
    public void detectorChanged(Detector det) {

        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();
        
        setupPlots();
        
        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }
        
        // Instantiate the interface to the Kalman-Filter code and set up the geometry
        KalmanParams kPar = new KalmanParams();
        kPar.print();
        
        KI = new KalmanInterface(kPar, det, fm);
        KI.createSiModules(detPlanes);

    }

    @Override
    public void process(EventHeader event) {
                
        String stripDataRelationsInputCollectionName = "KFGBLStripClusterDataRelations";
        if (!event.hasCollection(LCRelation.class, stripDataRelationsInputCollectionName)) {
            System.out.format("\nKalmanKinkFitDriver: the data collection %s is missing.\n",stripDataRelationsInputCollectionName);
        }
        String trackCollectionName = "KalmanFullTracks";
        if (event.hasCollection(Track.class, trackCollectionName)) {
            List<Track> kalmanFullTracks = event.get(Track.class, trackCollectionName);
            for (Track trk : kalmanFullTracks) {
                if (trk.getNDF() >= 7) {
                    if (debug) System.out.format("Event %d, Kalman track with %d degrees of freedom\n", event.getEventNumber(), trk.getNDF());
                    KalmanKinkFit knkFt = new KalmanKinkFit(event, KI, trk);
                    if (knkFt.doFits()) {
                        if (knkFt.innerMomentum()[2] < 0.) {
                            hProjAngBot.fill(knkFt.projectedAngle());
                            hAngBot.fill(knkFt.scatteringAngle());
                        } else {
                            hProjAngTop.fill(knkFt.projectedAngle());
                            hAngTop.fill(knkFt.scatteringAngle());
                        }
                        hChiIn.fill(knkFt.innerChi2());
                        hDofIn.fill(knkFt.innerDOF());
                        hChiOut.fill(knkFt.outerChi2());
                        hDofOut.fill(knkFt.outerDOF());
                        if (debug) {
                            System.out.format("KinkFit: event number %d\n", event.getEventNumber());
                            System.out.format("  KinkFit: chi^2 of inner helix = %8.4f with %d dof\n", knkFt.innerChi2(), knkFt.innerDOF());
                            System.out.format("  KinkFit: chi^2 of outer helix = %8.4f with %d dof\n", knkFt.outerChi2(), knkFt.outerDOF());
                            Vec inHx = new Vec(5,knkFt.innerHelix());
                            Vec outHx = new Vec(5,knkFt.outerHelix());
                            System.out.format("  KinkFit: inner helix = %s\n", inHx.toString());
                            System.out.format("  KinkFit: outer helix = %s\n", outHx.toString());
                            Vec inP = new Vec(3,knkFt.innerMomentum());
                            Vec outP = new Vec(3,knkFt.outerMomentum());
                            System.out.format("  KinkFit: inner momentum = %s\n", inP.toString());
                            System.out.format("  KinkFit: outer momentum = %s\n", outP.toString());
                            System.out.format("  KinkFit: scattering angle = %10.6f radians\n", knkFt.scatteringAngle());
                            System.out.format("  KinkFit: projected scattering angle = %10.6f radians\n", knkFt.projectedAngle());
                        }
                    }
                }
            }
        }
        KI.clearInterface();
    }   
    
    @Override
    public void endOfData() {
        try {
            System.out.println("Outputting the plots now.");
            aida.saveAs("KalmanKinkFitPlots.root");
        } catch (IOException ex) {
            Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
