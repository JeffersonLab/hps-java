package org.hps.conditions.database;

import junit.framework.TestCase;

public class ConverterRegistryTest extends TestCase {
    
    public void testConverterRegistry() {
        ConverterRegistry converterRegistry = ConverterRegistry.create();
        System.out.println(converterRegistry.toString());
    }

}
