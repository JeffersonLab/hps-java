package org.hps.analysis.alignment;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Strip tracks associated with MIP cluster in calorimeter Might be muons useful
 * for alignment.
 *
 * @author Norman A Graf
 */
public class StripMipTrack extends Driver {

    int _numberOfEventsSelected;
    private AIDA aida = AIDA.defaultInstance();

    int maxNClusters = 99;
    double _minClusterEnergy = 0.0;
    double _maxClusterEnergy = 0.3;
    double _minSeedHitEnergy = 0.0;

    private int _minNumberOfHitsOnTrack = 5;

    private int _maxSvtRawTrackerHits = 250;

    private boolean _skimTopTrack = true;
    private boolean _skimBottomTrack = true;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        if (event.hasCollection(GenericObject.class, "TSBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TSBank");
            for (GenericObject data : triggerList) {
                if (AbstractIntData.getTag(data) == TSData2019.BANK_TAG) {
                    TSData2019 triggerData = new TSData2019(data);
                    int[] indices = triggerData.getIndicesOfRegisteredTriggers(); // registered triggers are save into array 

                    // You also can call methods to check if a specified trigger is registered. For example, triggerData.isSingle3TopTrigger() to check if Single 3 top is registered. For other methods, please refer to the class TSData2019.
                }
            }
        }
        if (event.get(RawTrackerHit.class, "SVTRawTrackerHits").size() < _maxSvtRawTrackerHits) {
            // get the ReconstructedParticles in this event
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
            // now add in the FEE candidates
            rpList.addAll(event.get(ReconstructedParticle.class, "OtherElectrons"));
            aida.histogram1D("number of ReconstructedParticles", 10, 0., 10.).fill(rpList.size());
            int nMipTracks = 0;
            List<ReconstructedParticle> mipTracks = new ArrayList<>();
            for (ReconstructedParticle rp : rpList) {
                if (abs(rp.getParticleIDUsed().getPDG()) == 11 && TrackType.isGBL(rp.getType())) {
                    Track t = rp.getTracks().get(0);
                    int nhits = t.getTrackerHits().size();
                    aida.histogram1D(nhits + " hit track chisq", 100, 0., 100.).fill(t.getChi2());
                    aida.histogram1D(nhits + " hit track momentum", 100, 0., 5.).fill(rp.getMomentum().magnitude());
                    if (!rp.getClusters().isEmpty()) {
                        Cluster cluster = rp.getClusters().get(0);
                        //System.out.println("cluster energy "+cluster.getEnergy());
                        aida.histogram2D("Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                        if (cluster.getPosition()[1] > 0.) {
                            aida.histogram1D("Top cluster energy ", 100, 0., 5.5).fill(cluster.getEnergy());
                        } else {
                            aida.histogram1D("Bottom cluster energy ", 100, 0., 5.5).fill(cluster.getEnergy());
                        }
                        if (cluster.getEnergy() > _minClusterEnergy && cluster.getEnergy() < _maxClusterEnergy) {
                            if (TriggerModule.inFiducialRegion(cluster)) {
                                setupSensors(event);
                                boolean isTop = isTopTrack(t);
                                String half = isTop ? "top" : "bottom";
                                aida.histogram1D(half + " " + nhits + " hit track with MIP cluster chisq", 100, 0., 100.).fill(t.getChi2());
                                aida.histogram1D(half + " " + nhits + " hit track with MIP cluster momentum", 100, 0., 5.).fill(rp.getMomentum().magnitude());
                                aida.histogram1D(half + " number of hits on track", 10, 0., 10.).fill(nhits);
                                if (nhits >= _minNumberOfHitsOnTrack) {
                                    if (_skimTopTrack && isTop) {
                                        skipEvent = false;
                                        mipTracks.add(rp);
                                    }
                                    if (_skimBottomTrack && !isTop) {
                                        skipEvent = false;
                                        mipTracks.add(rp);
                                    }
                                    aida.histogram2D("MIP Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                                    if (cluster.getPosition()[1] > 0.) {
                                        aida.histogram1D("Top MIP cluster energy ", 100, 0., 0.5).fill(cluster.getEnergy());
                                    } else {
                                        aida.histogram1D("Bottom MIP cluster energy ", 100, 0., 0.5).fill(cluster.getEnergy());
                                    }
                                }
                            }
                        }// end of check on cluster energy
                    }//end of check whether ReconstructedParticle has a cluster
                }//end of check on GBL tracks
            } // end of loop over ReconstructedParticles
            aida.histogram1D("number of MIP ReconstructedParticles", 10, 0., 10.).fill(mipTracks.size());
            if (mipTracks.size() == 2) {
                double e1 = mipTracks.get(0).getEnergy();
                double e2 = mipTracks.get(1).getEnergy();
                double deltaE = e1 - e2;
                aida.histogram2D("Two MIP clusters e1 vs e2", 100, 0., 0.5, 100, 0., 0.5).fill(e1, e2);
                if (abs(deltaE) > .0001) {
                    aida.histogram1D("Two MIP clusters e1-e2", 100, -0.1, 0.1).fill(e1 - e2);
                    aida.histogram2D("Two MIP clusters e1 vs e2 nopulsers", 100, 0., 0.5, 100, 0., 0.5).fill(e1, e2);
                }

            }

        } // end of check on SVT monster events

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class,
                "SVTRawTrackerHits");
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

    public void setSkimBottomTrack(boolean b) {
        _skimBottomTrack = b;
    }

    public void setSkimTopTrack(boolean b) {
        _skimTopTrack = b;
    }

    public void setMinNumberOfHitsOnTrack(int i) {
        _minNumberOfHitsOnTrack = i;
    }

    public void setMaxClusterEnergy(double d) {
        double _maxClusterEnergy = d;
    }
}
