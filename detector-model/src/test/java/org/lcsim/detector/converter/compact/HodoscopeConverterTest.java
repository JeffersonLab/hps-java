package org.lcsim.detector.converter.compact;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

import junit.framework.TestCase;


/**
 * Unit test for the HPSTracker2Converter.
 * 
 * @author SA annie@jlab.org
 */
public class HodoscopeConverterTest extends TestCase {
       
    public void testHodoscopeConverter() {                
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        try {
            mgr.setDetector("HPS-HodoscopeTest-v1", 1000000);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        Detector det = mgr.getDetectorObject();
        Subdetector hodo = det.getSubdetector("Hodoscope");
        HodoscopeDetectorElement hodoDetElem = (HodoscopeDetectorElement)hodo.getDetectorElement();
        for (IDetectorElement detElem : hodoDetElem.getChildren()) {
            System.out.println(detElem.getName());
        }
    }   
}
