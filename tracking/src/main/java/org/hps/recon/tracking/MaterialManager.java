/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.recon.tracking.seedtracker.MaterialXPlane;



/**
 *
 * Extension to lcsim MaterialManager to allow more flexibility in track reconstruction
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @version $Id: MaterialManager.java,v 1.3 2013/11/07 03:54:58 phansson Exp $ $Date: 2013/11/07 03:54:58 $ $Author: phansson $ 
 */
public class MaterialManager extends org.lcsim.recon.tracking.seedtracker.MaterialManager {
    
    protected boolean _includeMS = true;
    private final static List<MaterialXPlane> _emptyMaterialXPlaneList = new ArrayList<MaterialXPlane>();
    public MaterialManager() {
        super();
    }
    public MaterialManager(boolean includeMS) {
        super();
        this._includeMS = includeMS;
    }
    @Override
    public List<MaterialXPlane> getMaterialXPlanes() {
        return this._includeMS ? super.getMaterialXPlanes() : _emptyMaterialXPlaneList;
    }

    @Override
    public void setDebug(boolean debug) {
        super.setDebug(debug);
    }
    
    
}
