package org.hps.evio;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtMotorPosition;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.HeadBankData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;

public class SvtEventFlagger {

    private static final double angleTolerance = 0.0001;
    SvtBiasConstant.SvtBiasConstantCollection svtBiasConstants = null;
    SvtMotorPosition.SvtMotorPositionCollection svtPositionConstants = null;
    private SvtTimingConstants svtTimingConstants = null;
    private boolean biasGood = false;
    private boolean positionGood = false;
    private boolean burstmodeNoiseGood = false;
    private boolean latencyGood = false;
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

        latencyGood = false;
        if (svtTimingConstants != null) {
            if (svtTimingConstants.getOffsetTime() <= 27) {
                latencyGood = true;
            } else {
                if (((event.getTimeStamp() - 4 * svtTimingConstants.getOffsetPhase()) % 24) < 16) {
                    latencyGood = true;
                }
            }
//        System.out.format("%f %b\n", svtTimingConstants.getOffsetTime() + (((event.getTimeStamp() - 4 * svtTimingConstants.getOffsetPhase()) % 24) - 12), latencyGood);
        }

        burstmodeNoiseGood = isBurstmodeNoiseGood(event);

        event.getIntegerParameters().put("svt_bias_good", new int[]{biasGood ? 1 : 0});
        event.getIntegerParameters().put("svt_position_good", new int[]{positionGood ? 1 : 0});
        event.getIntegerParameters().put("svt_burstmode_noise_good", new int[]{burstmodeNoiseGood ? 1 : 0});
        event.getIntegerParameters().put("svt_latency_good", new int[]{latencyGood ? 1 : 0});
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
        try {
            svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
        } catch (Exception e) {
            svtBiasConstants = null;
        }
        try {
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
        } catch (Exception e) {
            svtPositionConstants = null;
        }

        try {
            svtTimingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
        } catch (Exception e) {
            svtTimingConstants = null;
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

    public static void voidAddHeaderCheckResultToMetaData(boolean ok, EventHeader lcsimEvent) {
        //System.out.println("adding svt header check ");
        lcsimEvent.getIntegerParameters().put("svt_event_header_good", new int[]{ok ? 1 : 0});
        //if(lcsimEvent.hasItem("svt_event_header_good"))
        //        System.out.println("event header has the svt header check ");
        //else
        //    System.out.println("event header doesn't have the svt header check ");
    }

    public static void AddHeaderInfoToMetaData(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        int[] svtHeaders = new int[headers.size()];
        int[] svtTails = new int[headers.size()];
        for (int iSvtHeader = 0; iSvtHeader < headers.size(); ++iSvtHeader) {
            svtHeaders[iSvtHeader] = headers.get(iSvtHeader).getHeader();
            svtTails[iSvtHeader] = headers.get(iSvtHeader).getTail();

            lcsimEvent.getIntegerParameters().put("svt_event_header_roc" + headers.get(iSvtHeader).getNum(), new int[]{headers.get(iSvtHeader).getHeader()});
            lcsimEvent.getIntegerParameters().put("svt_event_tail_roc" + headers.get(iSvtHeader).getNum(), new int[]{headers.get(iSvtHeader).getTail()});

            int nMS = headers.get(iSvtHeader).getNumberOfMultisampleHeaders();
            int[] multisampleHeadersArray = new int[4 * nMS];
            for (int iMS = 0; iMS < nMS; ++iMS) {
                int[] multisampleHeader = headers.get(iSvtHeader).getMultisampleHeader(iMS);
                System.arraycopy(multisampleHeader, 0, multisampleHeadersArray, iMS * 4, multisampleHeader.length);
            }
            lcsimEvent.getIntegerParameters().put("svt_multisample_headers_roc" + headers.get(iSvtHeader).getNum(), multisampleHeadersArray);
        }

    }

    private static final Pattern rocIdPattern = Pattern.compile("svt_.*_roc(\\d+)");

    public static int getRocFromSvtHeaderName(String seq) {
        Matcher m = rocIdPattern.matcher(seq);
        if (m == null) {
            throw new RuntimeException("null matcher, don't think this should happen");
        }
        if (!m.matches()) {
            return -1;
        } else {
            return Integer.parseInt(m.group(1));
        }
    }

    public static List<SvtHeaderDataInfo> getHeaderInfoToMetaData(EventHeader lcsimEvent) {
        Map<Integer, Integer> headers = new HashMap<Integer, Integer>();
        Map<Integer, Integer> tails = new HashMap<Integer, Integer>();
        Map<Integer, Integer[]> multisampleHeaders = new HashMap<Integer, Integer[]>();

        for (Map.Entry<String, int[]> entry : lcsimEvent.getIntegerParameters().entrySet()) {

            int roc = getRocFromSvtHeaderName(entry.getKey());

            if (roc == -1) {
                continue;
            }
            //LOGGER.logger.fine("processing entry \"" + entry.getKey()+ "\"" + " for roc "  + roc);
            int[] value = entry.getValue();

            if (entry.getKey().contains("svt_event_header_roc")) {
                headers.put(roc, value[0]);
            }

            if (entry.getKey().contains("svt_event_tail_roc")) {
                tails.put(roc, value[0]);
            }

            // really need to copy?
            if (entry.getKey().contains("svt_multisample_headers_roc")) {
                Integer[] tmp = ArrayUtils.toObject(value); //new Integer[value.length];
                multisampleHeaders.put(roc, tmp);
            }

        }

        // create the new objects
        List<SvtHeaderDataInfo> headerDataInfo = new ArrayList<SvtHeaderDataInfo>();
        for (Integer roc : headers.keySet()) {
            int header = headers.get(roc);
            int tail = tails.get(roc);
            Integer[] ms = multisampleHeaders.get(roc);
            headerDataInfo.add(new SvtHeaderDataInfo(roc, header, tail, ms));
        }

        return headerDataInfo;

    }

}
