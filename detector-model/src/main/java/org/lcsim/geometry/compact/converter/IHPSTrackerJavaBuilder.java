package org.lcsim.geometry.compact.converter;

import org.lcsim.detector.DetectorIdentifierHelper;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.geometry.compact.Subdetector;

public interface IHPSTrackerJavaBuilder {

    /**
     * Build the JAVA geometry objects from the geometry definition.
     * @param trackingVolume - the reference volume.
     */
    public void build(ILogicalVolume trackingVolume);
    
    public DetectorIdentifierHelper getDetectorIdentifierHelper();

    public void setDetectorIdentifierHelper(
            DetectorIdentifierHelper detectorIdentifierHelper);

    public IIdentifierDictionary getIdentifierDictionary();

    public void setIdentifierDictionary(
            IIdentifierDictionary identifierDictionary);


    public void setSubdetector(Subdetector subdet);

    public Subdetector getSubdetector();
    
}