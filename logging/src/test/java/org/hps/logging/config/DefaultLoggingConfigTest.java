package org.hps.logging.config;

import java.util.logging.Logger;

import junit.framework.TestCase;

/**
 * Test that the default package loggers are setup properly.
 * 
 */
public class DefaultLoggingConfigTest extends TestCase {
  
    /**
     * The package loggers that should be setup by the default configuration. 
     */
    private static final String[] PACKAGES = {
        "org.hps.conditions.api",
        "org.hps.conditions.database",
        "org.hps.conditions.cli",
        "org.hps.conditions.ecal",
        "org.hps.conditions.svt",  
        "org.hps.monitoring.drivers.svt",        
        "org.hps.monitoring.plotting",
        "org.hps.evio",
        "org.hps.analysis.trigger",
        "org.hps.analysis.dataquality",
        "org.hps.crawler",        
        "org.hps.recon.ecal",
        "org.hps.recon.ecal.cluster",
        "org.hps.recon.filtering",    
        "org.hps.record.epics",
        "org.hps.record.evio",
        "org.hps.record.scalers",
        "org.hps.record.triggerbank",    
        "org.hps.recon.tracking",
        "org.hps.recon.tracking.gbl",    
        "org.hps.rundb",
        "org.hps.monitoring.application.model",
        "org.hps.monitoring.application",        
        "org.lcsim.detector.converter.compact",
        "org.lcsim.geometry.compact.converter",
        "org.hps.detector.svt"  
    };
    
    /**
     * Test that the default package loggers are initialized and have a non-null level.
     */
    public void testDefaultLogging() {
                
        for (String loggerName : PACKAGES) {
            System.out.println("checking logger " + loggerName);
            Logger logger = Logger.getLogger(loggerName);
            System.out.println(logger.getName() + " has level " + logger.getLevel());
            assertNotNull("The " + loggerName + " logger is null.", logger);
            assertNotNull("The " + loggerName + " logger does not have a level set.", logger.getLevel());
            assertTrue("The " + loggerName + " logger should not have any handlers.", logger.getHandlers().length == 0);
            logger.severe("hello from " + loggerName);
            logger.info("hello from " + loggerName + " again");
        }
    }

}
