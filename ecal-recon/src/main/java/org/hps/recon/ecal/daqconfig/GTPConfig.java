package org.hps.recon.ecal.daqconfig;

/**
 * Class <code>GTPConfig</code> stores GTP configuration settings
 * parsed from the an EVIO file. This class manages the following
 * properties:
 * <ul>
 * <li>Cluster verification window size (after seed)</li>
 * <li>Cluster verification window size (before seed)</li>
 * <li>Seed energy cut bounds</li>
 * </ul>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GTPConfig extends IDAQConfig {
    // Store clustering configuration parameters.
    private int windowBefore = -1;
    private int windowAfter = -1;
    private LBOCutConfig seedCut = new LBOCutConfig();
    
    /**
     * Gets the configuration for the seed energy cut.
     * @return Returns the seed energy cut configuration.
     */
    public LBOCutConfig getSeedEnergyCutConfig() {
        return seedCut;
    }
    
    /**
     * Gets the size of the temporal cluster verification window for
     * after a potential seed hit used for checking that a cluster is
     * a spatiotemporal local maximum. Value is in clock-cycles (4 ns
     * intervals).
     * @return Returns the size window after the seed.
     */
    public int getTimeWindowAfter() {
        return windowAfter;
    }
    
    /**
     * Gets the size of the temporal cluster verification window for
     * before a potential seed hit used for checking that a cluster
     * is  a spatiotemporal local maximum. Value is in clock-cycles
     * (4 ns intervals).
     * @return Returns the size window before the seed.
     */
    public int getTimeWindowBefore() {
        return windowBefore;
    }
    
    @Override
    void loadConfig(EvioDAQParser parser) {
        // Load the clustering settings.
        windowBefore = parser.gtpWindowBefore;
        windowAfter = parser.gtpWindowAfter;
        seedCut.setLowerBound(parser.gtpMinSeedEnergy / 1000.0);
    }
    
    @Override
    public void printConfig() {
        // Print the configuration header.
        System.out.println("GTP Configuration:");
        
        // Print the GTP settings.
        System.out.printf("\tTime Window Before :: %d clock-cycles%n", windowBefore);
        System.out.printf("\tTime Window After  :: %d clock-cycles%n", windowAfter);
        System.out.printf("\tSeed Energy Min    :: %5.3f GeV%n",       seedCut.getLowerBound());
    }

}