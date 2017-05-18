package org.lcsim.detector.tracker.silicon;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;

/**
 * This class extends {@link SiSensor} with conditions specific to HPS SVT
 * half-modules (sensors) used during the engineering run and beyond. Each
 * half-module is uniquely identified by a FEB ID/Hybrid ID pair which is then
 * related to calibration conditions such as baseline, noise, gain etc.
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class HpsSiSensor extends SiSensor {

    // --------------//
    //   Constants   //
    // --------------//

    public final static int NUMBER_OF_SAMPLES = 6;
    private final static int NUMBER_OF_SHAPE_FIT_PARAMETERS = 4;

    public final static int AMPLITUDE_INDEX = 0;
    public final static int T0_INDEX = 1;
    public final static int TP_INDEX = 2;

    public final static String ELECTRON_SIDE = "ELECTRON";
    public final static String POSITRON_SIDE = "POSITRON";

    //-----------------------//
    //   Sensor properties   //
    //-----------------------//

    protected int febID;
    protected int febHybridID;
    protected double t0Shift = 0;
    protected boolean isAxial = false;
    protected boolean isStereo = false;

    private final double readoutStripCapacitanceIntercept = 0;
    private final double readoutStripCapacitanceSlope = 0.16; // pf/mm
    private final double senseStripCapacitanceIntercept = 0;
    private final double senseStripCapacitanceSlope = 0.16; // pf/mm

    /*
     * Adding separate strip capacitance for long detectors following
     * S/N = mip_charge/(270e- + 36*C[pf/cm]*L[cm] e.g. for expected S/N=16
     * and L=20cm -> C=0.1708pf/mm e.g. for expected S/N=8 and
     * L=20cm -> C=0.39pf/mm FIXME: This should be taken into account by the
     * noise model.
     */
    protected double longSensorLengthThreshold = 190.0; // mm
    protected double readoutLongStripCapacitanceSlope = 0.39; // pf/mm
    protected double senseLongStripCapacitanceSlope = 0.39; // pf/mm

    // --------------------//
    //   Conditions Maps   //
    // --------------------//
    protected Map<Integer, double[]> pedestalMap = new HashMap<Integer, double[]>();
    protected Map<Integer, double[]> noiseMap = new HashMap<Integer, double[]>();
    protected Map<Integer, Double> gainMap = new HashMap<Integer, Double>();
    protected Map<Integer, Double> offsetMap = new HashMap<Integer, Double>();
    protected Map<Integer, double[]> shapeFitParametersMap = new HashMap<Integer, double[]>();
    protected Set<Integer> badChannels = new HashSet<Integer>();
    protected int millepedeId = -1;

    /**
     * This class constructor matches the signature of <code>SiSensor</code>.
     *
     * @param sensorid The sensor ID.
     * @param name The name of the sensor.
     * @param parent The parent DetectorElement.
     * @param support The physical support path.
     * @param id The identifier of the sensor.
     */
    public HpsSiSensor(final int sensorid, final String name, final IDetectorElement parent, final String support,
            final IIdentifier id) {
        super(sensorid, name, parent, support, id);

        // Set the default sensor orientation using the layer number. For sensors
        // belonging to the top volume, odd (even) layers are axial (stereo).
        // For sensors belonging to the bottom, even (odd) layers are axial (stereo).
        if (this.isTopLayer() && this.getLayerNumber() % 2 == 1) {
            this.setAxial(true);
        } else if (this.isBottomLayer() && this.getLayerNumber() % 2 == 0) {
            this.setAxial(true);
        } else {
            this.setStereo(true);
        }

        this.initialize();
    }

    /**
     * Get whether this sensor is in the top half of the detector. Modules in the top half have module numbers of 0 or
     * 2.
     *
     * @return True if sensor is in top layer; false if not.
     */
    public boolean isTopLayer() {
        return getModuleNumber() % 2 == 0;
    }

    /**
     * Get whether this sensor is in the bottom half of the detector. Modules in the bottom half have module numbers of
     * 1 or 3.
     *
     * @return True if sensor is in bottom layer; false if not.
     */
    public boolean isBottomLayer() {
        return getModuleNumber() % 2 != 0;
    }

    /**
     * Get the module number of the sensor.
     *
     * @return The module number of the sensor.
     */
    public int getModuleNumber() {
        return getTrackerIdHelper().getModuleValue(getIdentifier());
    }

    /**
     * Get the specific type of identifier helper for this component.
     *
     * @return The identifier helper.
     */
    public SiTrackerIdentifierHelper getTrackerIdHelper() {
        return (SiTrackerIdentifierHelper) getIdentifierHelper();
    }

    /**
     * Get whether this sensor is axial.
     *
     * @return True if sensor is axial; false if not.
     */
    public boolean isAxial() {
        return this.isAxial;
    }

    /**
     * Get whether this sensor is stereo.
     *
     * @return True is sensor is stereo; false if not.
     */
    public boolean isStereo() {
        return this.isStereo;
    }

    /**
     * Get the pedestal for the given channel and sample number.
     *
     * @param channel The channel number.
     * @param sample The sample number.
     * @return The pedestal value for the given channel and sample or null if not set.
     */
    public Double getPedestal(final int channel, final int sample) {
        if (sample >= NUMBER_OF_SAMPLES) {
            throw new RuntimeException("The sample number must be less than " + NUMBER_OF_SAMPLES);
        }
        return this.pedestalMap.get(channel)[sample];
    }

    /**
     * Get the noise for the given channel and sample number.
     *
     * @param channel The channel number.
     * @param sample The sample number.
     * @return The noise value for the given channel and sample or null if not set.
     */
    public Double getNoise(final int channel, final int sample) {
        if (sample >= NUMBER_OF_SAMPLES) {
            throw new RuntimeException("The sample number must be less than " + NUMBER_OF_SAMPLES);
        }
        return this.noiseMap.get(channel)[sample];
    }

    /**
     * Get the gain for the given channel.
     *
     * @param channel The channel number.
     * @return The gain value for the channel or null if not set.
     */
    public Double getGain(final int channel) {
        return this.gainMap.get(channel);
    }

    /**
     * Get the offset for the given channel.
     *
     * @param channel The channel number.
     * @return The offset for the channel or null if not set.
     */
    public Double getOffset(final int channel) {
        return this.offsetMap.get(channel);
    }

    /** @return The charge transfer efficiency of the readout strips. */
    public double getReadoutTransferEfficiency() {
        return 0.986;
    }

    /** @return The charge transfer efficiency of the sense strips. */
    public double getSenseTransferEfficiency() {
        return 0.419;
    }

    /**
     * Get the shape fit parameters (amplitude, t0, tp) associated with a given channel.
     *
     * @param channel The channel number.
     * @return The shape fit results for the channel.
     */
    public double[] getShapeFitParameters(final int channel) {
        return this.shapeFitParametersMap.get(channel);
    }

    /**
     * Get whether the given channel is bad or not.
     *
     * @param channel The channel number.
     * @return True if channel is bad; false if not.
     */
    public boolean isBadChannel(final int channel) {
        return this.badChannels.contains(channel);
    }

    /**
     * Get the total number of channels in the sensor.
     *
     * @return The total number of channels in the sensor.
     */
    public int getNumberOfChannels() {
        return this.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells();
    }

    /** @return The total number of sense strips per sensor. */
    public int getNumberOfSenseStrips() {
        return 1277;
    }

    /**
     * Get whether the given channel number if valid.
     *
     * @param channel The channel number.
     * @return True if channel number is valid; false if not.
     */
    public boolean isValidChannel(final int channel) {
        return this.getNumberOfChannels() >= 0 && channel < this.getNumberOfChannels();
    }

    /**
     * Get the front end board (FEB) ID associated with this sensor.
     *
     * @return The FEB ID
     */
    public int getFebID() {
        return this.febID;
    }

    /**
     * Get the FEB hybrid ID of the sensor.
     *
     * @return The FEB hybrid number of the sensor.
     */
    public int getFebHybridID() {
        return this.febHybridID;
    }

    /**
     * Get the layer number of the sensor.
     *
     * @return The layer number of the sensor.
     */
    public int getLayerNumber() {
        return getIdentifierHelper().getValue(getIdentifier(), "layer");
    }

    /**
     * Get the t0 shift for this sensor.
     *
     * @return The t0 shift for this sensor.
     */
    public double getT0Shift() {
        return this.t0Shift;
    }

    /**
     * Get the sensor side (ELECTRON or POSITRON). For single sensor half-modules, the side will always be ELECTRON.
     *
     * @return The side the sensor is on (ELECTRON or POSITRON)
     */
    public String getSide() {
        return this.getModuleNumber() < 2 ? ELECTRON_SIDE : POSITRON_SIDE;
    }

    /** @return The readout strip pitch. */
    public double getReadoutStripPitch() {
        return 0.060; // mm
    }

    /** @return The sense strip pitch. */
    public double getSenseStripPitch() {
        return 0.030; // mm
    }

    /**
     * Generate an ID for a channel (strip) on a sensor.
     *
     * @param channel : Physical channel number
     * @return the channel ID
     */
    public long makeChannelID(final int channel) {
        final int sideNumber = this.hasElectrodesOnSide(ChargeCarrier.HOLE) ? ChargeCarrier.HOLE.charge()
                : ChargeCarrier.ELECTRON.charge();
        return this.makeStripId(channel, sideNumber).getValue();
    }

    /**
     * Set the pedestal value for all samples for a given channel.
     *
     * @param channel The channel number.
     * @param pedestal The pedestal values for all samples.
     */
    public void setPedestal(final int channel, final double[] pedestal) {
        if (pedestal.length > NUMBER_OF_SAMPLES) {
            throw new RuntimeException("The number of pedestal samples must be equal to" + NUMBER_OF_SAMPLES);
        }
        this.pedestalMap.put(channel, pedestal);
    }

    /**
     * Set the noise value for the given channel.
     *
     * @param channel The channel number.
     * @param noise The noise values for all samples.
     */
    public void setNoise(final int channel, final double[] noise) {
        if (noise.length > NUMBER_OF_SAMPLES) {
            throw new RuntimeException("The number of pedestal samples must be equal to" + NUMBER_OF_SAMPLES);
        }
        this.noiseMap.put(channel, noise);
    }

    /**
     * Set the gain value for the given channel.
     *
     * @param channel The channel number.
     * @param gain The gain value.
     */
    public void setGain(final int channel, final double gain) {
        this.gainMap.put(channel, gain);
    }

    /**
     * Set the offset for the given channel.
     *
     * @param channel The channel number.
     * @param offset The offset value.
     */
    public void setOffset(final int channel, final double offset) {
        this.offsetMap.put(channel, offset);
    }

    /**
     * Set the shape fit results for the given channel.
     *
     * @param channel The channel number.
     * @param shapeFitParameters The shape fit results array (should be length 4).
     */
    public void setShapeFitParameters(final int channel, final double[] shapeFitParameters) {
        if (shapeFitParameters.length != NUMBER_OF_SHAPE_FIT_PARAMETERS) {
            throw new IllegalArgumentException("Number of shape fit parameters is incorrect: "
                    + shapeFitParameters.length);
        }
        this.shapeFitParametersMap.put(channel, shapeFitParameters);
    }

    /**
     * Flag the given channel as bad.
     *
     * @param channel The channel number.
     */
    public void setBadChannel(final int channel) {
        this.badChannels.add(channel);
    }

    /**
     * Set the front end board (FEB) ID of the sensor.
     *
     * @param febID FEB ID The FEB ID of the sensor.
     */
    public void setFebID(final int febID) {
        this.febID = febID;
    }

    /**
     * Set the FEB hybrid ID of the sensor.
     *
     * @param febHybridID FEB hybrid ID The FEB hybrid ID.
     */
    public void setFebHybridID(final int febHybridID) {
        this.febHybridID = febHybridID;
    }

    /**
     * Set the t0 shift for this sensor.
     *
     * @param t0Shift The t0 shift for this sensor.
     */
    public void setT0Shift(final double t0Shift) {
        this.t0Shift = t0Shift;
    }

    /**
     * Flag the sensor as being axial.
     *
     * @param isAxial true if the sensor is Axial, false otherwise
     */
    public void setAxial(final boolean isAxial) {
        this.isAxial = isAxial;
    }

    /**
     * Flag the sensor as being stereo
     *
     * @param isStereo true is the sensor is stereo, false otherwise
     */
    public void setStereo(final boolean isStereo) {
        this.isStereo = isStereo;
    }

    /**
     * Reset the time dependent conditions data of this sensor. This does NOT reset the sensor setup information, which
     * is assumed to be fixed once it is setup for a given session.
     */
    public void reset() {
        this.pedestalMap.clear();
        this.noiseMap.clear();
        this.offsetMap.clear();
        this.shapeFitParametersMap.clear();
        this.badChannels.clear();
        this.gainMap.clear();
        this.t0Shift = 0;
    }

    @Override
    public String toString() {

        final StringBuffer buffer = new StringBuffer();
        buffer.append("HpsSiSensor: " + this.getName());
        buffer.append("\n");
        buffer.append("----------------------------------");
        buffer.append("\n");
        buffer.append("Feb ID: " + this.getFebID() + "\n");
        buffer.append("Feb Hybrid ID: " + this.getFebHybridID() + "\n");
        buffer.append("Layer: " + this.getLayerNumber() + "\n");
        buffer.append("Module: " + this.getModuleNumber() + "\n");
        buffer.append("Number of readout strips: " + this.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells() + "\n");
        buffer.append("Number of sense strips: " + this.getSenseElectrodes(ChargeCarrier.HOLE).getNCells() + "\n");
        buffer.append("Strip length: " + this.getStripLength() + "\n");
        buffer.append("----------------------------------");

        return buffer.toString();
    }

    /**
     * Setup the geometry and electrical characteristics of an {@link HpsSiSensor}
     */
    @Override
    public void initialize() {

        // Get the solid corresponding to the sensor volume
        final Box sensorSolid = (Box) this.getGeometry().getLogicalVolume().getSolid();

        // Get the faces of the solid corresponding to the n and p sides of the sensor
        final Polygon3D pSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
        final Polygon3D nSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, -1)).get(0);

        // p side collects holes.
        this.setBiasSurface(ChargeCarrier.HOLE, pSide);

        // n side collects electrons.
        this.setBiasSurface(ChargeCarrier.ELECTRON, nSide);

        // Translate to the outside of the sensor solid in order to setup the electrodes
        final ITranslation3D electrodesPosition = new Translation3D(VecOp.mult(-pSide.getDistance(), pSide.getNormal()));

        // Align the strips with the edge of the sensor.
        final IRotation3D electrodesRotation = new RotationPassiveXYZ(0, 0, 0);
        final Transform3D electrodesTransform = new Transform3D(electrodesPosition, electrodesRotation);

        // Set the number of readout and sense electrodes.
        final SiStrips readoutElectrodes = new SiStrips(ChargeCarrier.HOLE, getReadoutStripPitch(), this,
                electrodesTransform);
        final SiStrips senseElectrodes = new SiStrips(ChargeCarrier.HOLE, getSenseStripPitch(),
                this.getNumberOfSenseStrips(), this, electrodesTransform);

        final double readoutCapacitance = this.getStripLength() > this.longSensorLengthThreshold ? this.readoutLongStripCapacitanceSlope
                : this.readoutStripCapacitanceSlope;
        final double senseCapacitance = this.getStripLength() > this.longSensorLengthThreshold ? this.senseLongStripCapacitanceSlope
                : this.senseStripCapacitanceSlope;

        // Set the strip capacitance.
        readoutElectrodes.setCapacitanceIntercept(this.readoutStripCapacitanceIntercept);
        readoutElectrodes.setCapacitanceSlope(readoutCapacitance);
        senseElectrodes.setCapacitanceIntercept(this.senseStripCapacitanceIntercept);
        senseElectrodes.setCapacitanceSlope(senseCapacitance);

        // Set sense and readout electrodes.
        this.setSenseElectrodes(senseElectrodes);
        this.setReadoutElectrodes(readoutElectrodes);

        // Set the charge transfer efficiency of both the sense and readout
        // strips.
        final double[][] transferEfficiencies = {
          {this.getReadoutTransferEfficiency(), this.getSenseTransferEfficiency()}
        };
        this.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));

    }

    /**
     * Return the length of an {@link HpsSiSensor} strip. This is done by getting the face of the {@link HpsSiSensor}
     * and returning the length of the longest edge.
     *
     * @return The length of the longest {@link HpsSiSensor} edge
     */
    protected double getStripLength() {

        double length = 0;

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) this.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {

            // Loop through the edges of the sensor face and find the longest
            // one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }

    /**
     * Set the sensor id used by millepede.
     *
     * @param id - millepede sensor id
     */
    public void setMillepedeId(final int id) {
        this.millepedeId = id;
    }

    /**
     * Get the sensor id used by millepede.
     *
     * @return the millepede sensor id.
     */
    public int getMillepedeId() {
        return this.millepedeId;
    }
}
