/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.alignment;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author ngraf
 */
public class FieldOffMCStripEvent extends Driver {

    boolean _selectTopTracks = true;
    boolean _selectBottomTracks = false;
    boolean skipEvent = false;
    int _numberOfEventsWritten;

    @Override
    protected void process(EventHeader event) {
        skipEvent = false;
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
//        System.out.println("event has " + simHits.size() + " SimTrackerHits");
        Set<Integer> topLayers = new TreeSet<Integer>();
        Set<Integer> bottomLayers = new TreeSet<Integer>();
        for (SimTrackerHit hit : simHits) {
            int layer = hit.getLayer();
            double y = hit.getPoint()[1];
            if (y > 0) {
                topLayers.add(layer);
            } else {
                bottomLayers.add(layer);
            }
        }
        System.out.println("top has " + topLayers.size() + " hits");
        System.out.println("bottom has " + bottomLayers.size() + " hits");

        if (_selectTopTracks && topLayers.size() != 12) {
            skipEvent = true;
        }
        
        if (_selectBottomTracks && bottomLayers.size() != 12) {
            skipEvent = true;
        }
        System.out.println("skipEvent "+skipEvent);
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
        System.out.println(_numberOfEventsWritten);
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsWritten + " events");
    }

    public void setSelectTopTracks(boolean b) {
        _selectTopTracks = b;
    }

    public void setSelectBottomTracks(boolean b) {
        _selectBottomTracks = b;
    }

}
