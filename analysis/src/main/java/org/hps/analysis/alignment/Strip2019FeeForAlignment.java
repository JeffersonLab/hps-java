package org.hps.analysis.alignment;

import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
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
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A Graf
 */
public class Strip2019FeeForAlignment extends Driver {

    int _numberOfEventsSelected;
    private AIDA aida = AIDA.defaultInstance();

    int _maxNClusters = 1;
    double _minClusterEnergy = 3.5;
    double _minSeedHitEnergy = 3.0;

    private int _minNumberOfTracks = 1;
    private int _maxNumberOfTracks = 1;
    private int _minNumberOfHitsOnTrack = 5;

    private int _maxSvtRawTrackerHits = 250;

    private boolean _skimTopTrack = true;
    private boolean _skimBottomTrack = true;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        if (event.get(RawTrackerHit.class, "SVTRawTrackerHits").size() < _maxSvtRawTrackerHits) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
            //System.out.println(clusters.size()+ "clusters");
            if (clusters.size() > 0 && clusters.size() <= _maxNClusters) {
                for (Cluster cluster : clusters) {
                    //System.out.println("cluster energy "+cluster.getEnergy());
                    if (cluster.getEnergy() > _minClusterEnergy) {
                        double seedEnergy = ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy();
                        if (seedEnergy > _minSeedHitEnergy) {
                            if (TriggerModule.inFiducialRegion(cluster)) {
                                List<Track> tracks = event.get(Track.class, "GBLTracks");
                                int nTracks = tracks.size();
                                //System.out.println(nTracks+" GBL tracks");
                                if (nTracks >= _minNumberOfTracks && nTracks <= _maxNumberOfTracks) {
                                    setupSensors(event);
                                    for (Track t : tracks) {
                                        int nhits = t.getTrackerHits().size();
                                        boolean isTop = isTopTrack(t);
                                        String half = isTop ? "top" : "bottom";
                                        aida.histogram1D(half + " number of hits on track", 10, 0., 10.).fill(nhits);
                                        //System.out.println("with "+nhits+" hits");
                                        if (nhits >= _minNumberOfHitsOnTrack) {
                                            if (_skimTopTrack && isTop) {
                                                skipEvent = false;
                                            }
                                            if (_skimBottomTrack && !isTop) {
                                                skipEvent = false;
                                            }
                                            //System.out.println("good");
                                            aida.histogram2D("Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                                            if (cluster.getPosition()[1] > 0.) {
                                                aida.histogram1D("Top cluster energy ", 100, 3.5, 5.5).fill(cluster.getEnergy());
                                            } else {
                                                aida.histogram1D("Bottom cluster energy ", 100, 3.5, 5.5).fill(cluster.getEnergy());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

    public void setMinClusterEnergy(double d) {
        _minClusterEnergy = d;
    }

    public void setMinSeedHitEnergy(double d) {
        _minSeedHitEnergy = d;
    }

    public void setMaxNClusters(int i) {
        _maxNClusters = i;
    }

    public void setMaxNumberOfTracks(int i) {
        _maxNumberOfTracks = i;
    }

    public void setMinNumberOfTracks(int i) {
        _minNumberOfTracks = i;
    }

}
