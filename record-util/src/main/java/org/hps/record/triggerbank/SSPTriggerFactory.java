package org.hps.record.triggerbank;

/**
 * Class <code>SSPTriggerFactory</code> builds <code>SSPTrigger<code>
 * objects from basic trigger information. These objects will vary in
 * subclass depending on the trigger type and provide trigger data
 * parsing options. Unknown trigger types will always be of object type
 * <code>SSPTrigger</code>, which does not provide trigger data parsing.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPTriggerFactory {
    /**
     * Creates an <code>SSPTrigger</code> object representing the given
     * trigger information. If the trigger type is unknown, this creates
     * a <code>SSPTrigger</code> object. Otherwise, an object for the
     * appropriate type of trigger is generated which will provide trigger
     * data parsing options.
     * @param type - The type of trigger.
     * @param time - The time at which the trigger occurred in ns.
     * @param data - The trigger data.
     * @return Returns an <code>SSPTrigger</code> object. This may be
     * a subclass appropriate to the trigger type.
     */
    public static final SSPTrigger makeTrigger(int type, int time, int data) {
        // Check for cosmic triggers.
        if(isCosmicTrigger(type)) {
            return new SSPCosmicTrigger(isTopTrigger(type), time);
        }
        
        // Check for singles triggers.
        else if(isSinglesTrigger(type)) {
            return new SSPSinglesTrigger(isFirstTrigger(type), isTopTrigger(type), time, data);
        }
        
        // Check for pair triggers.
        else if(isPairTrigger(type)) {
            return new SSPPairTrigger(isFirstTrigger(type), time, data);
        }
        
        // Otherwise, this is an unknown trigger type.
        else {
            return new SSPTrigger(type, time, data);
        }
    }
    
    /**
     * Indicates whether the trigger type is a cosmic trigger.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if the trigger is a cosmic
     * trigger and <code>false</code> otherwise.
     */
    public static final boolean isCosmicTrigger(int type) {
        return (type == SSPData.TRIG_TYPE_COSMIC_BOT) || (type == SSPData.TRIG_TYPE_COSMIC_TOP);
    }
    
    /**
     * Indicates whether the trigger type is a singles trigger.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if the trigger is a singles
     * trigger and <code>false</code> otherwise.
     */
    public static final boolean isSinglesTrigger(int type) {
        return (type == SSPData.TRIG_TYPE_SINGLES0_BOT) || (type == SSPData.TRIG_TYPE_SINGLES0_TOP) ||
                (type == SSPData.TRIG_TYPE_SINGLES1_BOT) || (type == SSPData.TRIG_TYPE_SINGLES1_TOP);
    }
    
    /**
     * Indicates whether the trigger type is a pair trigger.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if the trigger is a pair
     * trigger and <code>false</code> otherwise.
     */
    public static final boolean isPairTrigger(int type) {
        return (type == SSPData.TRIG_TYPE_PAIR0) || (type == SSPData.TRIG_TYPE_PAIR1);
    }
    
    /**
     * Indicates whether this is the first trigger of a set of two
     * triggers of the same type. This always returns <code>true</code>
     * for cosmic triggers, since there is only one. It always returns
     * <code>false</code> for unknown trigger types.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if this is the first trigger
     * in a set of two and <code>false</code> otherwise.
     */
    public static final boolean isFirstTrigger(int type) {
        // There is only one cosmic trigger, so all cosmic triggers
        // are the first trigger.
        if(isCosmicTrigger(type)) { return true; }
        
        // Otherwise, singles trigger 0 (either top or bottom) and
        // pair trigger 0 are first triggers.
        else if((type == SSPData.TRIG_TYPE_SINGLES0_BOT) || (type == SSPData.TRIG_TYPE_SINGLES0_TOP) ||
                    (type == SSPData.TRIG_TYPE_PAIR0)) {
            return true;
        }
        
        // Anything else is not a first trigger.
        else {
            return false;
        }
    }
    
    /**
     * Indicates whether this is the second trigger of a set of two
     * triggers of the same type. This always returns <code>false</code>
     * for cosmic triggers, since there is only one. It also always
     * returns <code>false</code> for unknown trigger types.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if this is the second trigger
     * in a set of two and <code>false</code> otherwise.
     */
    public static final boolean isSecondTrigger(int type) {
        // There is only one cosmic trigger, so no cosmic triggers
        // are the second trigger.
        if(isCosmicTrigger(type)) { return false; }
        
        // Otherwise, singles trigger 1 (either top or bottom) and
        // pair trigger 1 are second triggers.
        else if((type == SSPData.TRIG_TYPE_SINGLES1_BOT) || (type == SSPData.TRIG_TYPE_SINGLES1_TOP) ||
                    (type == SSPData.TRIG_TYPE_PAIR1)) {
            return true;
        }
        
        // Anything else is not a second trigger.
        else { return false; }
    }
    
    /**
     * Indicates whether this trigger originated in the top SPP crate.
     * This always returns <code>true</code> for pair triggers, as they
     * require both crates, and always returns false for unknown trigger
     * types.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if this trigger originated
     * in the top crate and <code>false</code> otherwise.
     */
    public static final boolean isTopTrigger(int type) {
        // Pair triggers require both crates, so this is always true
        // for all triggers of that type.
        if(isPairTrigger(type)) { return true; }
        
        // For singles and cosmic triggers, check the type.
        else if((type == SSPData.TRIG_TYPE_COSMIC_TOP) || (type == SSPData.TRIG_TYPE_SINGLES0_TOP) ||
                (type == SSPData.TRIG_TYPE_SINGLES1_TOP)) {
            return true;
        }
        
        // Otherwise, this is not a (known) top trigger.
        else { return false; }
    }
    
    /**
     * Indicates whether this trigger originated in the bottom SPP crate.
     * This always returns <code>true</code> for pair triggers, as they
     * require both crates, and always returns false for unknown trigger
     * types.
     * @param type - The trigger type.
     * @return Returns <code>true</code> if this trigger originated
     * in the bottom crate and <code>false</code> otherwise.
     */
    public static final boolean isBottomTrigger(int type) {
        // Pair triggers require both crates, so this is always true
        // for all triggers of that type.
        if(isPairTrigger(type)) { return true; }
        
        // For singles and cosmic triggers, check the type.
        else if((type == SSPData.TRIG_TYPE_COSMIC_BOT) || (type == SSPData.TRIG_TYPE_SINGLES0_BOT) ||
                (type == SSPData.TRIG_TYPE_SINGLES1_BOT)) {
            return true;
        }
        
        // Otherwise, this is not a (known) bottom trigger.
        else { return false; }
    }
    
    /**
     * Checks whether the trigger type is from a recognized trigger.
     * @param type - The type code.
     * @return Returns <code>true</code> if the trigger type is of a
     * recognized type and returns <code>false</code> if it is not.
     */
    public static final boolean isKnownTriggerType(int type) {
        // Check against all known trigger types.
        if(type == SSPData.TRIG_TYPE_COSMIC_BOT || type == SSPData.TRIG_TYPE_COSMIC_TOP ||
                type == SSPData.TRIG_TYPE_PAIR0 || type == SSPData.TRIG_TYPE_PAIR1 ||
                type == SSPData.TRIG_TYPE_SINGLES0_BOT || type == SSPData.TRIG_TYPE_SINGLES0_TOP ||
                type == SSPData.TRIG_TYPE_SINGLES1_BOT || type == SSPData.TRIG_TYPE_SINGLES1_TOP) {
            return true;
        }
        
        // If it does not match, it is unknown,
        else { return false; }
    }
}