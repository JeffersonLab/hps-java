package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalTimeWalk;
import org.hps.conditions.ecal.EcalTimeWalk.EcalTimeWalkCollection;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Perform time walk correction on ECal hits and create new collection of hits with corrected time.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class EcalTimeCorrectionDriver extends Driver {

    private String inputHitsCollectionName = "EcalUncalHits";

    private String outputHitsCollectionName = "EcalCalHits";

    /**
     * ecalCollectionName "type" (must match detector-data)
     */
    private final String ecalReadoutName = "EcalHits";

    private boolean mode3 = false;
    private boolean useFit = true;
    private boolean useTimeWalkCondition = true;

    private EcalConditions ecalConditions = null;

    public void setMode3(boolean mode3) {
        this.mode3 = mode3;
    }

    public void setUseFit(boolean useFit) {
        this.useFit = useFit;
    }

    public void setUseTimeWalkCondition(boolean useTimeWalkCondition) {
        this.useTimeWalkCondition = useTimeWalkCondition;
    }

    /**
     * Set the input {@link org.lcsim.event.CalorimeterHit} collection name,
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     */
    public void setInputHitsCollectionName(String inputHitsCollectionName) {
        this.inputHitsCollectionName = inputHitsCollectionName;
    }

    /**
     * Set the output {@link org.lcsim.event.CalorimeterHit} collection name,
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     */
    public void setOutputHitsCollectionName(String name) {
        this.outputHitsCollectionName = name;
    }

    // Time walk default parameters for mode 3. Not studied since 2014 run.
    // This is basically not used anymore.
    private static final double[] DEFAULT_PARAMETERS = {3.64218e+01, -4.60756e+02, 9.18743e+03, 3.73873e+01,
            -6.57130e+01, 1.07182e+02};

    private double[] parameters = DEFAULT_PARAMETERS;

    /*
     * Time walk parameters for mode 1. These parameters were dervied from data for both 2015 and 2016 running.
     */
    public void detectorChanged(Detector detector) {
        System.out.println("detector changed");

        if (useTimeWalkCondition) {
            DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
            EcalTimeWalkCollection timeWalks = manager.getCachedConditions(EcalTimeWalkCollection.class,
                    "ecal_time_walk").getCachedData();
            ecalConditions = manager.getEcalConditions();

            EcalTimeWalk timeWalk = timeWalks.get(0);
            parameters = new double[6];
            parameters[0] = timeWalk.getP0();
            parameters[1] = timeWalk.getP1();
            parameters[2] = timeWalk.getP2();
            parameters[3] = timeWalk.getP3();
            parameters[4] = timeWalk.getP4();
        }
    }

    public void process(EventHeader event) {

        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitsCollectionName);

        List<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

        for (CalorimeterHit hit : hits) {
            double time = hit.getTime();
            double energy = hit.getRawEnergy();

            if (mode3) {
                time = correctTimeWalk(time, energy);

            } else if (useFit) {
                time = correctTimeWalkPulseFitting(time, energy);

            }

            // Apply overall time offset
            time -= findChannel(hit.getCellID()).getTimeShift().getTimeShift();

            newHits.add(CalorimeterHitUtilities.create(energy, time, hit.getCellID()));
        }

        event.put(this.outputHitsCollectionName, newHits, CalorimeterHit.class, event.getMetaData(hits).getFlags(),
                ecalReadoutName);

    }

    /**
     * Perform time walk correction.
     * 
     * @param time FADC Mode-3 Hit time (ns)
     * @param energy Pulse energy (GeV)
     * @return corrected time (ns)
     */
    private final double correctTimeWalk(double time, double energy) {
        final double poly1 = parameters[0] + parameters[1] * energy + parameters[2] * energy * energy;
        final double poly2 = parameters[3] * energy + parameters[4] * energy * energy + parameters[5]
                * Math.pow(energy, 4);
        return time - poly1 * Math.exp(-poly2);
    }

    /**
     * Perform time walk correction for mode 1 hits using pulse fitting.
     * 
     * @param time FADC Mode 1 hit time from pulse fitting (ns)
     * @param energy Pulse energy from pulse fitting (GeV)
     * @return corrected time (ns)
     */
    private final double correctTimeWalkPulseFitting(double time, double energy) {
        final double polyA = parameters[0] + parameters[1] * energy;
        final double polyB = parameters[2] + parameters[3] * energy + parameters[4] * Math.pow(energy, 2);
        return time - (Math.exp(polyA) + polyB);
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

}
