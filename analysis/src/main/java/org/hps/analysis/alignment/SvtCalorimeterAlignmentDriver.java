package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class SvtCalorimeterAlignmentDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IHistogram2D trkAtEcalYvsNSigmaTop = aida.histogram2D("trackY at Ecal vs nSigma top", 100, 0., 6., 500, 20., 60.);
    private IHistogram2D trkAtEcalYvsNSigmaBottom = aida.histogram2D("-trackY at Ecal vs nSigma bottom", 100, 0., 6., 500, 20., 60.);

    protected void process(EventHeader event) {
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        if (event.hasCollection(ReconstructedParticle.class, "OtherElectrons")) {
            rpList.addAll(event.get(ReconstructedParticle.class, "OtherElectrons"));
        }
        setupSensors(event);
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

            double nSigma = rp.getGoodnessOfPID();
            Track t = rp.getTracks().get(0);
            int nTrackHits = t.getTrackerHits().size();
            TrackState trackStateAtEcal = TrackStateUtils.getTrackStateAtECal(t);
            Hep3Vector atEcal = new BasicHep3Vector(Double.NaN, Double.NaN, Double.NaN);
            if (trackStateAtEcal != null) {
                atEcal = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
                atEcal = CoordinateTransformations.transformVectorToDetector(atEcal);
            }
            double[] tposAtEcal = atEcal.v();
            Hep3Vector pmom = rp.getMomentum();
            double p = rp.getMomentum().magnitude();
            boolean isTopTrack = isTopTrack(t);
            String topOrBottom = isTopTrack ? " top " : " bottom ";
            boolean isFee = p > 1.6;
            String isFeeString = isFee ? " Fee " : " not Fee ";
            Cluster c = rp.getClusters().get(0);
            int nClusterHits = c.getCalorimeterHits().size();
            double[] cposAtEcal = c.getPosition();
            double clusterEnergy = c.getEnergy();
            CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
            int ix = seedHit.getIdentifierFieldValue("ix");
            int iy = seedHit.getIdentifierFieldValue("iy");
            boolean isFiducial = isFiducial(seedHit);

            aida.histogram2D("Cluster x vs y " + isFeeString + nTrackHits + "hits on Track", 300, -300., 300., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
            aida.histogram2D("Track x vs y " + isFeeString + nTrackHits + "hits on Track", 300, -300., 300., 100, -100., 100.).fill(tposAtEcal[0], tposAtEcal[1]);
            aida.histogram1D("Cluster z", 50, 1393., 1396.).fill(c.getPosition()[2]);
            aida.histogram1D("Track z", 50, 1393., 1396.).fill(tposAtEcal[2]);

            double dX = tposAtEcal[0] - cposAtEcal[0];
            double dY = tposAtEcal[1] - cposAtEcal[1];

            // require fiducial cluster, more than two hits in y, plot dy as fn of x
            int[] rowsColumns = rowsColumns(c);
            aida.histogram2D("Cluster nColumns vs nRows", 6, 0.5, 6.5, 6, 0.5, 6.5).fill(rowsColumns[1], rowsColumns[0]);

            aida.histogram1D("Track momentum" + topOrBottom + nTrackHits + "hits on Track", 100, 0., 3.0).fill(p);
            aida.histogram1D("Cluster energy" + topOrBottom + nTrackHits + "hits on Track", 100, 0., 3.0).fill(c.getEnergy());
            aida.histogram1D("cluster nHits", 20, 0., 20.).fill(c.getCalorimeterHits().size());

            if (!isFiducial) {
                continue;
            }
            if (abs(iy) != 3) {
                continue;
            }
            if (rowsColumns[0] < 2 || rowsColumns[0] > 3) {
                continue;
            }

            if (isFiducial) {
                aida.histogram2D("Fiducial Cluster nColumns vs nRows", 6, 0.5, 6.5, 6, 0.5, 6.5).fill(rowsColumns[1], rowsColumns[0]);
//                aida.histogram2D("Fiducial Cluster x vs y " + isFeeString + nTrackHits + "hits on Track", 300, -300., 300., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
//                aida.histogram2D("Fiducial Cluster trackX at Ecal row " + iy + " vs dY" + topOrBottom + isFeeString + nTrackHits + "hits on Track " + rowsColumns[0] + " rows", 400, -300., 100., 100, -10., 10.).fill(tposAtEcal[0], dY);
                aida.histogram1D("Fiducial Cluster at Ecal row " + iy + " dY" + topOrBottom + isFeeString + nTrackHits + "hits on Track " + rowsColumns[0] + " rows", 100, -10., 10.).fill(dY);
            }

//            aida.histogram2D("trackX at Ecal vs dY" + topOrBottom, 400, -300., 100., 500, -20., 20.).fill(tposAtEcal[0], dY);
//            aida.histogram2D("trackY at Ecal vs dY" + topOrBottom, 200, -100., 100., 500, -20., 20.).fill(tposAtEcal[1], dY);
//            aida.cloud2D("dY vs track x" + topOrBottom).fill(tposAtEcal[0], dY);
//            aida.cloud2D("dY vs track y" + topOrBottom).fill(tposAtEcal[1], dY);
            double absdY = isTopTrack ? abs(dY) : -abs(dY);
            aida.histogram2D("trackY at Ecal vs |dY|" + topOrBottom + isFeeString + nTrackHits + "hits on Track", 400, 20., 60., 500, -20., 20.).fill(abs(tposAtEcal[1]), absdY);

            // look for calorimeter edge wrt SVT
            if (isTopTrack) {
                trkAtEcalYvsNSigmaTop.fill(nSigma, tposAtEcal[1]);
                //aida.cloud2D("trackY at Ecal vs nSigma top cloud").fill(nSigma, tposAtEcal[1]);
            } else {
                trkAtEcalYvsNSigmaBottom.fill(nSigma, -tposAtEcal[1]);
                //aida.cloud2D("trackY at Ecal vs nSigma bottom cloud").fill(nSigma, -tposAtEcal[1]);
            }
        }
    }

    protected void endOfData() {
        //let's try some splitting and fitting
        IAnalysisFactory af = IAnalysisFactory.create();
        IHistogramFactory hf = af.createHistogramFactory(af.createTreeFactory().create());
        IHistogram1D[] bottomSlices = new IHistogram1D[25];
        IHistogram1D[] topSlices = new IHistogram1D[25];
        for (int i = 0; i < 25; ++i) {
            bottomSlices[i] = hf.sliceY("bottom slice " + i, trkAtEcalYvsNSigmaBottom, i);
            topSlices[i] = hf.sliceY("top slice " + i, trkAtEcalYvsNSigmaTop, i);
        }
    }

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    public boolean isFiducial(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        // Get the x and y indices for the cluster.
        int absx = Math.abs(ix);
        int absy = Math.abs(iy);

        // Check if the cluster is on the top or the bottom of the
        // calorimeter, as defined by |y| == 5. This is an edge cluster
        // and is not in the fiducial region.
        if (absy == 5) {
            return false;
        }

        // Check if the cluster is on the extreme left or right side
        // of the calorimeter, as defined by |x| == 23. This is also
        // an edge cluster and is not in the fiducial region.
        if (absx == 23) {
            return false;
        }

        // Check if the cluster is along the beam gap, as defined by
        // |y| == 1. This is an internal edge cluster and is not in the
        // fiducial region.
        if (absy == 1) {
            return false;
        }

        // Lastly, check if the cluster falls along the beam hole, as
        // defined by clusters with -11 <= x <= -1 and |y| == 2. This
        // is not the fiducial region.
        if (absy == 2 && ix <= -1 && ix >= -11) {
            return false;
        }

        // If all checks fail, the cluster is in the fiducial region.
        return true;
    }

    //return the number of rows in in this cluster 
    public int[] rowsColumns(Cluster c) {

        CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
        int ix0 = seedHit.getIdentifierFieldValue("ix");
        int iy0 = seedHit.getIdentifierFieldValue("iy");
//        System.out.println("ix0 " + ix0 + " iy0 " + iy0);
        List<CalorimeterHit> hits = c.getCalorimeterHits();
        Set<Integer> rows = new HashSet<Integer>();
        Set<Integer> columns = new HashSet<Integer>();

        for (CalorimeterHit h : hits) {
            int ix = h.getIdentifierFieldValue("ix");
            columns.add(ix0 - ix);
            int iy = h.getIdentifierFieldValue("iy");
            rows.add(iy0 - iy);
//            System.out.println("ix  " + ix + " iy  " + iy);
        }
//        System.out.println("rows " + rows.size() + " columns " + columns.size());
        return new int[]{rows.size(), columns.size()};
    }
}
