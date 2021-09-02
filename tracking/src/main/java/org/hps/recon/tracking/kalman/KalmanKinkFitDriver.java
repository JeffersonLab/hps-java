package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class KalmanKinkFitDriver extends Driver {

    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private KalmanInterface KI;
    private KalmanParams kPar;
      
    @Override
    public void detectorChanged(Detector det) {

        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();
        
        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }
        
        // Instantiate the interface to the Kalman-Filter code and set up the geometry
        KalmanParams kPar = new KalmanParams();
        kPar.print();
        
        KI = new KalmanInterface(false, kPar, fm);
        KI.createSiModules(detPlanes);

    }

    @Override
    public void process(EventHeader event) {
                
        String stripDataInputCollectionName = "KFGBLStripClusterData";
        if (!event.hasCollection(GBLStripClusterData.class, stripDataInputCollectionName)) {
            System.out.format("\nKalmanKinkFitDriver: the data collection %s is missing.\n",stripDataInputCollectionName);
        }
        String stripDataRelationsInputCollectionName = "KFGBLStripClusterDataRelations";
        if (!event.hasCollection(LCRelation.class, stripDataRelationsInputCollectionName)) {
            System.out.format("\nKalmanKinkFitDriver: the data collection %s is missing.\n",stripDataRelationsInputCollectionName);
        }
        String trackCollectionName = "KalmanFullTracks";
        if (event.hasCollection(Track.class, trackCollectionName)) {
            List<Track> kalmanFullTracks = event.get(Track.class, trackCollectionName);
            for (Track trk : kalmanFullTracks) {
                if (trk.getNDF() >= 7) {
                    System.out.format("Event %d, Kalman track with %d degrees of freedom\n", event.getEventNumber(), trk.getNDF());
                    KalmanKinkFit knkFt = new KalmanKinkFit(event, KI, trk);
                    if (knkFt.doFits()) {
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
        KI.clearInterface();
    }   
}