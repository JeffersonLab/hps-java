package org.hps.conditions.database;

import junit.framework.TestCase;

/**
 * This test loads the {@link TableRegistry}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: Doesn't test anything.
public final class TableRegistryTest extends TestCase {

    /**
     * Load the global table registry.
     */
    public void testTableRegistry() {
        final TableRegistry registry = TableRegistry.create();
        System.out.println(registry.toString());
    }
}
