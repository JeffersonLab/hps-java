package org.hps.conditions.svt;

import java.util.LinkedHashMap;

/**
 * This class represents a list of {@link SvtGain} objects associated 
 * with their SVT channel IDs from the database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtGainCollection extends LinkedHashMap<Integer, SvtGain> {
    SvtGainCollection() {        
    }
}
