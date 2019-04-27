package org.lcsim.detector.converter.compact;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

import junit.framework.TestCase;

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
        IIdentifierHelper helper = hodo.getDetectorElement().getIdentifierHelper();
        for (IDetectorElement detElem : hodoDetElem.getChildren()) {
            System.out.println(detElem.getName());
            System.out.println(detElem.getGeometry().getPosition());
            System.out.println(helper.unpack(detElem.getIdentifier()));
            System.out.println();
        }
    }   
}
