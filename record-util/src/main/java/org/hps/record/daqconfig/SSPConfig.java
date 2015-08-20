package org.hps.record.daqconfig;


/**
 * Class <code>SSPConfig</code> stores SSP configuration settings
 * parsed from the an EVIO file. This class manages the following
 * properties:
 * <ul>
 * <li>Singles 1 Trigger</li>
 * <li>Singles 2 Trigger</li>
 * <li>Pair 1 Trigger</li>
 * <li>Pair 2 Trigger</li>
 * </ul>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPConfig extends IDAQConfig {
    // Store trigger configuration parameters.
    private PairTriggerConfig[] pairTrigger = { new PairTriggerConfig(), new PairTriggerConfig() };
    private SinglesTriggerConfig[] singlesTrigger = { new SinglesTriggerConfig(), new SinglesTriggerConfig() };
    
    @Override
    void loadConfig(EvioDAQParser parser) {
        // Set the trigger parameters.
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
            // Set whether the triggers are enabled or not.
            singlesTrigger[triggerNum].setIsEnabled(parser.singlesEn[triggerNum]);
            pairTrigger[triggerNum].setIsEnabled(parser.pairsEn[triggerNum]);
            
            // Set the cut enabled statuses for the singles trigger.
            singlesTrigger[triggerNum].getEnergyMinCutConfig().setIsEnabled(parser.singlesEnergyMinEn[triggerNum]);
            singlesTrigger[triggerNum].getEnergyMaxCutConfig().setIsEnabled(parser.singlesEnergyMaxEn[triggerNum]);
            singlesTrigger[triggerNum].getHitCountCutConfig().setIsEnabled(parser.singlesNhitsEn[triggerNum]);
            
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
            singlesTrigger[triggerNum].getEnergyMinCutConfig().setLowerBound(parser.singlesEnergyMin[triggerNum] / 1000.0);
            singlesTrigger[triggerNum].getEnergyMaxCutConfig().setUpperBound(parser.singlesEnergyMax[triggerNum] / 1000.0);
            singlesTrigger[triggerNum].getHitCountCutConfig().setLowerBound(parser.singlesNhits[triggerNum]);
            
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
    public SinglesTriggerConfig getSingles1Config() {
        return singlesTrigger[0];
    }
    
    /**
     * Gets the configuration parameters for the second singles trigger.
     * @return Returns the second singles trigger configuration.
     */
    public SinglesTriggerConfig getSingles2Config() {
        return singlesTrigger[1];
    }
    
    /**
     * Gets the configuration parameters for the first pair trigger.
     * @return Returns the first pair trigger configuration.
     */
    public PairTriggerConfig getPair1Config() {
        return pairTrigger[0];
    }
    
    /**
     * Gets the configuration parameters for the second pair trigger.
     * @return Returns the second trigger trigger configuration.
     */
    public PairTriggerConfig getPair2Config() {
        return pairTrigger[1];
    }
    
    @Override
    public void printConfig() {
        // Print the configuration header.
        System.out.println("SSP Configuration:");
        
        // Print the singles triggers.
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
            System.out.printf("\tSingles Trigger %d%n", (triggerNum + 1));
            System.out.println("\t\tCluster Energy Lower Bound Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getEnergyMinCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", singlesTrigger[triggerNum].getEnergyMinCutConfig().getLowerBound());
            
            System.out.println("\t\tCluster Energy Upper Bound Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getEnergyMaxCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", singlesTrigger[triggerNum].getEnergyMaxCutConfig().getUpperBound());
            
            System.out.println("\t\tCluster Hit Count Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", singlesTrigger[triggerNum].getHitCountCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %1.0f hits%n", singlesTrigger[triggerNum].getHitCountCutConfig().getLowerBound());
            System.out.println();
        }
        
        // Print the pair triggers.
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
            System.out.printf("\tPair Trigger %d%n", (triggerNum + 1));
            System.out.println("\t\tCluster Energy Lower Bound Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyMinCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyMinCutConfig().getLowerBound());
            
            System.out.println("\t\tCluster Energy Upper Bound Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyMaxCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyMaxCutConfig().getUpperBound());
            
            System.out.println("\t\tCluster Hit Count Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getHitCountCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %1.0f hits%n", pairTrigger[triggerNum].getHitCountCutConfig().getLowerBound());
            
            System.out.println("\t\tPair Energy Sum Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergySumCutConfig().isEnabled());
            System.out.printf("\t\t\tMin     :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySumCutConfig().getLowerBound());
            System.out.printf("\t\t\tMax     :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySumCutConfig().getUpperBound());
            
            System.out.println("\t\tPair Energy Difference Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergyDifferenceCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergyDifferenceCutConfig().getUpperBound());
            
            System.out.println("\t\tPair Energy Slope Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %5.3f GeV%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().getLowerBound());
            System.out.printf("\t\t\tParam F :: %6.4f GeV/mm%n", pairTrigger[triggerNum].getEnergySlopeCutConfig().getParameterF());
            
            System.out.println("\t\tPair Coplanarity Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getCoplanarityCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %3.0f degrees%n", pairTrigger[triggerNum].getCoplanarityCutConfig().getUpperBound());
            
            System.out.println("\t\tPair Time Coincidence Cut");
            System.out.printf("\t\t\tEnabled :: %b%n", pairTrigger[triggerNum].getTimeDifferenceCutConfig().isEnabled());
            System.out.printf("\t\t\tValue   :: %1.0f clock-cycles%n", pairTrigger[triggerNum].getTimeDifferenceCutConfig().getUpperBound());
            System.out.println();
        }
    }
}