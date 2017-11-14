package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;

/**
 * Utility class, for evaluating acceptances of layers and sensors.
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @version $id: v1 05/30/2017$
 */

public class AcceptanceHelper {
    private Map<Integer, List<SvtStereoLayer>> StereoLayersMapBottom;
    private Map<Integer, List<SvtStereoLayer>> StereoLayersMapTop;
    private Map<SiSensor, Map<Integer, Hep3Vector>> StripPositionsMap;
    private TrackerHitUtils trackerHitUtils;
    private List<HpsSiSensor> sensors;

    /**
     * Default constructor
     */
    public AcceptanceHelper() {
        StereoLayersMapTop = new HashMap<Integer, List<SvtStereoLayer>>();
        StereoLayersMapBottom = new HashMap<Integer, List<SvtStereoLayer>>();
        StripPositionsMap = new HashMap<SiSensor, Map<Integer, Hep3Vector>>();
        trackerHitUtils = new TrackerHitUtils();
    }

    /**
     * Initialization: call me from detectorChanged method of driver
     *
     * @param Detector
     * @param SubdetectorName
     */
    public void initializeMaps(Detector det, String SubdetectorName) {

        List<SvtStereoLayer> stereoLayers = ((HpsTracker2) det.getSubdetector(
                SubdetectorName).getDetectorElement()).getStereoPairs();

        for (SvtStereoLayer stereoLayer : stereoLayers) {
            int layer = stereoLayer.getLayerNumber(); // returns 1 to 6

            if (stereoLayer.getAxialSensor().isTopLayer()) {
                if (!StereoLayersMapTop.containsKey(layer)) {
                    StereoLayersMapTop.put(layer,
                            new ArrayList<SvtStereoLayer>());
                    // System.out.format("new layer %d \n", layer);
                }
                StereoLayersMapTop.get(layer).add(stereoLayer);
            } else {
                if (!StereoLayersMapBottom.containsKey(layer)) {
                    StereoLayersMapBottom.put(layer,
                            new ArrayList<SvtStereoLayer>());
                    // System.out.format("new layer %d \n", layer);
                }
                StereoLayersMapBottom.get(layer).add(stereoLayer);
            }

        }

        sensors = det.getSubdetector(SubdetectorName).getDetectorElement()
                .findDescendants(HpsSiSensor.class);
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
            for (SiSensor sensor : sensors) {
                if (sensor.hasElectrodesOnSide(carrier)) {
                    StripPositionsMap.put(sensor,
                            new HashMap<Integer, Hep3Vector>());
                    SiStrips strips = (SiStrips) sensor
                            .getReadoutElectrodes(carrier);
                    ITransform3D parentToLocal = sensor.getReadoutElectrodes(
                            carrier).getParentToLocal();
                    for (int physicalChannel = 0; physicalChannel < 640; physicalChannel++) {
                        Hep3Vector localStripPosition = strips
                                .getCellPosition(physicalChannel);
                        Hep3Vector stripPosition = parentToLocal
                                .transformed(localStripPosition);
                        StripPositionsMap.get(sensor).put(physicalChannel,
                                stripPosition);

                    }
                }
            }
        }

    }

    /**
     * Determines whether a given track would pass through a given layer's
     * acceptance
     *
     * @param Track
     * @param LayerNumber
     * @return yes/no
     */
    protected boolean isWithinAcceptance(Track trk, int layer) {
        boolean debug = false;

        List<SvtStereoLayer> stereoLayers;

        if (trk.getTrackParameter(4) > 0)
            stereoLayers = StereoLayersMapTop.get(layer);
        else
            stereoLayers = StereoLayersMapBottom.get(layer);

        for (SvtStereoLayer sLayer : stereoLayers) {
            HpsSiSensor axialSensor = sLayer.getAxialSensor();
            HpsSiSensor stereoSensor = sLayer.getStereoSensor();

            Hep3Vector axialSensorPosition = axialSensor.getGeometry()
                    .getPosition();
            Hep3Vector axialTrackPos = TrackUtils.extrapolateTrack(trk,
                    axialSensorPosition.z());
            Hep3Vector stereoSensorPosition = stereoSensor.getGeometry()
                    .getPosition();
            Hep3Vector stereoTrackPos = TrackUtils.extrapolateTrack(trk,
                    stereoSensorPosition.z());

            if (TrackUtils.sensorContainsTrack(axialTrackPos, axialSensor)) {

                if (debug) {
                    System.out
                            .format("sensorContainsTrack found layer %d with trackParams",
                                    layer);
                    for (int i = 0; i < 5; i++)
                        System.out.format(" %f ", trk.getTrackParameter(i));
                    System.out.format("\n");
                }

                if (TrackUtils
                        .sensorContainsTrack(stereoTrackPos, stereoSensor)) {

                    // track intersecting bad channel is considered outside
                    // acceptance
                    int intersectingChannel = findIntersectingChannel(
                            axialTrackPos, axialSensor);
                    if (intersectingChannel == 0 || intersectingChannel == 638)
                        return false;
                    if (axialSensor.isBadChannel(intersectingChannel)
                            || axialSensor
                                    .isBadChannel(intersectingChannel + 1)
                            || axialSensor
                                    .isBadChannel(intersectingChannel - 1))
                        return false;

                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Determines which physical channel (0-639) in a given sensor a track would
     * hit
     *
     * @param TrackPosition
     *            extrapolated to z position of sensor
     * @param sensor
     * @return PhysicalChannelNumber
     */
    public int findIntersectingChannel(Hep3Vector trackPosition, SiSensor sensor) {
        double firstXchannel = StripPositionsMap.get(sensor).get(0).x();
        double deltaXchannel = StripPositionsMap.get(sensor).get(1).x()
                - firstXchannel;

        Hep3Vector trackPositionDet = VecOp.mult(
                VecOp.inverse(this.trackerHitUtils.detToTrackRotationMatrix()),
                trackPosition);

        ITransform3D globalToLocal = sensor.getReadoutElectrodes(
                ChargeCarrier.HOLE).getGlobalToLocal();
        globalToLocal.transform(trackPositionDet);

        // Find the closest channel to the track position
        /*
         * double deltaY = Double.MAX_VALUE; int intersectingChannel = 0; for
         * (int physicalChannel = 0; physicalChannel < 639; physicalChannel++) {
         * if (Math.abs(trackPositionDet.x() -
         * StripPositionsMap.get(sensor).get(physicalChannel).x()) < deltaY) {
         * deltaY = Math.abs(trackPositionDet.x() -
         * StripPositionsMap.get(sensor).get(physicalChannel) .x());
         * intersectingChannel = physicalChannel; } }
         */
        int channelNum = (int) Math
                .round(Math.abs((trackPositionDet.x() - firstXchannel)
                        / deltaXchannel));
        if (channelNum > 639)
            channelNum = 639;

        return channelNum;

    }

}
