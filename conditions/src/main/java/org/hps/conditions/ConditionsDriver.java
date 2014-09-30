package org.hps.conditions;

import org.lcsim.conditions.ConditionsManager;

/**
 * This {@link org.lcsim.util.Driver} is a subclass of
 * {@link AbstractConditionsDriver} and specifies the database connection
 * parameters and configuration for the development database.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends AbstractConditionsDriver {

    // Default conditions system XML config, which is for the Test Run 2012 database.
    static final String DB_CONFIG = "/org/hps/conditions/config/conditions_dev.xml";

    // Default database connection parameters, which points to the SLAC development database.
    static final String DB_CONNECTION = "/org/hps/conditions/config/conditions_dev.properties";

    public ConditionsDriver() {
        if (ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager) {
            System.out.println(this.getName()+": Found existing DatabaseConditionsManager");
            manager = (DatabaseConditionsManager) ConditionsManager.defaultInstance();
        } else {
            manager = new DatabaseConditionsManager();
            manager.setConnectionResource(DB_CONNECTION);
            manager.configure(DB_CONFIG);
            manager.register();
        }
    }
}
