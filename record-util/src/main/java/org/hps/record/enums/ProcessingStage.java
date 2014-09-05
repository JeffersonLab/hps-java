package org.hps.record.enums;

/**
* When set this can be used to limit the number 
* of processing stages that are excecuted by the
* {@link org.hps.record.composite.CompositeLoop}.
* For example, if the <code>ProcessingStage</code>
* is set to <code>EVIO</code> then the <code>ET</code>
* and <code>EVIO</code> adapters will be activated
* but LCIO events will not be created or processed.
*/
public enum ProcessingStage {
    ET,
    EVIO,
    LCIO
}