package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.util.Driver;
import org.lcsim.event.EventHeader;

public class AlignmentTestDriver extends Driver {

    private List<SiSensor> sensors = new ArrayList<SiSensor>();

    @Override
    protected void startOfData() {
    }

    @Override
    protected void endOfData() {
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Alignment Manager - Get the composite structures.
        IDetectorElement detectorElement = detector.getDetectorElement();

        // Get the sensors subcomponents // This should be only HpsSiSensors
        sensors = detectorElement.findDescendants(SiSensor.class);

        System.out.println(":::AlignmentTestDriver:::");
        AlignmentStructuresBuilder asb = new AlignmentStructuresBuilder(sensors);

    }

    protected void process(EventHeader event) {
    }

}
