package org.lcsim.detector.converter.compact;

import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal;

public class HPSEcalConverter extends AbstractSubdetectorConverter
{
    public void convert(Subdetector subdet, Detector detector)
    {
        System.out.println(this.getClass().getCanonicalName());        
    }
    
    public Class getSubdetectorType()
    {
        return HPSEcal.class;
    }
}
