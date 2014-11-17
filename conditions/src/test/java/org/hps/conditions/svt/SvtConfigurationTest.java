package org.hps.conditions.svt;

import java.io.IOException;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.svt.SvtConfiguration.SvtConfigurationCollection;
import org.jdom.Document;
import org.jdom.JDOMException;

public class SvtConfigurationTest extends TestCase {

    DatabaseConditionsManager manager;

    public void testSvtConfiguration() {
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
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
