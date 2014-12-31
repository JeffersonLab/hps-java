package org.hps.conditions.database;

import junit.framework.TestCase;

public class TableRegistryTest extends TestCase {

    public void testTableRegistry() {
        TableRegistry registry = TableRegistry.create();
        System.out.println(registry.toString());
    }
}
