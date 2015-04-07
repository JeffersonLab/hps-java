package org.hps.conditions.svt;

import java.io.IOException;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtConfiguration.SvtConfigurationCollection;
import org.jdom.Document;
import org.jdom.JDOMException;

/**
 * Load an SVT XML configuration from the database.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class SvtConfigurationTest extends TestCase {

    /**
     * Load an SVT XML configuration from the database.
     */
    public void testSvtConfiguration() {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        final SvtConfigurationCollection collection = manager.getCachedConditions(SvtConfigurationCollection.class,
                "svt_configurations").getCachedData();
        for (final SvtConfiguration config : collection) {
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
