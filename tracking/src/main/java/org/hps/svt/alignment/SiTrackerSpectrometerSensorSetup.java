package org.hps.svt.alignment;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;

import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.solids.Trd;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.SiTrackerSpectrometer;
import org.lcsim.util.Driver;

public class SiTrackerSpectrometerSensorSetup extends Driver {

    String subdetectorName;

    public SiTrackerSpectrometerSensorSetup() {
    }

    public SiTrackerSpectrometerSensorSetup(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void detectorChanged(Detector detector) {
        if (subdetectorName == null) {
            throw new RuntimeException("The subdetectorName was not set.");
        }

        Subdetector subdetector = detector.getSubdetector(subdetectorName);
        if (subdetector instanceof SiTrackerSpectrometer) {
            setupSensorDetectorElements(subdetector);
        } else {
            throw new RuntimeException("The subdetector " + subdetectorName
                    + " is not an instance of SiTrackerSpectrometer.");
        }
    }

    private void setupSensorDetectorElements(Subdetector subdet) {
        System.out.println(this.getClass().getCanonicalName() + " - Setting up sensors for " + subdet.getName()
                + " ...");
        int sensorId = 0;

        for (IDetectorElement endcap : subdet.getDetectorElement().getChildren()) {
            for (IDetectorElement layer : endcap.getChildren()) {
                // int nwedges = layer.getChildren().size();
                for (IDetectorElement wedge : layer.getChildren()) {
                    for (IDetectorElement module : wedge.getChildren()) {
                        List<SiSensor> sensors = module.findDescendants(SiSensor.class);

                        if (sensors.size() == 0) {
                            throw new RuntimeException("No sensors found in module.");
                        }

                        for (SiSensor sensor : sensors) {
                            Trd sensor_solid = (Trd) sensor.getGeometry().getLogicalVolume().getSolid();

                            Polygon3D n_side = sensor_solid.getFacesNormalTo(new BasicHep3Vector(0, -1, 0)).get(0);
                            Polygon3D p_side = sensor_solid.getFacesNormalTo(new BasicHep3Vector(0, 1, 0)).get(0);

                            // Bias the sensor
                            // sensor.setBiasSurface(ChargeCarrier.ELECTRON, p_side);
                            // sensor.setBiasSurface(ChargeCarrier.HOLE, n_side);

                            sensor.setBiasSurface(ChargeCarrier.HOLE, p_side);
                            sensor.setBiasSurface(ChargeCarrier.ELECTRON, n_side);

                            // double strip_angle = Math.atan2(sensor_solid.getXHalfLength2() -
                            // sensor_solid.getXHalfLength1(), sensor_solid.getZHalfLength() * 2);
                            double strip_angle = 0.00;
                            ITranslation3D electrodes_position = new Translation3D(VecOp.mult(-p_side.getDistance(),
                                    new BasicHep3Vector(0, 0, 1))); // translate to outside of polygon
                            // ITranslation3D electrodes_position = new Translation3D(VecOp.mult(n_side.getDistance(),
                            // new BasicHep3Vector(0, 0, 1))); // translate to outside of polygon
                            // System.out.println("SensorID = " + sensorId + "  " + electrodes_position.toString());
                            IRotation3D electrodes_rotation = new RotationPassiveXYZ(-Math.PI / 2, 0, strip_angle);
                            Transform3D electrodes_transform = new Transform3D(electrodes_position, electrodes_rotation);

                            // Free calculation of readout electrodes, sense electrodes determined thereon
                            // SiStrips readout_electrodes = new SiStrips(ChargeCarrier.HOLE, 0.060, sensor,
                            // electrodes_transform);
                            // SiStrips sense_electrodes = new
                            // SiStrips(ChargeCarrier.HOLE,0.030,(readout_electrodes.getNCells()*2-1),sensor,electrodes_transform);
                            ITranslation3D misalign_position;
                            // System.out.println(layer.getName());
                            // if (layer.getName().contains("3")) {
                            // if (layer.getName().contains("3")&&layer.getName().contains("positive")) {
                            /*
                             * if (layer.getName().contains("positive")) {
                             * System.out.println("Putting in a misalignment for layer "+layer.getName());
                             * misalign_position = new Translation3D(0, 0.05, 0.0); // translate to outside of polygon }
                             * else { // misalign_position = new Translation3D(0, 0.0, 0.0); misalign_position = new
                             * Translation3D(0, -0.05, 0.0); }
                             */

                            // if
                            // ((layer.getName().contains("3")||layer.getName().contains("4"))&&layer.getName().contains("positive"))
                            // {
                            // if (layer.getName().contains("positive")) {
                            // System.out.println("Putting in a misalignment for layer "+layer.getName());
                            // misalign_position = new Translation3D(0, 0.010, 0.0);
                            // } else {
                            // misalign_position = new Translation3D(0, 0.0, 0.0);
                            // misalign_position = new Translation3D(0, -0.05, 0.0);
                            // }
                            /*
                             * if (layer.getName().contains("positive")) { int gid =
                             * GetIdentifierModule(layer.getName()); misalign_position = new Translation3D(0, 0.0, 0.0);
                             * if (gid == 1) misalign_position = new Translation3D(0, -0.0144, 0.0); if (gid == 2)
                             * misalign_position = new Translation3D(0, 0.05-0.0297, 0.0); if (gid == 3)
                             * misalign_position = new Translation3D(0, -0.0253, 0.0); if (gid == 4) misalign_position =
                             * new Translation3D(0, -0.0346, 0.0); if (gid == 5) misalign_position = new
                             * Translation3D(0, -0.0433, 0.0); } else { misalign_position = new Translation3D(0, 0.0,
                             * 0.0); }
                             */

                            int gid = GetIdentifierLayer(layer.getName());
                            misalign_position = new Translation3D(0, 0.0, 0.0);
                            /*
                             * if (layer.getName().contains("positive")) { if (gid == 1) misalign_position = new
                             * Translation3D(0, -0.00144, 0.0); if (gid == 2) misalign_position = new Translation3D(0,
                             * 0.005, 0.0); if (gid == 3) misalign_position = new Translation3D(0, -0.00253, 0.0); if
                             * (gid == 4) misalign_position = new Translation3D(0, -0.00346, 0.0); if (gid == 5)
                             * misalign_position = new Translation3D(0, 0.00433, 0.0); if (gid == 6) misalign_position =
                             * new Translation3D(0, 0.0002, 0.0); if (gid == 7) misalign_position = new Translation3D(0,
                             * 0.002, 0.0); if (gid == 8) misalign_position = new Translation3D(0, -0.004, 0.0); if (gid
                             * == 9) misalign_position = new Translation3D(0, 0.006, 0.0); if (gid == 10)
                             * misalign_position = new Translation3D(0, -0.001, 0.0); } else { if (gid == 1)
                             * misalign_position = new Translation3D(0, 0.00, 0.0); if (gid == 2) misalign_position =
                             * new Translation3D(0, 0.00, 0.0); if (gid == 3) misalign_position = new Translation3D(0,
                             * 0.00, 0.0); if (gid == 4) misalign_position = new Translation3D(0, 0.00, 0.0); if (gid ==
                             * 5) misalign_position = new Translation3D(0, 0.00, 0.0); if (gid == 6) misalign_position =
                             * new Translation3D(0, 0.00, 0.0); if (gid == 7) misalign_position = new Translation3D(0,
                             * 0.00, 0.0); if (gid == 8) misalign_position = new Translation3D(0, 0.00, 0.0); if (gid ==
                             * 9) misalign_position = new Translation3D(0, 0.00, 0.0); if (gid == 10) misalign_position
                             * = new Translation3D(0, 0.01, 0.0); }
                             */
                            IRotation3D misalign_rotation = new RotationPassiveXYZ(0, 0, 0);
                            Transform3D misalign_transform = new Transform3D(misalign_position, misalign_rotation);

                            HPSStrips readout_electrodes = new HPSStrips(ChargeCarrier.HOLE, 0.060, sensor,
                                    electrodes_transform, misalign_transform);
                            HPSStrips sense_electrodes = new HPSStrips(ChargeCarrier.HOLE, 0.030,
                                    (readout_electrodes.getNCells() * 2 - 1), sensor, electrodes_transform,
                                    misalign_transform);

                            // SiStrips readout_electrodes = new SiStrips(ChargeCarrier.ELECTRON, 0.060, sensor,
                            // electrodes_transform);
                            // SiStrips sense_electrodes = new SiStrips(ChargeCarrier.ELECTRON, 0.030,
                            // (readout_electrodes.getNCells() * 2 - 1), sensor, electrodes_transform);

                            // SiSensorElectrodes sense_electrodes = new SiStrips(ChargeCarrier.HOLE, 0.060, sensor,
                            // electrodes_transform);

                            // pristine conditions
                            /*
                             * readout_electrodes.setCapacitanceIntercept(0);
                             * readout_electrodes.setCapacitanceSlope(0.12);
                             * sense_electrodes.setCapacitanceIntercept(0); sense_electrodes.setCapacitanceSlope(0.12);
                             */

                            readout_electrodes.setCapacitanceIntercept(0);
                            readout_electrodes.setCapacitanceSlope(0.16);
                            sense_electrodes.setCapacitanceIntercept(0);
                            sense_electrodes.setCapacitanceSlope(0.16);

                            sensor.setSenseElectrodes(sense_electrodes);
                            sensor.setReadoutElectrodes(readout_electrodes);
                            //

                            // double[][] transfer_efficiencies = {{1.0}};
                            double[][] transfer_efficiencies = {{0.986, 0.419}};
                            sensor.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transfer_efficiencies));
                            // sensor.setTransferEfficiencies(ChargeCarrier.ELECTRON, new
                            // BasicMatrix(transfer_efficiencies));
                            // here

                            sensor.setSensorID(++sensorId);
                        }
                    }
                }
            }
        }
    }

    private int GetIdentifierModule(String mylayer) {
        int gid = -1;
        if (mylayer.contains("1") || mylayer.contains("2"))
            gid = 1;
        if (mylayer.contains("3") || mylayer.contains("4"))
            gid = 2;
        if (mylayer.contains("5") || mylayer.contains("6"))
            gid = 3;
        if (mylayer.contains("7") || mylayer.contains("8"))
            gid = 4;
        if (mylayer.contains("9") || mylayer.contains("10"))
            gid = 5;

        return gid; // return top/bottom plates
    }

    private int GetIdentifierLayer(String mylayer) {
        int gid = -1;
        if (mylayer.contains("1"))
            gid = 1;
        if (mylayer.contains("2"))
            gid = 2;
        if (mylayer.contains("3"))
            gid = 3;
        if (mylayer.contains("4"))
            gid = 4;
        if (mylayer.contains("5"))
            gid = 5;
        if (mylayer.contains("6"))
            gid = 6;
        if (mylayer.contains("7"))
            gid = 7;
        if (mylayer.contains("8"))
            gid = 8;
        if (mylayer.contains("9"))
            gid = 9;
        if (mylayer.contains("10"))
            gid = 10;

        return gid; // return top/bottom plates
    }
}
