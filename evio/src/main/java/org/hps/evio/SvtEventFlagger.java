package org.hps.evio;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtMotorPosition;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.HeadBankData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class SvtEventFlagger {

    private static final double angleTolerance = 0.0001;
    SvtBiasConstant.SvtBiasConstantCollection svtBiasConstants = null;
    SvtMotorPosition.SvtMotorPositionCollection svtPositionConstants = null;
    private boolean biasGood = false;
    private boolean positionGood = false;
    private boolean burstmodeNoiseGood = false;
    private double nominalAngleTop = 0;
    private double nominalAngleBottom = 0;

    public void writeFlags(EventHeader event) {
        Date eventDate = getEventTimeStamp(event);
        if (eventDate != null) {
            biasGood = false;
            if (svtBiasConstants != null) {
                SvtBiasConstant biasConstant = svtBiasConstants.find(eventDate);
                if (biasConstant != null) {
                    biasGood = true;
                }
            }

            positionGood = false;
            if (svtPositionConstants != null) {
                SvtMotorPosition positionConstant = svtPositionConstants.find(eventDate);
                if (positionConstant != null) {
//                    System.out.format("%f %f %f %f\n", positionConstant.getBottom(), nominalAngleBottom, positionConstant.getTop(), nominalAngleTop);
                    if (Math.abs(positionConstant.getBottom() - nominalAngleBottom) < angleTolerance && Math.abs(positionConstant.getTop() - nominalAngleTop) < angleTolerance) {
                        positionGood = true;
                    }
                }
            }
        }

        burstmodeNoiseGood = isBurstmodeNoiseGood(event);

        event.getIntegerParameters().put("svt_bias_good", new int[]{biasGood ? 1 : 0});
        event.getIntegerParameters().put("svt_position_good", new int[]{positionGood ? 1 : 0});
        event.getIntegerParameters().put("svt_burstmode_noise_good", new int[]{burstmodeNoiseGood ? 1 : 0});
    }

    private Date getEventTimeStamp(EventHeader event) {
        List<GenericObject> intDataCollection = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject data : intDataCollection) {
            if (AbstractIntData.getTag(data) == HeadBankData.BANK_TAG) {
                Date date = HeadBankData.getDate(data);
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    public void initialize() {
        svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
        svtPositionConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtMotorPosition.SvtMotorPositionCollection.class, "svt_motor_positions").getCachedData();

        SvtAlignmentConstant.SvtAlignmentConstantCollection alignmentConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtAlignmentConstant.SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();

        for (final SvtAlignmentConstant constant : alignmentConstants) {
            switch (constant.getParameter()) {
                case 13100:
//                    System.out.format("nominal top angle: %f\n", constant.getValue());
                    nominalAngleTop = constant.getValue();
                    break;
                case 23100:
//                    System.out.format("nominal bottom angle: %f\n", constant.getValue());
                    nominalAngleBottom = -constant.getValue();
                    break;
            }
        }

    }

    private static boolean isBurstmodeNoiseGood(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
            return countSmallHits(rawTrackerHits) <= 3;
        }
        return false;
    }

    private static int countSmallHits(List<RawTrackerHit> rawHits) {
        int smallHitCount = 0;
        Map<HpsSiSensor, Set<Integer>> hitMap = new HashMap<HpsSiSensor, Set<Integer>>();

        for (RawTrackerHit hit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
            Set<Integer> hitStrips = hitMap.get(sensor);
            if (hitStrips == null) {
                hitStrips = new HashSet<Integer>();
                hitMap.put(sensor, hitStrips);
            }
            int strip = hit.getIdentifierFieldValue("strip");
            hitStrips.add(strip);
        }

        for (RawTrackerHit hit : rawHits) {
            if (isSmallHit(hitMap, hit)) {
                smallHitCount++;
            }
        }
        return smallHitCount;
    }

    private static boolean isSmallHit(Map<HpsSiSensor, Set<Integer>> hitMap, RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int strip = hit.getIdentifierFieldValue("strip");
        double pedestal = sensor.getPedestal(strip, 0);
        double noise = sensor.getNoise(strip, 0);

        if (hitMap.get(sensor) != null && (hitMap.get(sensor).contains(strip - 1) || hitMap.get(sensor).contains(strip + 1))) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (hit.getADCValues()[i] > pedestal + 4.0 * noise) {
                return false;
            }
        }
        return true;
    }
}
