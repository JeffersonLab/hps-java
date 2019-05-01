package org.hps.readout.hodoscope;

import org.hps.readout.RawConverterReadoutDriver;
import org.hps.readout.rawconverter.AbstractMode3RawConverter;
import org.hps.readout.rawconverter.HodoscopeReadoutMode3RawConverter;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.Hodoscope_v1;

/**
 * <code>HodoscopeRawConverterReadoutDriver</code> is an
 * implementation of {@link org.hps.readout.RawConverterReadoutDriver
 * RawConverterReadoutDriver} for the hodoscope subdetector.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.RawConverterReadoutDriver
 */
public class HodoscopeRawConverterReadoutDriver extends RawConverterReadoutDriver {
    /**
     * The converter object responsible for processing raw hits into
     * proper {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
     * objects.
     */
    private HodoscopeReadoutMode3RawConverter converter = new HodoscopeReadoutMode3RawConverter();
    
    /**
     * Instantiates the driver with the correct default parameters.
     */
    public HodoscopeRawConverterReadoutDriver() {
        super("HodoscopeRawHits", "HodoscopeCorrectedHits");
    }
    
    @Override
    protected AbstractMode3RawConverter getConverter() {
        return converter;
    }
    
    @Override
    protected String getSubdetectorReadoutName(Detector detector) {
        Hodoscope_v1 hodoscopeGeometry = (Hodoscope_v1) detector.getSubdetector("Hodoscope");
        return hodoscopeGeometry.getReadout().getName();
    }
    
    @Override
    protected void updateDetectorDependentParameters(Detector detector) { }
}