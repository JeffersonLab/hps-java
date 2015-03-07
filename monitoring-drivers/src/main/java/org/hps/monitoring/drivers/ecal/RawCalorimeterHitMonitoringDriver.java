package org.hps.monitoring.drivers.ecal;

import static org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.EXTRA_DATA_RELATIONS_NAME;
import static org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.INTEGRAL_WINDOW;
import static org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.createChannelHistogram2D;
import static org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.createChannelProfile2D;
import static org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.plot;
import hep.aida.ICloud1D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotterFactory;
import hep.aida.IProfile2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.HitExtraData;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This is a set of detailed plots for monitoring ECAL raw data collections in integral mode (modes 3 and 7).
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class RawCalorimeterHitMonitoringDriver extends Driver {

    ICloud1D pedSubAmplitudeC1D;
    IHistogram1D amplitudeH1D;
    ICloud1D timestampC1D;
    ICloud1D amplitudeLowC1D;
    ICloud1D amplitudeHighC1D;
    IHistogram1D hitsPerEventH1D;
    IHistogram2D hitMapH2D;
    IHistogram2D occupancyH2D;
    IProfile2D amplitudeHighP2D;
    IProfile2D amplitudeLowP2D;
    IProfile2D timestampP2D;

    Map<Long, Integer> occupancyMap = new HashMap<Long, Integer>();
    int accumulatedEvents = 0;
    long lastUpdatedMillis = 0;
    long occupancyUpdateMillis = 5000; // Refresh occupancy plots every 5 seconds.

    AIDA aida = AIDA.defaultInstance();

    EcalConditions conditions;
    
    String rawCalorimeterHitCollectionName = "EcalReadoutHits";
    Subdetector subdetector;
    IIdentifierHelper helper;

    public void setOccupancyUpdateMillis(long occupancyUpdateMillis) {
        this.occupancyUpdateMillis = occupancyUpdateMillis;
    }

    public void setRawCalorimeterHitCollectionName(String rawCalorimeterHitCollectionName) {
        this.rawCalorimeterHitCollectionName = rawCalorimeterHitCollectionName;
    }

    public void detectorChanged(Detector detector) {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        conditions = manager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        subdetector = detector.getSubdetector("Ecal");
        helper = subdetector.getDetectorElement().getIdentifierHelper();
    }

    public void startOfData() {
        pedSubAmplitudeC1D = aida.cloud1D("Pedestal Sub Amplitude");
        amplitudeH1D = aida.histogram1D("Amplitude", 150, 0.5, 14999.5);
        timestampC1D = aida.cloud1D("Timestamp");
        amplitudeLowC1D = aida.cloud1D("Mode 7 Amplitude Low");
        amplitudeHighC1D = aida.cloud1D("Mode 7 Amplitude High");
        hitsPerEventH1D = aida.histogram1D("Hits Per Event", 100, -0.5, 99.5);
        hitMapH2D = createChannelHistogram2D("Hit Map");
        occupancyH2D = createChannelHistogram2D("Hit Occupancy");
        amplitudeHighP2D = createChannelProfile2D("Mode 7 Average Amplitude High");
        amplitudeLowP2D = createChannelProfile2D("Mode 7 Average Amplitude Low");
        timestampP2D = createChannelProfile2D("Average Timestamp");

        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("ECAL - " + rawCalorimeterHitCollectionName);

        plot(plotterFactory, pedSubAmplitudeC1D, null, true);
        plot(plotterFactory, amplitudeH1D, null, true);
        plot(plotterFactory, timestampC1D, null, true);
        plot(plotterFactory, amplitudeLowC1D, null, true);
        plot(plotterFactory, amplitudeHighC1D, null, true);
        plot(plotterFactory, hitsPerEventH1D, null, true);
        plot(plotterFactory, hitMapH2D, null, true);
        plot(plotterFactory, occupancyH2D, null, true);
        plot(plotterFactory, amplitudeHighP2D, null, true);
        plot(plotterFactory, amplitudeLowP2D, null, true);
        plot(plotterFactory, timestampP2D, null, true);

        lastUpdatedMillis = System.currentTimeMillis();
    }

    public void process(EventHeader event) {
                
        if (event.hasCollection(RawCalorimeterHit.class, rawCalorimeterHitCollectionName)) {

            List<RawCalorimeterHit> rawHits = event.get(RawCalorimeterHit.class, rawCalorimeterHitCollectionName);

            /*
             * Do basic plots of RawCalorimeterHit data.
             */
            hitsPerEventH1D.fill(rawHits.size());
            Set<RawCalorimeterHit> uniqueRawHits = new HashSet<RawCalorimeterHit>();
            for (RawCalorimeterHit rawHit : rawHits) {
                amplitudeH1D.fill(rawHit.getAmplitude());
                timestampC1D.fill(rawHit.getTimeStamp());
                EcalChannel channel = conditions.getChannelCollection().findGeometric(rawHit.getCellID());
                EcalChannelConstants constants = conditions.getChannelConstants(channel);
                pedSubAmplitudeC1D.fill(rawHit.getAmplitude() - (constants.getCalibration().getPedestal() * INTEGRAL_WINDOW));

                int ix = helper.getValue(new Identifier(rawHit.getCellID()), "ix");
                int iy = helper.getValue(new Identifier(rawHit.getCellID()), "iy");
                hitMapH2D.fill(ix, iy);

                timestampP2D.fill(ix, iy, rawHit.getTimeStamp());

                uniqueRawHits.add(rawHit);
            }

            /*
             * Update and plot occupancies based on unique hits in the event.
             */
            for (RawCalorimeterHit rawHit : uniqueRawHits) {

                // Add entry if does not exist.
                if (occupancyMap.get(rawHit.getCellID()) == null) {
                    occupancyMap.put(rawHit.getCellID(), 0);
                }

                // Increment hit count by one for this hit.
                int nHits = occupancyMap.get(rawHit.getCellID()) + 1;
                occupancyMap.put(rawHit.getCellID(), nHits);
            }
            ++accumulatedEvents;
            long elapsed = System.currentTimeMillis() - lastUpdatedMillis;
            if (elapsed >= occupancyUpdateMillis) {
                occupancyH2D.reset();
                for (Entry<Long, Integer> entry : occupancyMap.entrySet()) {
                    IIdentifier hitId = new Identifier(entry.getKey());
                    int ix = helper.getValue(hitId, "ix");
                    int iy = helper.getValue(hitId, "iy");
                    occupancyH2D.fill(ix, iy, (double) entry.getValue() / (double) accumulatedEvents);
                }
                lastUpdatedMillis = System.currentTimeMillis();
                accumulatedEvents = 0;
                occupancyMap.clear();
            }

            /*
             * Plot from mode 7 data if it exists.
             */
            if (event.hasCollection(LCRelation.class, EXTRA_DATA_RELATIONS_NAME)) {
                List<LCRelation> extraDataRelations = event.get(LCRelation.class, EXTRA_DATA_RELATIONS_NAME);
                if (extraDataRelations != null) {
                    for (LCRelation rel : event.get(LCRelation.class, EXTRA_DATA_RELATIONS_NAME)) {
                        GenericObject extraData = (GenericObject) rel.getTo();
                        if (extraData instanceof HitExtraData.Mode7Data) {
                            HitExtraData.Mode7Data mode7Data = (HitExtraData.Mode7Data) extraData;
                            amplitudeHighC1D.fill(mode7Data.getAmplHigh());
                            amplitudeLowC1D.fill(mode7Data.getAmplLow());

                            RawCalorimeterHit rawHit = (RawCalorimeterHit) rel.getFrom();
                            // FIXME: Duplicated from RawTrackerHit loop from above.
                            int ix = helper.getValue(new Identifier(rawHit.getCellID()), "ix");
                            int iy = helper.getValue(new Identifier(rawHit.getCellID()), "iy");

                            // Average amplitude high.
                            amplitudeHighP2D.fill(ix, iy, mode7Data.getAmplHigh());

                            // Average amplitude low.
                            amplitudeLowP2D.fill(ix, iy, mode7Data.getAmplLow());
                        }
                    }
                }
            }
        }
    }
}
