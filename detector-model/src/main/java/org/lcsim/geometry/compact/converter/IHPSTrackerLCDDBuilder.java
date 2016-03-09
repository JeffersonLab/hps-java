package org.lcsim.geometry.compact.converter;

import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public interface IHPSTrackerLCDDBuilder {

    public  void setSensitiveDetector(SensitiveDetector sens);

    public  SensitiveDetector getSensitiveDetector();
    
    public void build(Volume worldVolume);

    public void setVisualization();
}