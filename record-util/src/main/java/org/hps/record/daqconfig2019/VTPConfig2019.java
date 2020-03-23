package org.hps.record.daqconfig2019;

import java.io.PrintStream;

/**
 * Class <code>VTPConfig2019</code> stores VTP configuration settings
 * parsed from the an EVIO file. This class manages the following
 * properties:
 * <ul>
 * <li>Singles 1 Trigger</li>
 * <li>Singles 2 Trigger</li>
 * <li>Singles 3 Trigger</li>
 * <li>Singles 4 Trigger</li>
 * <li>Pair 1 Trigger</li>
 * <li>Pair 2 Trigger</li>
 * <li>Pair 3 Trigger</li>
 * <li>Pair 4 Trigger</li>
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class VTPConfig2019 extends IDAQConfig2019 {
    ////// Store cluster cut parameters
    // Cluster hit timing coincidence: 0 to 16, units: +/-ns
    private int ecalClusterHitDT = 0;
    // Cluster seed threshold in: 1 to 8191, units MeV
    private double ecalClusterSeedThr = 0;
    // Hodoscope fadc hit cut: minimum acceptable FADC hit integral: 1 to 8191, units TBD
    private int hodoFADCHitThr = 0;
    // Hodoscope trigger hit cut: minimum acceptable integral (clustered or single tile): 1 to 8191, units TBD
    private int hodoThr = 0;   
    // Hodoscope hit coincidence between L1,L2, and also ECAL clusters (real with is specified value +4ns): 0 to 60, units: ns
    private int hodoDT = 0;
           
    //////// Store trigger configuration parameters.
    private PairTriggerConfig2019[] pairTrigger = { new PairTriggerConfig2019(), new PairTriggerConfig2019(), 
            new PairTriggerConfig2019(), new PairTriggerConfig2019() };
    private SinglesTriggerConfig2019[] singlesTrigger = { new SinglesTriggerConfig2019(), new SinglesTriggerConfig2019(), 
            new SinglesTriggerConfig2019(), new SinglesTriggerConfig2019() };
    private MultiplicityTriggerConfig2019[] multiplicityTrigger = { new MultiplicityTriggerConfig2019(), new MultiplicityTriggerConfig2019()};
    private FEETriggerConfig2019 FEETrigger = new FEETriggerConfig2019();
        
    @Override
    void loadConfig(EvioDAQParser2019 parser) {
        // Set cluster cut parameters
        ecalClusterHitDT = parser.ecalClusterHitDT;
        ecalClusterSeedThr = parser.ecalClusterSeedThr / 1000.;
        hodoFADCHitThr = parser.hodoFADCHitThr;
        hodoThr = parser.hodoThr;
        hodoDT = parser.hodoDT;
        
        // Set the trigger parameters.
        for(int triggerNum = 0; triggerNum < 4; triggerNum++) {
            // Set whether the triggers are enabled or not.
            singlesTrigger[triggerNum].setIsEnabled(parser.singlesEn[triggerNum]);
            pairTrigger[triggerNum].setIsEnabled(parser.pairsEn[triggerNum]);
            
            // Set the cut enabled statuses for the singles trigger.
            singlesTrigger[triggerNum].getEnergyMinCutConfig().setIsEnabled(parser.singlesEnergyMinEn[triggerNum]);
            singlesTrigger[triggerNum].getEnergyMaxCutConfig().setIsEnabled(parser.singlesEnergyMaxEn[triggerNum]);
            singlesTrigger[triggerNum].getHitCountCutConfig().setIsEnabled(parser.singlesNhitsMinEn[triggerNum]);
            singlesTrigger[triggerNum].getXMinCutConfig().setIsEnabled(parser.singlesXMinEn[triggerNum]);
            singlesTrigger[triggerNum].getPDECutConfig().setIsEnabled(parser.singlesPDEEn[triggerNum]);
            singlesTrigger[triggerNum].getL1MatchingConfig().setIsEnabled(parser.singlesL1MatchingEn[triggerNum]);
            singlesTrigger[triggerNum].getL2MatchingConfig().setIsEnabled(parser.singlesL2MatchingEn[triggerNum]);
            singlesTrigger[triggerNum].getL1L2GeoMatchingConfig().setIsEnabled(parser.singlesL1L2MatchingEn[triggerNum]);
            singlesTrigger[triggerNum].getHodoEcalGeoMatchingConfig().setIsEnabled(parser.singlesL1L2EcalMatchingEn[triggerNum]);
            
            // Set the individual cut values.
            singlesTrigger[triggerNum].getEnergyMinCutConfig().setLowerBound(parser.singlesEnergyMin[triggerNum] / 1000.0);
            singlesTrigger[triggerNum].getEnergyMaxCutConfig().setUpperBound(parser.singlesEnergyMax[triggerNum] / 1000.0);
            singlesTrigger[triggerNum].getHitCountCutConfig().setLowerBound(parser.singlesNhitsMin[triggerNum]);
            singlesTrigger[triggerNum].getXMinCutConfig().setLowerBound(parser.singlesXMin[triggerNum]);
            singlesTrigger[triggerNum].getPDECutConfig().setParC0(parser.singlesPDEC0[triggerNum]/ 1000.0);
            singlesTrigger[triggerNum].getPDECutConfig().setParC1(parser.singlesPDEC1[triggerNum]/ 1000.0);
            singlesTrigger[triggerNum].getPDECutConfig().setParC2(parser.singlesPDEC2[triggerNum]/ 1000.0);
            singlesTrigger[triggerNum].getPDECutConfig().setParC3(parser.singlesPDEC3[triggerNum]/ 1000.0);
            
            
            // The pair trigger singles cuts are always enabled.
            pairTrigger[triggerNum].getEnergyMinCutConfig().setIsEnabled(true);
            pairTrigger[triggerNum].getEnergyMaxCutConfig().setIsEnabled(true);
            pairTrigger[triggerNum].getHitCountCutConfig().setIsEnabled(true);
            
            // The pair trigger time difference cut is always enabled.
            pairTrigger[triggerNum].getTimeDifferenceCutConfig().setIsEnabled(true);

            // Set the pair cut enabled statuses for the pair trigger.
            pairTrigger[triggerNum].getEnergySumCutConfig().setIsEnabled(parser.pairsEnergySumMaxMinEn[triggerNum]);
            pairTrigger[triggerNum].getEnergyDifferenceCutConfig().setIsEnabled(parser.pairsEnergyDiffEn[triggerNum]);
            pairTrigger[triggerNum].getEnergySlopeCutConfig().setIsEnabled(parser.pairsEnergyDistEn[triggerNum]);
            pairTrigger[triggerNum].getCoplanarityCutConfig().setIsEnabled(parser.pairsCoplanarityEn[triggerNum]);
                        
            // Set the individual cut values.
            pairTrigger[triggerNum].getEnergyMinCutConfig().setLowerBound(parser.pairsEnergyMin[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getEnergyMaxCutConfig().setUpperBound(parser.pairsEnergyMax[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getHitCountCutConfig().setLowerBound(parser.pairsNhitsMin[triggerNum]);
            pairTrigger[triggerNum].getEnergySumCutConfig().setLowerBound(parser.pairsEnergySumMin[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getEnergySumCutConfig().setUpperBound(parser.pairsEnergySumMax[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getEnergyDifferenceCutConfig().setUpperBound(parser.pairsEnergyDiffMax[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getEnergySlopeCutConfig().setLowerBound(parser.pairsEnergyDistMin[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getEnergySlopeCutConfig().setParameterF(parser.pairsEnergyDistSlope[triggerNum] / 1000.0);
            pairTrigger[triggerNum].getCoplanarityCutConfig().setUpperBound(parser.pairsCoplanarityMax[triggerNum]);
            pairTrigger[triggerNum].getTimeDifferenceCutConfig().setUpperBound(parser.pairsTimeDiffMax[triggerNum]);
        }
        
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
            // Set whether the triggers are enabled or not.
            multiplicityTrigger[triggerNum].setIsEnabled(parser.multEn[triggerNum]);
            
            // Set the individual cut values.
            multiplicityTrigger[triggerNum].getEnergyMinCutConfig().setLowerBound(parser.multEnergyMin[triggerNum] / 1000.0);
            multiplicityTrigger[triggerNum].getEnergyMaxCutConfig().setUpperBound(parser.multEnergyMax[triggerNum] / 1000.0);
            multiplicityTrigger[triggerNum].getHitCountCutConfig().setLowerBound(parser.multNhitsMin[triggerNum]);
            multiplicityTrigger[triggerNum].getNClusterTopCutConfig().setLowerBound(parser.multTopMultMin[triggerNum]);
            multiplicityTrigger[triggerNum].getNClusterBotCutConfig().setLowerBound(parser.multBotMultMin[triggerNum]);
            multiplicityTrigger[triggerNum].getNClusterTotCutConfig().setLowerBound(parser.multTotMultMin[triggerNum]);
            multiplicityTrigger[triggerNum].getTimeDifferenceCutConfig().setUpperBound(parser.multDT[triggerNum]);            
        }
        
        FEETrigger.setIsEnabled(parser.FEEEn);
        FEETrigger.getEnergyMinCutConfig().setLowerBound(parser.FEEEnergyMin / 1000.0);
        FEETrigger.getEnergyMaxCutConfig().setUpperBound(parser.FEEEnergyMax / 1000.0);
        FEETrigger.getHitCountCutConfig().setLowerBound(parser.FEENhitsMin / 1000.0);
        FEETrigger.getPrescaleRegion0Config().setRegionMin(parser.FEERegionXMin[0]);
        FEETrigger.getPrescaleRegion0Config().setRegionMax(parser.FEERegionXMax[0]);
        FEETrigger.getPrescaleRegion0Config().setRegionPrescale(parser.FEERegionPrescale[0]);
        FEETrigger.getPrescaleRegion1Config().setRegionMin(parser.FEERegionXMin[1]);
        FEETrigger.getPrescaleRegion1Config().setRegionMax(parser.FEERegionXMax[1]);
        FEETrigger.getPrescaleRegion1Config().setRegionPrescale(parser.FEERegionPrescale[1]);
        FEETrigger.getPrescaleRegion2Config().setRegionMin(parser.FEERegionXMin[2]);
        FEETrigger.getPrescaleRegion2Config().setRegionMax(parser.FEERegionXMax[2]);
        FEETrigger.getPrescaleRegion2Config().setRegionPrescale(parser.FEERegionPrescale[2]);
        FEETrigger.getPrescaleRegion3Config().setRegionMin(parser.FEERegionXMin[3]);
        FEETrigger.getPrescaleRegion3Config().setRegionMax(parser.FEERegionXMax[3]);
        FEETrigger.getPrescaleRegion3Config().setRegionPrescale(parser.FEERegionPrescale[3]);
        FEETrigger.getPrescaleRegion4Config().setRegionMin(parser.FEERegionXMin[4]);
        FEETrigger.getPrescaleRegion4Config().setRegionMax(parser.FEERegionXMax[4]);
        FEETrigger.getPrescaleRegion4Config().setRegionPrescale(parser.FEERegionPrescale[4]);
        FEETrigger.getPrescaleRegion5Config().setRegionMin(parser.FEERegionXMin[5]);
        FEETrigger.getPrescaleRegion5Config().setRegionMax(parser.FEERegionXMax[5]);
        FEETrigger.getPrescaleRegion5Config().setRegionPrescale(parser.FEERegionPrescale[5]);
        FEETrigger.getPrescaleRegion6Config().setRegionMin(parser.FEERegionXMin[6]);
        FEETrigger.getPrescaleRegion6Config().setRegionMax(parser.FEERegionXMax[6]);
        FEETrigger.getPrescaleRegion6Config().setRegionPrescale(parser.FEERegionPrescale[6]);
    }
    
    /**
     * Gets cluster hit timing coincidence: 0 to 16, units: +/-ns.
     * @return Returns limit for cluster hit timing coincidence.
     */
    public int getEcalClusterHitDT() {
        return ecalClusterHitDT;
    }
    /**
     * Gets cluster seed threshold in: 1 to 8191, units MeV.
     * @return Ecal cluster seed threshold.
     */
    public double getEcalClusterSeedThr() {
        return ecalClusterSeedThr;
    }
    /**
     * Gets Hodoscope FADC hit cut: minimum acceptable FADC hit integral: 1 to 8191, units TBD.
     * @return Hodoscope FADC hit.
     */
    public int getHodoFADCHitThr() {
        return hodoFADCHitThr;       
    }
    /**
     * Gets Hodoscope trigger hit cut: minimum acceptable integral (clustered or single tile): 1 to 8191, units TBD.
     * @return Hodoscope cluster/single-tile threshold. 
     */
    public int getHodoThr() {
        return hodoThr;
    }
    /**
     * Gets Hodoscope hit coincidence between L1,L2, and also ECAL clusters (real with is specified value +4ns): 0 to 60, units: ns.
     * return limit for Hodoscope hit coincidence.
     */
    public int getHodoDT() {
        return hodoDT;
    }
    
    /**
     * Gets the configuration parameters for the first singles trigger.
     * @return Returns the first singles trigger configuration.
     */
    public SinglesTriggerConfig2019 getSingles1Config() {
        return singlesTrigger[0];
    }
    
    /**
     * Gets the configuration parameters for the second singles trigger.
     * @return Returns the second singles trigger configuration.
     */
    public SinglesTriggerConfig2019 getSingles2Config() {
        return singlesTrigger[1];
    }
    
    /**
     * Gets the configuration parameters for the third singles trigger.
     * @return Returns the second singles trigger configuration.
     */
    public SinglesTriggerConfig2019 getSingles3Config() {
        return singlesTrigger[2];
    }
    
    /**
     * Gets the configuration parameters for the forth singles trigger.
     * @return Returns the second singles trigger configuration.
     */
    public SinglesTriggerConfig2019 getSingles4Config() {
        return singlesTrigger[3];
    }
    
    /**
     * Gets the configuration parameters for the first pair trigger.
     * @return Returns the first pair trigger configuration.
     */
    public PairTriggerConfig2019 getPair1Config() {
        return pairTrigger[0];
    }
    
    /**
     * Gets the configuration parameters for the second pair trigger.
     * @return Returns the second trigger trigger configuration.
     */
    public PairTriggerConfig2019 getPair2Config() {
        return pairTrigger[1];
    }
    
    /**
     * Gets the configuration parameters for the third pair trigger.
     * @return Returns the second trigger trigger configuration.
     */
    public PairTriggerConfig2019 getPair3Config() {
        return pairTrigger[2];
    }
    
    /**
     * Gets the configuration parameters for the forth pair trigger.
     * @return Returns the second trigger trigger configuration.
     */
    public PairTriggerConfig2019 getPair4Config() {
        return pairTrigger[3];
    }
    
    @Override
    public void printConfig(PrintStream ps) {
        // Print the configuration header.
        ps.println("VTP Configuration:");
        ps.printf("\tCoindence time to accept hits before and after seed hit time :: %d ns%n", getEcalClusterHitDT());
        ps.printf("\tSeed threshold for Ecal clusters  :: %5.3f GeV%n", getEcalClusterSeedThr());
        ps.printf("\tThreshold for hit FADC of Hodoscope :: %d TBD%n", getHodoFADCHitThr());
        ps.printf("\tThreshold for clusters or tiles of Hodoscope :: %d TBD%n", getHodoThr());
        ps.printf("\tHodoscope hit coincidence between L1, L2 and Ecal clusters (real with specified value + 4ns) "
                + ":: %d ns%n", getHodoDT());
        
        // Print the singles triggers.
        for(int triggerNum = 0; triggerNum < 4; triggerNum++) {
            ps.printf("\tSingles Trigger %d%n", (triggerNum + 1));
            ps.println("\t\tCluster Energy Lower Bound Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getEnergyMinCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", singlesTrigger[triggerNum].getEnergyMinCutConfig().getLowerBound());
            
            ps.println("\t\tCluster Energy Upper Bound Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getEnergyMaxCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", singlesTrigger[triggerNum].getEnergyMaxCutConfig().getUpperBound());
            
            ps.println("\t\tCluster Hit Count Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getHitCountCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %1.0f hits%n", singlesTrigger[triggerNum].getHitCountCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tCluster XMin Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getXMinCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %1.0f%n", singlesTrigger[triggerNum].getXMinCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tCluster PDE Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getPDECutConfig().isEnabled());
            ps.printf("\t\t\tC0 :: %10.8f GeV\tC1 :: %10.8f GeV\tC2 :: %10.8f GeV\tC3 :: %10.8f GeV%n", singlesTrigger[triggerNum].getPDECutConfig().getParC0(), 
                        singlesTrigger[triggerNum].getPDECutConfig().getParC1(), singlesTrigger[triggerNum].getPDECutConfig().getParC2(), singlesTrigger[triggerNum].getPDECutConfig().getParC3());
            ps.println();
            
            ps.println("\t\tHodoscope and calorimeter coincidence");
            ps.printf("\t\t\tEnabled :: %b\t%b\t%b\t%b%n", singlesTrigger[triggerNum].getL1MatchingConfig().isEnabled(), 
                    singlesTrigger[triggerNum].getL2MatchingConfig().isEnabled(), singlesTrigger[triggerNum].getL1L2GeoMatchingConfig().isEnabled(), singlesTrigger[triggerNum].getHodoEcalGeoMatchingConfig().isEnabled());
        }
        
        // Print the pair triggers.
        for(int triggerNum = 0; triggerNum < 4; triggerNum++) {
            ps.printf("\tPair Trigger %d%n", (triggerNum + 1));
            ps.println("\t\tCluster Energy Lower Bound Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyMinCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyMinCutConfig().getLowerBound());
            
            ps.println("\t\tCluster Energy Upper Bound Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyMaxCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyMaxCutConfig().getUpperBound());
            
            ps.println("\t\tCluster Hit Count Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getHitCountCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %1.0f hits%n", pairTrigger[triggerNum].getHitCountCutConfig().getLowerBound());
            
            ps.println("\t\tPair Energy Sum Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergySumCutConfig().isEnabled());
            ps.printf("\t\t\tMin     :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySumCutConfig().getLowerBound());
            ps.printf("\t\t\tMax     :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySumCutConfig().getUpperBound());
            
            ps.println("\t\tPair Energy Difference Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyDifferenceCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyDifferenceCutConfig().getUpperBound());
            
            ps.println("\t\tPair Energy Slope Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().getLowerBound());
            ps.printf("\t\t\tParam F :: %6.4f GeV/mm%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().getParameterF());
            
            ps.println("\t\tPair Coplanarity Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getCoplanarityCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %3.0f degrees%n", pairTrigger[triggerNum].getCoplanarityCutConfig().getUpperBound());
            
            ps.println("\t\tPair Time Coincidence Cut");
            ps.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getTimeDifferenceCutConfig().isEnabled());
            ps.printf("\t\t\tValue   :: %1.0f ns%n", pairTrigger[triggerNum].getTimeDifferenceCutConfig().getUpperBound());
            ps.println();
        }
        
        // Print the Multiplicity triggers.
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
            ps.printf("\t%d cluster Multiplicity Trigger%n", (triggerNum + 2));
            ps.println("\t\tCluster Energy Lower Bound Cut");
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", multiplicityTrigger[triggerNum].getEnergyMinCutConfig().getLowerBound());
            
            ps.println("\t\tCluster Energy Upper Bound Cut");
            ps.printf("\t\t\tValue   :: %5.3f GeV%n", multiplicityTrigger[triggerNum].getEnergyMaxCutConfig().getUpperBound());
            
            ps.println("\t\tCluster Hit Count Cut");
            ps.printf("\t\t\tValue   :: %1.0f hits%n", multiplicityTrigger[triggerNum].getHitCountCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tMininum for number of clusters at top");
            ps.printf("\t\t\tValue   :: %1.0f clusters%n", multiplicityTrigger[triggerNum].getNClusterTopCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tMininum for number of clusters at bot");
            ps.printf("\t\t\tValue   :: %1.0f clusters%n", multiplicityTrigger[triggerNum].getNClusterBotCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tMininum for number of total clusters");
            ps.printf("\t\t\tValue   :: %1.0f clusters%n", multiplicityTrigger[triggerNum].getNClusterTotCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tTime Coincidence Cut among clusters");
            ps.printf("\t\t\tValue   :: %1.0f ns%n", multiplicityTrigger[triggerNum].getTimeDifferenceCutConfig().getUpperBound());
            ps.println();
        }
        
        // Print the FEE trigger.
        ps.printf("\tFEE trigger%n");
        ps.println("\t\tCluster Energy Lower Bound Cut");
        ps.printf("\t\t\tValue   :: %5.3f GeV%n", FEETrigger.getEnergyMinCutConfig().getLowerBound());
        
        ps.println("\t\tCluster Energy Upper Bound Cut");
        ps.printf("\t\t\tValue   :: %5.3f GeV%n", FEETrigger.getEnergyMaxCutConfig().getUpperBound());
        
        ps.println("\t\tCluster Hit Count Cut");
        ps.printf("\t\t\tValue   :: %1.0f hits%n", FEETrigger.getHitCountCutConfig().getLowerBound());
        ps.println();
        
        ps.println("\t\tFEE Prescale :: xmin\t xmax\t Prescale");
        ps.printf("\t\t\tRegion 0 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion0Config().getRegionMin(),
                FEETrigger.getPrescaleRegion0Config().getRegionMax(), FEETrigger.getPrescaleRegion0Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 1 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion1Config().getRegionMin(),
                FEETrigger.getPrescaleRegion1Config().getRegionMax(), FEETrigger.getPrescaleRegion1Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 2 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion2Config().getRegionMin(),
                FEETrigger.getPrescaleRegion2Config().getRegionMax(), FEETrigger.getPrescaleRegion2Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 3 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion3Config().getRegionMin(),
                FEETrigger.getPrescaleRegion3Config().getRegionMax(), FEETrigger.getPrescaleRegion3Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 4 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion4Config().getRegionMin(),
                FEETrigger.getPrescaleRegion4Config().getRegionMax(), FEETrigger.getPrescaleRegion4Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 5 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion5Config().getRegionMin(),
                FEETrigger.getPrescaleRegion5Config().getRegionMax(), FEETrigger.getPrescaleRegion5Config().getRegionPrescale());
        ps.printf("\t\t\tRegion 6 :: %3.0f\t%3.0f\t%3.0f%n", FEETrigger.getPrescaleRegion6Config().getRegionMin(),
                FEETrigger.getPrescaleRegion6Config().getRegionMax(), FEETrigger.getPrescaleRegion6Config().getRegionPrescale());
        
    }
}