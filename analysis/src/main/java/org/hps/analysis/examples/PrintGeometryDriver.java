package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class PrintGeometryDriver extends Driver {
    
    @Override
    protected void detectorChanged(Detector detector) {
        System.out.printf("%s: ################# Print geometry ##########################\n",this.getClass().getSimpleName());
        IDetectorElement detectorElement = detector.getDetectorElement();
        List<SiSensor> sensors = detectorElement.findDescendants(SiSensor.class);
        System.out.printf("%s: %5s %40s %40s\n",this.getClass().getSimpleName(),"ID","Pos","u");
        for (SiSensor sensor: sensors) {
            Hep3Vector position = sensor.getGeometry().getPosition();
            Hep3Vector u = this.getUnitVector(sensor, "measured");
            System.out.printf("%s: %5d %40s %40s\n",this.getClass().getSimpleName(),sensor.getSensorID(),position.toString(),u.toString());
        }
        System.out.printf("%s: ###########################################################\n",this.getClass().getSimpleName());
    }
    
    
    private  Hep3Vector getUnitVector(SiSensor sensor,String type)
        {

            Hep3Vector unit_vec = new BasicHep3Vector(-99,-99,-99);

            for (ChargeCarrier carrier : ChargeCarrier.values()) {
                if (sensor.hasElectrodesOnSide(carrier)) {
                int channel = 1;
                    long cell_id = sensor.makeStripId(channel, carrier.charge()).getValue();
                    IIdentifier id = new Identifier(cell_id);
                    SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();    
                    SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(carrier);
                    ITransform3D local_to_global = new Transform3D();// sensor.getGeometry().getLocalToGlobal();
                    ITransform3D electrodes_to_global = electrodes.getLocalToGlobal();
                    ITransform3D global_to_hit = local_to_global.inverse();
                    ITransform3D electrodes_to_hit = Transform3D.multiply(global_to_hit, electrodes_to_global);
                    if(type=="measured") {
                        unit_vec = electrodes_to_hit.rotated(electrodes.getMeasuredCoordinate(0));
                    } else if (type=="unmeasured") {
                        unit_vec = electrodes_to_hit.rotated(electrodes.getUnmeasuredCoordinate(0));
                    } else {
                        throw new UnsupportedOperationException(String.format("type=\"%s\" not supported",type));
                    }
                }
            }
            return unit_vec;
        }
    
}



