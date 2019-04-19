/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.hodo;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Logger;
//
//import org.hps.conditions.database.DatabaseConditionsManager;
//import org.hps.conditions.ecal.EcalChannel;
//import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
//import org.lcsim.event.GenericObject;
//import org.lcsim.event.LCRelation;
//import org.lcsim.event.RawCalorimeterHit;
//import org.lcsim.event.RawTrackerHit;
//import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;


/**
 *
 * @author rafopar
 */
public class HodoTestDriver extends Driver {

    @Override
    protected void process(EventHeader event) {

        System.out.println(" Kuku Testing Rafo");
        
    }

}
