package org.lcsim.hps.conditions.demo;

import org.lcsim.conditions.CachedConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsSetNotFoundException;
import org.lcsim.conditions.ConditionsSet;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author onoprien
 */
public class ExampleDriver extends Driver {
  
  public ExampleDriver() {
    
  }

  protected void detectorChanged(Detector detector) {
    System.out.println("detectorChanged: "+ detector.getDetectorName());
    super.detectorChanged(detector);
    ConditionsManager condMan = getConditionsManager();
    try {
      ConditionsSet set1 = condMan.getConditions("HadronCalibration/EMBarrel");
      System.out.println("HadronCalibration/EMBarrel: hitEnergycut1 = "+ set1.getDouble("hitEnergycut1"));
    } catch (ConditionsSetNotFoundException x) {
      System.out.println("No HadronCalibration/EMBarrel: hitEnergycut1");
    }
    try {
      ConditionsSet set1 = condMan.getConditions("SamplingFractions/EMBarrel");
      System.out.println("1");
      System.out.println("SamplingFractions/EMBarrel: samplingFraction = "+ set1.getDouble("samplingFraction"));
    } catch (ConditionsSetNotFoundException x) {
      System.out.println("No SamplingFractions/EMBarrel: samplingFraction");
    }
    try {
      ConditionsSet set2 = condMan.getConditions("calibration");
      System.out.println("calibration: "+ set2.getString("table") +"  "+ set2.getString("column") +"  "+ set2.getString("id"));
      CachedConditions<Calibration> c = condMan.getCachedConditions(Calibration.class, "");
      Calibration cal = c.getCachedData();
      System.out.println("New calibration "+ cal);
    } catch (ConditionsSetNotFoundException x) {
      System.out.println("No calibration found "+ x);
    }
  }

  protected void endOfData() {
    System.out.println("endOfData");
    super.endOfData();
  }

  protected void resume() {
    System.out.println("resume");
    super.resume();
  }

  protected void suspend() {
    System.out.println("suspend");
    super.suspend();
  }

  protected void startOfData() {
    System.out.println("startOfData");
    super.startOfData();
  }
  
}
