package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.hps.conditions.trigger.TriggerTimeWindow.TriggerTimeWindowCollection;

/**
 * This driver chooses the highest energy cluster within the trigger time window in an event and uses the RF time to set
 * the trigger time of the event.
 * 
 * @author holly <hszumila@jlab.org>
 */
public class ClusterRFTimeCorrDriver extends Driver {

    // From database as mean, min, max
    // Defaults here are from 2015 running
    // This is a tunable parameter from the DAQ
    private static final double[] DEFAULT_PARAMETER = {45, 40, 50};

    private double[] parameter = DEFAULT_PARAMETER;

    // Get the ideal trigger time window for a run from the conditions db
    public void detectorChanged(Detector detector) {
        System.out.println("detector changed");

        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        TriggerTimeWindowCollection trigColl = manager.getCachedConditions(TriggerTimeWindowCollection.class,
                "trigger_time_windows").getCachedData();

        parameter[0] = trigColl.get(0).getOffset();
        parameter[1] = trigColl.get(0).getOffsetMin();
        parameter[2] = trigColl.get(0).getOffsetMax();

    }

    public void process(EventHeader event) {

        List<TriggerTime> triggerTime = new ArrayList<TriggerTime>();

        boolean foundTdiff = false;

        double clT = parameter[0];
        double selectedClT = parameter[0];
        double rfT = parameter[0];
        int seedX = 0;
        int seedY = 0;
        int jj = 0;
        // read in ecal clusters, choose the highest energy cluster in the trigger time window
        if (event.hasCollection(Cluster.class, "EcalClusters")) {
            // Get the list of clusters in the event
            List<Cluster> cl = event.get(Cluster.class, "EcalClusters");
            // Sort the clusters as highest to lowest energy
            Comparator<Cluster> comparator = new ClusterUtilities.ClusterSeedComparator();
            ClusterUtilities.sort(cl, comparator, true, true);
            int listSize = cl.size();
            while (foundTdiff == false) {
                if (listSize == 0) {
                    break;
                }
                // choose the highest seed energy cluster in the time window
                for (Cluster cc : cl) {
                    clT = ClusterUtilities.getSeedHitTime(cc);
                    if (clT > parameter[1] - jj * 10 && clT < parameter[2] + jj * 10) {
                        foundTdiff = true;
                        selectedClT = clT;
                        seedX = ClusterUtilities.findSeedHit(cc).getIdentifierFieldValue("ix");
                        seedY = ClusterUtilities.findSeedHit(cc).getIdentifierFieldValue("iy");
                        break;
                    }// end if
                }// end for
                jj++;
            }// end while
        }// end if has clusters

        double jitter = 0;
        // read in rf time
        if (event.hasCollection(GenericObject.class, "RFHits")) {
            List<GenericObject> rfTimes = event.get(GenericObject.class, "RFHits");

            rfT = rfTimes.get(0).getDoubleVal(1);
            jitter = ((rfT - selectedClT + 400 * 2.004) % 2.004) - 1.002;
            selectedClT += jitter;
        }

        triggerTime.add(new TriggerTime(selectedClT, seedX, seedY));
        event.put("TriggerTime", triggerTime, TriggerTime.class, 1);

    }// end process event

}// end driver