package org.hps.digi;

import java.util.Collection;
import java.util.List;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.TriggeredLCIOData;

import org.lcsim.event.RawTrackerHit;

/**
 * <code>RawTrackerHitReadoutDriver</code> handles pulser-data objects in input
 * file of type {@link org.lcsim.event.RawTrackerHit RawTrackerHit}.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class RawTrackerHitReadoutDriver extends PulserDataReadoutDriver<RawTrackerHit> {
    /**
     * Instantiate an instance of {@link org.hps.readout.PulserDataReadoutDriver
     * PulserDataReadoutDriver} for objects of type
     * {@link org.lcsim.event.RawTrackerHit RawTrackerHit} and set the appropriate
     * LCIO flags.
     */
    public RawTrackerHitReadoutDriver() {
        super(RawTrackerHit.class, 0x0);
    }

    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // If hodoscope hits are not persisted, truth data doesn't
        // need to be written out.
        if (!isPersistent()) {
            return null;
        }

        // Get the truth hits in the indicated time range.
        Collection<RawTrackerHit> truthHits = ReadoutDataManager.getData(triggerTime - getReadoutWindowBefore(),
                triggerTime + getReadoutWindowAfter(), collectionNameModified, RawTrackerHit.class);

        // Create the truth MC particle collection.
        LCIOCollection<RawTrackerHit> truthRawHitCollection = ReadoutDataManager
                .getCollectionParameters(collectionNameModified, RawTrackerHit.class);
        TriggeredLCIOData<RawTrackerHit> truthRawHitData = new TriggeredLCIOData<RawTrackerHit>(truthRawHitCollection);
        truthRawHitData.getData().addAll(truthHits);

        // Create a list to store the output data.
        List<TriggeredLCIOData<?>> output = new java.util.ArrayList<TriggeredLCIOData<?>>(2);
        output.add(truthRawHitData);

        // Return the result.
        return output;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return isPersistent() ? getReadoutWindowAfter() : 0;
    }

}
