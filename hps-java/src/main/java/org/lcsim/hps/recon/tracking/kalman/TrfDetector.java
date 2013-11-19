/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lcsim.hps.recon.tracking.kalman;

import java.util.List;
import java.util.Map;
import org.lcsim.recon.tracking.trflayer.Detector;
import org.lcsim.recon.tracking.trflayer.Layer;

/**
 *
 * @author ecfine
 */
public class TrfDetector extends Detector {
     // List of layer names in original order.
    private List _names;
    
    // Map layer names to layers.
    private Map _layermap;


    
    public int addLayer(String name, Layer lyr)
    {
        // Set unchecked.
//        if ( _check == CHECKED_OK ) _check = UNCHECKED;

        // Check name does not already appear in map.
        if ( isAssigned(name) ){
            System.out.println("Name already assigned, layer not added.");
            return 0;
        }
        // Add layer.
        _names.add(name);
        _layermap.put(name, lyr);
        System.out.println("Layer named " + name + " added");
        return 1;
    }

}
