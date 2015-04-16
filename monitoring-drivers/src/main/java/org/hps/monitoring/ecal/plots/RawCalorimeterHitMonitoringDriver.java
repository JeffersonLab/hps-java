package org.hps.monitoring.ecal.plots;

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

    private int accumulatedEvents = 0;
    private final AIDA aida = AIDA.defaultInstance();
    private IHistogram1D amplitudeH1D;
    private ICloud1D amplitudeHighC1D;
    private IProfile2D amplitudeHighP2D;
    private ICloud1D amplitudeLowC1D;
    private IProfile2D amplitudeLowP2D;
    private EcalConditions conditions;
    private IIdentifierHelper helper;
    private IHistogram2D hitMapH2D;
    private IHistogram1D hitsPerEventH1D;

    private long lastUpdatedMillis = 0;
    private IHistogram2D occupancyH2D;
    private final Map<Long, Integer> occupancyMap = new HashMap<Long, Integer>();
    private long occupancyUpdateMillis = 5000; // Refresh occupancy plots every 5 seconds.

    private ICloud1D pedSubAmplitudeC1D;

    private String rawCalorimeterHitCollectionName = "EcalReadoutHits";

    private Subdetector subdetector;
    private ICloud1D timestampC1D;
    private IProfile2D timestampP2D;

    @Override
    public void detectorChanged(final Detector detector) {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        this.conditions = manager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        this.subdetector = detector.getSubdetector("Ecal");
        this.helper = this.subdetector.getDetectorElement().getIdentifierHelper();
    }

    @Override
    public void process(final EventHeader event) {

        if (event.hasCollection(RawCalorimeterHit.class, this.rawCalorimeterHitCollectionName)) {

            final List<RawCalorimeterHit> rawHits = event.get(RawCalorimeterHit.class,
                    this.rawCalorimeterHitCollectionName);

            /*
             * Do basic plots of RawCalorimeterHit data.
             */
            this.hitsPerEventH1D.fill(rawHits.size());
            final Set<RawCalorimeterHit> uniqueRawHits = new HashSet<RawCalorimeterHit>();
            for (final RawCalorimeterHit rawHit : rawHits) {
                this.amplitudeH1D.fill(rawHit.getAmplitude());
                this.timestampC1D.fill(rawHit.getTimeStamp());
                final EcalChannel channel = this.conditions.getChannelCollection().findGeometric(rawHit.getCellID());
                final EcalChannelConstants constants = this.conditions.getChannelConstants(channel);
                this.pedSubAmplitudeC1D.fill(rawHit.getAmplitude() - constants.getCalibration().getPedestal()
                        * EcalMonitoringUtilities.INTEGRAL_WINDOW);

                final int ix = this.helper.getValue(new Identifier(rawHit.getCellID()), "ix");
                final int iy = this.helper.getValue(new Identifier(rawHit.getCellID()), "iy");
                this.hitMapH2D.fill(ix, iy);

                this.timestampP2D.fill(ix, iy, rawHit.getTimeStamp());

                uniqueRawHits.add(rawHit);
            }

            /*
             * Update and plot occupancies based on unique hits in the event.
             */
            for (final RawCalorimeterHit rawHit : uniqueRawHits) {

                // Add entry if does not exist.
                if (this.occupancyMap.get(rawHit.getCellID()) == null) {
                    this.occupancyMap.put(rawHit.getCellID(), 0);
                }

                // Increment hit count by one for this hit.
                final int nHits = this.occupancyMap.get(rawHit.getCellID()) + 1;
                this.occupancyMap.put(rawHit.getCellID(), nHits);
            }
            ++this.accumulatedEvents;
            final long elapsed = System.currentTimeMillis() - this.lastUpdatedMillis;
            if (elapsed >= this.occupancyUpdateMillis) {
                this.occupancyH2D.reset();
                for (final Entry<Long, Integer> entry : this.occupancyMap.entrySet()) {
                    final IIdentifier hitId = new Identifier(entry.getKey());
                    final int ix = this.helper.getValue(hitId, "ix");
                    final int iy = this.helper.getValue(hitId, "iy");
                    this.occupancyH2D.fill(ix, iy, (double) entry.getValue() / (double) this.accumulatedEvents);
                }
                this.lastUpdatedMillis = System.currentTimeMillis();
                this.accumulatedEvents = 0;
                this.occupancyMap.clear();
            }

            /*
             * Plot from mode 7 data if it exists.
             */
            if (event.hasCollection(LCRelation.class, EcalMonitoringUtilities.EXTRA_DATA_RELATIONS_NAME)) {
                final List<LCRelation> extraDataRelations = event.get(LCRelation.class,
                        EcalMonitoringUtilities.EXTRA_DATA_RELATIONS_NAME);
                if (extraDataRelations != null) {
                    for (final LCRelation rel : event.get(LCRelation.class,
                            EcalMonitoringUtilities.EXTRA_DATA_RELATIONS_NAME)) {
                        final GenericObject extraData = (GenericObject) rel.getTo();
                        if (extraData instanceof HitExtraData.Mode7Data) {
                            final HitExtraData.Mode7Data mode7Data = (HitExtraData.Mode7Data) extraData;
                            this.amplitudeHighC1D.fill(mode7Data.getAmplHigh());
                            this.amplitudeLowC1D.fill(mode7Data.getAmplLow());

                            final RawCalorimeterHit rawHit = (RawCalorimeterHit) rel.getFrom();
                            // FIXME: Duplicated from RawTrackerHit loop from above.
                            final int ix = this.helper.getValue(new Identifier(rawHit.getCellID()), "ix");
                            final int iy = this.helper.getValue(new Identifier(rawHit.getCellID()), "iy");

                            // Average amplitude high.
                            this.amplitudeHighP2D.fill(ix, iy, mode7Data.getAmplHigh());

                            // Average amplitude low.
                            this.amplitudeLowP2D.fill(ix, iy, mode7Data.getAmplLow());
                        }
                    }
                }
            }
        }
    }

    public void setOccupancyUpdateMillis(final long occupancyUpdateMillis) {
        this.occupancyUpdateMillis = occupancyUpdateMillis;
    }

    public void setRawCalorimeterHitCollectionName(final String rawCalorimeterHitCollectionName) {
        this.rawCalorimeterHitCollectionName = rawCalorimeterHitCollectionName;
    }

    @Override
    public void startOfData() {
        this.pedSubAmplitudeC1D = this.aida.cloud1D("Pedestal Sub Amplitude");
        this.amplitudeH1D = this.aida.histogram1D("Amplitude", 150, 0.5, 14999.5);
        this.timestampC1D = this.aida.cloud1D("Timestamp");
        this.amplitudeLowC1D = this.aida.cloud1D("Mode 7 Amplitude Low");
        this.amplitudeHighC1D = this.aida.cloud1D("Mode 7 Amplitude High");
        this.hitsPerEventH1D = this.aida.histogram1D("Hits Per Event", 100, -0.5, 99.5);
        this.hitMapH2D = EcalMonitoringUtilities.createChannelHistogram2D("Hit Map");
        this.occupancyH2D = EcalMonitoringUtilities.createChannelHistogram2D("Hit Occupancy");
        this.amplitudeHighP2D = EcalMonitoringUtilities.createChannelProfile2D("Mode 7 Average Amplitude High");
        this.amplitudeLowP2D = EcalMonitoringUtilities.createChannelProfile2D("Mode 7 Average Amplitude Low");
        this.timestampP2D = EcalMonitoringUtilities.createChannelProfile2D("Average Timestamp");

        final IPlotterFactory plotterFactory = this.aida.analysisFactory().createPlotterFactory(
                "ECAL - " + this.rawCalorimeterHitCollectionName);

        EcalMonitoringUtilities.plot(plotterFactory, this.pedSubAmplitudeC1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.amplitudeH1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.timestampC1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.amplitudeLowC1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.amplitudeHighC1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.hitsPerEventH1D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.hitMapH2D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.occupancyH2D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.amplitudeHighP2D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.amplitudeLowP2D, null, true);
        EcalMonitoringUtilities.plot(plotterFactory, this.timestampP2D, null, true);

        this.lastUpdatedMillis = System.currentTimeMillis();
    }
}
