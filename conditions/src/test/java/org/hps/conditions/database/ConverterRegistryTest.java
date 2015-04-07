package org.hps.conditions.database;

import junit.framework.TestCase;

/**
 * This test loads the {@link ConverterRegistry}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: Doesn't test anything.
public final class ConverterRegistryTest extends TestCase {

    /**
     * Load the global converter registry.
     */
    public final void testConverterRegistry() {
        final ConverterRegistry converterRegistry = ConverterRegistry.create();
        System.out.println(converterRegistry.toString());
    }

}
