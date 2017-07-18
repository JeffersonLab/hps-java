/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import junit.framework.TestCase;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.util.DetectorLocator;
import org.lcsim.util.loop.LCSimConditionsManagerImplementation;
import org.hps.recon.tracking.seedtracker.MaterialSupervisor

/**
 *
 * @author ngraf
 */
public class MaterialSupervisorTest extends TestCase
{

    public void testMaterialSupervisor()
    {
        String detectorName = "HPS-Proposal2014-v5-2pt2";
        Detector det = DetectorLocator.findDetector(detectorName);
        System.out.println(det.getName());
        
        boolean debug = true;
        boolean includeMS=true;
        MaterialSupervisor instance = new MaterialSupervisor(includeMS);
        instance.setDebug(debug);
        // following call crashes.
        // evidently something else needs to be set up
     //   instance.buildModel(det);

    }
}
