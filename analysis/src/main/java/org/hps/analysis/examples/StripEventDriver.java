package org.hps.analysis.examples;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripEventDriver extends Driver
{

    String _triggerType = "all";//allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1

    private int _minNumberOfTracks = 0;
    private int _maxNumberOfTracks = Integer.MAX_VALUE;
    private int _minNumberOfHitsOnTrack = 0;
    private int _numberOfEventsWritten = 0;
    private int _minNumberOfUnconstrainedV0Vertices = 0;
    private int _minNumberOfStripHits = 0;
    private int _maxNumberOfStripHits = Integer.MAX_VALUE;
    private int _minNumberOfClusters = 0;
    private int _maxNumberOfClusters = Integer.MAX_VALUE;
    private double _minClusterEnergy = 0.;
    private double _maxClusterEnergy = 12.;
    private boolean _selectTopClusters = false;
    private boolean _selectBottomClusters = false;

    private String _clusterCollectionName = "EcalClusters";

    private boolean _selectAllLayers = false;
    private int _requireNLayers = 12;
    private boolean _selectTopHits = false;
    private boolean _selectBottomHits = false;

    @Override
    protected void process(EventHeader event)
    {
        boolean skipEvent = false;
        int nTracks = 0;

        if (!matchTrigger(event)) {
            skipEvent = true;
        } else {
            if (event.hasCollection(Track.class, "MatchedTracks")) {
                nTracks = event.get(Track.class, "MatchedTracks").size();
                if (nTracks >= _minNumberOfTracks && nTracks <= _maxNumberOfTracks) {
                    List<Track> tracks = event.get(Track.class, "MatchedTracks");
                    for (Track t : tracks) {
                        int nhits = t.getTrackerHits().size();
                        if (nhits < _minNumberOfHitsOnTrack) {
                            skipEvent = true;
                        }
                    }
                } else {
                    skipEvent = true;
                }
            }
            if (event.hasCollection(Vertex.class, "UnconstrainedV0Vertices")) {
                int nVertices = event.get(Vertex.class, "UnconstrainedV0Vertices").size();
                if (nVertices < _minNumberOfUnconstrainedV0Vertices) {
                    skipEvent = true;
                }
            }
            if (event.hasCollection(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D")) {
                int nHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D").size();
                if (nHits < _minNumberOfStripHits) {
                    skipEvent = true;
                }
                if (nHits > _maxNumberOfStripHits) {
                    skipEvent = true;
                }

                // add capability to require all tracking layers to have been hit
                if (!skipEvent && _selectAllLayers) {
                    setupSensors(event);
                    Set<Integer> topLayers = new TreeSet<Integer>();
                    Set<Integer> bottomLayers = new TreeSet<Integer>();
                    List<TrackerHit> hits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
                    for (TrackerHit hit : hits) {
                        List rawHits = hit.getRawHits();
                        HpsSiSensor sensor = null;
                        for (Object o : rawHits) {
                            RawTrackerHit rth = (RawTrackerHit) o;
                            sensor = (HpsSiSensor) rth.getDetectorElement();
                        }
                        String layerName = sensor.getName();
                        if (layerName.contains("t_")) {
                            topLayers.add(sensor.getLayerNumber());
                        } else {
                            bottomLayers.add(sensor.getLayerNumber());
                        }
                    }
                    if (_selectTopHits) {
                        if (topLayers.size() != _requireNLayers) {
                            skipEvent = true;
                        }
                    }
                    if (_selectBottomHits) {
                        if (bottomLayers.size() != _requireNLayers) {
                            skipEvent = true;
                        }
                    }
                    // if we don't explicitly request top or bottom, 
                    // only keep event if either the top or the bottom has all twelve layers hit
                    if (!_selectTopHits && !_selectBottomHits) {
                        skipEvent = true;
                        if (topLayers.size() == _requireNLayers) {
                            skipEvent = false;
                        }
                        if (bottomLayers.size() == _requireNLayers) {
                            skipEvent = false;
                        }
                    }
                }
            }
            if (event.hasCollection(Cluster.class, _clusterCollectionName)) {
                List<Cluster> clusters = event.get(Cluster.class, _clusterCollectionName);
                int nclusters = clusters.size();
                if (nclusters < _minNumberOfClusters) {
                    skipEvent = true;
                }
                if (nclusters > _maxNumberOfClusters) {
                    skipEvent = true;
                }
                for (Cluster clus : clusters) {
                    double e = clus.getEnergy();
                    if (e < _minClusterEnergy) {
                        skipEvent = true;
                    }
                    if (e > _maxClusterEnergy) {
                        skipEvent = true;
                    }
                    double y = clus.getPosition()[1];
                    if (_selectTopClusters && y < 0) {
                        skipEvent = true;
                    }
                    if (_selectBottomClusters && y > 0) {
                        skipEvent = true;
                    }
                }
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

    public void setMinNumberOfTracks(int nTracks)
    {
        _minNumberOfTracks = nTracks;
    }

    public void setMaxNumberOfTracks(int nTracks)
    {
        _maxNumberOfTracks = nTracks;
    }

    public void setMinNumberOfHitsOnTrack(int nHits)
    {
        _minNumberOfHitsOnTrack = nHits;
    }

    public void setMinNumberOfUnconstrainedV0Vertices(int nVertices)
    {
        _minNumberOfUnconstrainedV0Vertices = nVertices;
    }

    public void setMinNumberOfStripHits(int n)
    {
        _minNumberOfStripHits = n;
    }

    public void setMaxNumberOfStripHits(int n)
    {
        _maxNumberOfStripHits = n;
    }

    public void setMinNumberOfClusters(int n)
    {
        _minNumberOfClusters = n;
    }

    public void setMaxNumberOfClusters(int n)
    {
        _maxNumberOfClusters = n;
    }

    public void setMinClusterEnergy(double e)
    {
        _minClusterEnergy = e;
    }

    public void setMaxClusterEnergy(double e)
    {
        _maxClusterEnergy = e;
    }

    public void setClusterCollectionName(String s)
    {
        _clusterCollectionName = s;
    }

    public void setSelectTopHits(boolean b)
    {
        _selectTopHits = b;
    }

    public void setSelectBottomHits(boolean b)
    {
        _selectBottomHits = b;
    }

    public void setSelectTopClusters(boolean b)
    {
        _selectTopClusters = b;
    }

    public void setSelectBottomClusters(boolean b)
    {
        _selectBottomClusters = b;
    }

    public void setSelectAllLayers(boolean b)
    {
        _selectAllLayers = b;
    }

    public void setSelectNumberOfLayers(int i)
    {
        _requireNLayers = i;
    }

    private void setupSensors(EventHeader event)
    {
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

    public void setTriggerType(String type)
    {
        _triggerType = type;
    }

    public boolean matchTriggerType(TIData triggerData)
    {
        if (_triggerType.contentEquals("") || _triggerType.contentEquals("all")) {
            return true;
        }
        if (triggerData.isSingle0Trigger() && _triggerType.contentEquals("singles0")) {
            return true;
        }
        if (triggerData.isSingle1Trigger() && _triggerType.contentEquals("singles1")) {
            return true;
        }
        if (triggerData.isPair0Trigger() && _triggerType.contentEquals("pairs0")) {
            return true;
        }
        if (triggerData.isPair1Trigger() && _triggerType.contentEquals("pairs1")) {
            return true;
        }
        if (triggerData.isPulserTrigger() && _triggerType.contentEquals("pulser")) {
            return true;
        }
        return false;

    }

    public boolean matchTrigger(EventHeader event)
    {
        boolean match = true;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList) {
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!matchTriggerType(triggerData))//only process singles0 triggers...
                    {
                        match = false;
                    }
                }
            }
        }
        return match;
    }

}
