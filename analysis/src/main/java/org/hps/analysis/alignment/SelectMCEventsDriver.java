package org.hps.analysis.alignment;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

/**
 * Primarily intended to select single track events for tracking studies such as
 * FEE and unconverted WABs. Could also be used for Moller selection.
 *
 * @author Norman A. Graf
 */
public class SelectMCEventsDriver extends Driver {

    private boolean _selectAllLayers = false;
    private int _requireNLayers = 14;
    private boolean _selectTopHits = true;
    private boolean _selectBottomHits = true;
    private int _numberOfEventsSelected = 0;
    private int _numberOfEventsProcessed = 0;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        Set<Integer> topLayers = new TreeSet<Integer>();
        Set<Integer> bottomLayers = new TreeSet<Integer>();
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        for (SimTrackerHit hit : simHits) {
            if (hit.getDetectorElement() instanceof HpsSiSensor) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                String sensorName = hit.getDetectorElement().getName();
                System.out.println(hit.getLayerNumber());
                System.out.println(sensorName);
                if (sensorName.contains("t_")) {
                    topLayers.add(sensor.getLayerNumber());
                } else {
                    bottomLayers.add(sensor.getLayerNumber());
                }
            }
        }

        if (_selectTopHits) {
            if (topLayers.size() >= _requireNLayers) {
                skipEvent = false;
            }
        }
        if (_selectBottomHits) {
            if (bottomLayers.size() >= _requireNLayers) {
                skipEvent = false;
            }
        }
        System.out.println("Number of top hits: " + topLayers.size());
        System.out.println("Number of bottom hits: " + bottomLayers.size());
        _numberOfEventsProcessed++;
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }

    }
    
    public void setSelectTopLayerHits(boolean b)
    {
        _selectTopHits = b;
    }
    
    public void setSelectBottomLayerHits(boolean b)
    {
        _selectBottomHits = b;
    }
    
    public void setSelectMinNumberOfHitLayers(int n)
    {
        _requireNLayers = n;
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events out of " + _numberOfEventsProcessed);
    }
}
