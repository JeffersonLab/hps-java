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
    // Store trigger configuration parameters.
    private PairTriggerConfig2019[] pairTrigger = { new PairTriggerConfig2019(), new PairTriggerConfig2019(), new PairTriggerConfig2019(), new PairTriggerConfig2019() };
    private SinglesTriggerConfig2019[] singlesTrigger = { new SinglesTriggerConfig2019(), new SinglesTriggerConfig2019(), new SinglesTriggerConfig2019(), new SinglesTriggerConfig2019() };
    
    @Override
    void loadConfig(EvioDAQParser2019 parser) {
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
        ps.println("SSP Configuration:");
        
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
            ps.printf("\t\t\tValue   :: %1.0f hits%n", singlesTrigger[triggerNum].getXMinCutConfig().getLowerBound());
            ps.println();
            
            ps.println("\t\tCluster PDE Cut");
            ps.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getPDECutConfig().isEnabled());
            ps.printf("\t\t\tC0 :: %8.6f GeV\tC1 :: %8.6f GeV\tC2 :: %8.6f GeV\tC3 :: %8.6f GeV%n", singlesTrigger[triggerNum].getPDECutConfig().getParC0(), 
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
    }
}