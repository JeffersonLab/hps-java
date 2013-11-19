package org.lcsim.hps.monitoring.svt;

import hep.aida.*;
import hep.physics.vec.Hep3Vector;
import java.util.List;
import org.lcsim.detector.tracker.silicon.*;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTEventDisplay extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    //    String eCalClusterCollectionName = "EcalClusters";
    private int eventCount;
    private IPlotter plotter;
//    private ICloud2D cl2D;
    private IHistogram2D svtDispZX;
    private IHistogram2D svtDispZY;
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    double zEcal = 130;

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        if (detector.getSubdetector(ecalSubdetectorName) == null) {
            throw new RuntimeException("There is no subdetector called " + ecalSubdetectorName + " in this detector");
        }

        IAnalysisFactory fac = aida.analysisFactory();

        plotter = fac.createPlotterFactory().create("HPS SVT Event Display");
        IPlotterStyle style = plotter.style();
        style.statisticsBoxStyle().setVisible(false);
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
//        style.zAxisStyle().setParameter("scale", "log");

        plotter.createRegions(1, 2);
        svtDispZY = aida.histogram2D("SVT Raw Hits:  z vs y", 50, 0, 140, 50, -10, 10);
        svtDispZX = aida.histogram2D("SVT Raw Hits:  z vs x", 50, 0, 140, 100, -35, 35);
//        cl2D = aida.cloud2D("SVT Raw Hits:  z vs y");

        plotter.region(0).plot(svtDispZY);
        plotter.region(1).plot(svtDispZX);
        plotter.show();

    }

//    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
//        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
//    }
    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void process(EventHeader event) {
//        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
        if (event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName)) {
            ++eventCount;

//            aida.histogram2D("SVT Raw Hits:  z vs y").reset();

            svtDispZX.reset();
            svtDispZY.reset();
            List<HelicalTrackHit> rawHits = event.get(HelicalTrackHit.class, helicalTrackHitCollectionName);
            for (HelicalTrackHit hrth : rawHits) {
                fillPlots(hrth);
            }

            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalCollectionName);
            //             System.out.println("Number of ECAL clusters="+clusters.size());
            for (HPSEcalCluster cluster : clusters) {
//                dec.setID(cluster.getSeedHit().getCellID());
//                CalorimeterHit seedHit = cluster.getSeedHit();
//                    System.out.println("z = "+seedHit.getPosition()[2]+" y = "+seedHit.getPosition()[1]);
                if (cluster.getEnergy() > 0) {
                    svtDispZY.fill(zEcal, cluster.getPosition()[1] / 10, cluster.getEnergy());
                    svtDispZX.fill(zEcal, cluster.getPosition()[0] / 10, cluster.getEnergy());
                }
            }
        } else {
            System.out.println("SVTEventDisplay:  Event has no HelicalTrackHits");
        }
    }

    @Override
    public void endOfData() {
        plotter.hide();
    }

    private void fillPlots(HelicalTrackHit hit) {

//        SiSensor sensor = (SiSensor) hit.getDetectorElement();
//        SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) (sensor.getIdentifierHelper());
//        int strip = hit.getIdentifierFieldValue("strip");
//        ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(hit.getIdentifier()));
//        SiSensorElectrodes electrodes = ((SiSensor) hit.getDetectorElement()).getReadoutElectrodes(carrier);
//        Hep3Vector position = getGlobalHitPosition(hit, electrodes);

//        short[] adcVal = hit.getADCValues();
//        double ped = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
//        double noise = HPSSVTCalibrationConstants.getNoise(sensor, strip);


//        aida.cloud2D("SVT Raw Hits:  z vs y").fill(position.z() / 10.0, position.y() / 10.0);
//        aida.histogram2D("SVT Raw Hits:  z vs y").fill(position.z()/10.0,position.y()/10.0);
//        aida.histogram2D("SVT Raw Hits:  z vs y").fill(position.z()/10.0,position.y()/10.0);
//        double maxAdc = -9999;
//        for (int i = 0; i < 6; i++) {
//            if (adcVal[i] - ped > maxAdc)
//                maxAdc = adcVal[i] - ped;
//        }
//        if(noise<70){
        //       if (noise < 70 && !mask(position)) {
//                  System.out.println(sensor.getName()+" strip # "+strip+"   "+position.z()+"   " + position.y());
//            svtDisp.fill(position.z()/10.0,position.y()/10.0);
//            svtDispZY.fill(position.z() / 10.0, position.y() / 10.0, maxAdc);
//            svtDispZX.fill(position.z() / 10.0, position.x() / 10.0, maxAdc);

        //           svtDispZY.fill(position.z() / 10.0, position.y() / 10.0, 1000.0);
        //           svtDispZX.fill(position.z() / 10.0, position.x() / 10.0, 1000.0);
        //       }
        svtDispZY.fill(hit.z() / 10.0, hit.y() / 10.0, 1000.0);
        svtDispZX.fill(hit.z() / 10.0, hit.x() / 10.0, 1000.0);

    }

    private Hep3Vector getGlobalHitPosition(RawTrackerHit hit, SiSensorElectrodes electrodes) {
        Hep3Vector position = (((SiStrips) electrodes).getStripCenter(hit.getIdentifierFieldValue("strip")));
        return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
    }

    private boolean mask(Hep3Vector pos) {
        double x = pos.x();
        double y = pos.y();
        double z = pos.z();

        if (z > 300 && z < 320) {
            if (y > -50 && y < -20) {
                return true;
            }
        }

        return false;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
}
