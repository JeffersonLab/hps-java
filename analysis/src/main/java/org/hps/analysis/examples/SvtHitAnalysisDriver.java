package org.hps.analysis.examples;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class SvtHitAnalysisDriver extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree;
    IHistogramFactory histogramFactory;

    //List of Sensors
    private List<HpsSiSensor> sensors = null;

    Map<String, IHistogram1D> nRawHits = new HashMap<String, IHistogram1D>();
//    Map<String, IHistogram2D> adc = new HashMap<String, IHistogram2D>();
//    Map<String, IHistogram2D> adcMonster = new HashMap<String, IHistogram2D>();
//    Map<String, IHistogram1D> channelMonster = new HashMap<String, IHistogram1D>();
//    Map<String, IHistogram1D> channelAboveThresh = new HashMap<String, IHistogram1D>();
//    Map<String, IHistogram1D> channelBelowThresh = new HashMap<String, IHistogram1D>();

    //Histogram Settings
    double minX = 0;
    double maxX = 10;
    double minADC = -1000;
    double maxADC = 5000;
    int nBins = 120;
    int nHitMax = 100;

    //Collection Strings
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    String rawTrackerHitCollectionName = "SVTRawTrackerHits";

    private static final String SUBDETECTOR_NAME = "Tracker";

    public void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        //Set Beam Energy
//        BeamEnergy.BeamEnergyCollection beamEnergyCollection
//                = this.getConditionsManager().getCachedConditions(BeamEnergy.BeamEnergyCollection.class, "beam_energies").getCachedData();
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);

        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        for (HpsSiSensor sensor : sensors) {
            nRawHits.put(sensor.getName(), histogramFactory.createHistogram1D("Raw Hits " + sensor.getName(), 640, 0, 640));
//            adc.put(sensor.getName(), histogramFactory.createHistogram2D("ADC Counts " + sensor.getName(), 6, 0, 6, nBins, minADC, maxADC));
//            adcMonster.put(sensor.getName(), histogramFactory.createHistogram2D("ADC Counts Monster Events " + sensor.getName(), 6, 0, 6, nBins, minADC, maxADC));
//            channelMonster.put(sensor.getName(), histogramFactory.createHistogram1D("Channels Hit Monster Events " + sensor.getName(), 640, 0, 640));
//            channelAboveThresh.put(sensor.getName(), histogramFactory.createHistogram1D("Channels Hit Above Threshold " + sensor.getName(), 640, 0, 640));
//            channelBelowThresh.put(sensor.getName(), histogramFactory.createHistogram1D("Channels Hit Below Threshold " + sensor.getName(), 640, 0, 640));
        }

    }

    public void process(EventHeader event) {
        aida.tree().cd("/");
        setupSensors(event);
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);

        // Map the fitted hits to their corresponding raw hits
        Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        /*for (RawTrackerHit rawHit : rawHits) {
             
            // Access the sensor associated with the raw hit
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
             
            // Retrieve the channel ID of the raw hit
            int channel = rawHit.getIdentifierFieldValue("strip");
        } */
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
        Map<String, Integer> hits = new HashMap<String, Integer>();
        for (HpsSiSensor sensor : sensors) {
            hits.put(sensor.getName(), 0);
        }

        for (RawTrackerHit rawHit : rawHits) {
            // Access the sensor associated with the raw hit
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            Integer nHits = hits.get(sensor.getName());
            if (nHits == null) {
                nHits = 0;
            }
            nHits++;
            hits.put(rawHit.getDetectorElement().getName(), nHits);

            // Get the hit amplitude
            double amplitude = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rawHit));

            aida.histogram1D(sensor.getName() + " amplitude", 250, 0., 5000.).fill(amplitude);
            // Get the t0 of the hit
            double t0 = FittedRawTrackerHit.getT0(fittedRawTrackerHitMap.get(rawHit));
            aida.histogram1D(sensor.getName() + " t0", 250, -400., 100.).fill(t0);

            // Retrieve the channel ID of the raw hit
            int channel = rawHit.getIdentifierFieldValue("strip");
            aida.histogram1D(sensor.getName() + " channel", 640, 0, 640.).fill(channel);

//            short[] adcs = rawHit.getADCValues();
//
//            double[] threshold = new double[6];
//
//            for (int i = 0; i < adcs.length; i++) {
//                adc.get(sensor.getName()).fill(i, adcs[i] - sensor.getPedestal(channel, i));
//                threshold[i] = adcs[i] - sensor.getPedestal(channel, i) - 2 * sensor.getNoise(channel, i);
//            }
//
//            boolean sample1 = threshold[0] > 0 && threshold[1] > 0 && threshold[2] > 0;
//            boolean sample2 = threshold[1] > 0 && threshold[2] > 0 && threshold[3] > 0;
//            boolean sample3 = threshold[2] > 0 && threshold[3] > 0 && threshold[4] > 0;
//            boolean sample4 = threshold[3] > 0 && threshold[4] > 0 && threshold[5] > 0;
//
//            if (sample1 || sample2 || sample3 || sample4) {
//                channelAboveThresh.get(sensor.getName()).fill(channel);
//            } else {
//                channelBelowThresh.get(sensor.getName()).fill(channel);
//            }
        }
        for (HpsSiSensor sensor : sensors) {
            Integer nHits = hits.get(sensor.getName());
            if (nHits == null) {
                nRawHits.get(sensor.getName()).fill(0);
            } else {
                nRawHits.get(sensor.getName()).fill(nHits);
                if (nHits > nHitMax) {
                    for (RawTrackerHit rawHit : rawHits) {
                        if (!sensor.equals(rawHit.getDetectorElement())) {
                            continue;
                        }
                        int channel = rawHit.getIdentifierFieldValue("strip");
//                        short[] adcs = rawHit.getADCValues();
//                        for (int i = 0; i < adcs.length; i++) {
//                            adcMonster.get(sensor.getName()).fill(i, adcs[i] - sensor.getPedestal(channel, i));
//                        }
//                        channelMonster.get(sensor.getName()).fill(channel);
                    }
                }
            }
        }
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }
}
