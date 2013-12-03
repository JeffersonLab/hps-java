package org.hps.conditions.svt;

import java.util.LinkedHashMap;

/**
 * A collection of {@link PulseParameters} objects stored by SVT channel ID.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PulseParametersCollection extends LinkedHashMap<Integer,PulseParameters> {
    PulseParametersCollection() {        
    }
}
