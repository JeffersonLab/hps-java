package org.hps.conditions.svt;

import java.io.IOException;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.config.ConditionsDatabaseConfiguration;
import org.hps.conditions.svt.SvtConfiguration.SvtConfigurationCollection;
import org.jdom.Document;
import org.jdom.JDOMException;

public class SvtConfigurationTest extends TestCase {

    DatabaseConditionsManager manager;
    
    public void setUp() {
        new ConditionsDatabaseConfiguration(
                "/org/hps/conditions/config/conditions_dev.xml", 
                "/org/hps/conditions/config/conditions_dev_local.properties").setup();
        manager = DatabaseConditionsManager.getInstance();
        manager.openConnection();        
    }
    
    public void testSvtConfiguration() {
        SvtConfigurationCollection collection = manager.getCachedConditions(SvtConfigurationCollection.class, TableConstants.SVT_CONFIGURATIONS).getCachedData();
        
        for (SvtConfiguration config : collection) {
            Document doc = null;
            try {
                doc = config.createDocument();
            } catch (IOException | JDOMException e) {
                throw new RuntimeException(e);
            }
            System.out.println(doc.toString());
        }
    }
    
}
