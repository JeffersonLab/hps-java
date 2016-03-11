package org.hps.analysis.trigger.data;

import org.hps.analysis.trigger.util.ComponentUtils;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;

public class TriggerDiagStats {
    // Define TI trigger type identifiers.
    public static final int SINGLES0 = TriggerStatModule.SINGLES_0;
    public static final int SINGLES1 = TriggerStatModule.SINGLES_1;
    public static final int PAIR0    = TriggerStatModule.PAIR_0;
    public static final int PAIR1    = TriggerStatModule.PAIR_1;
    public static final int PULSER   = TriggerStatModule.PULSER;
    public static final int COSMIC   = TriggerStatModule.COSMIC;
    
    // Tracks the number of TI triggers seen across all events for only
    // the TI trigger with the highest priority in the event.
    private int[] tiSeenHierarchical = new int[6];
    
    // Tracks the number of TI triggers across all events.
    private int[] tiSeenAll = new int[6];
    
    // Store the statistics modules for each of the regular triggers.
    private TriggerEvent[] triggerStats = new TriggerEvent[4];
    
    /**
     * Instantiates a new <code>TriggerDiagStats</code> object.
     */
    public TriggerDiagStats() {
        // Instantiate a trigger statistics module for each of the
        // triggers for which statistics are supported.
        for(int triggerType = 0; triggerType < 4; triggerType++) {
            triggerStats[triggerType] = new TriggerEvent();
        }
    }
    
    /**
     * Clears all of the statistical counters in the object.
     */
    void clear() {
        // Clear the tracked TI trigger data.
        for(int tiType = 0; tiType < 6; tiType++) {
            tiSeenAll[tiType] = 0;
            tiSeenHierarchical[tiType] = 0;
        }
        
        // Clear the trigger statistical modules.
        for(int triggerType = 0; triggerType < 4; triggerType++) {
            triggerStats[triggerType].clear();
        }
    }
    
    /**
     * Gets the trigger data for the pair 0 trigger.
     * @return Returns the <code>TriggerEvent</code> object that holds
     * the trigger data for the pair 0 trigger.
     */
    public TriggerEvent getPair0Stats() {
        return triggerStats[PAIR0];
    }
    
    /**
     * Gets the trigger data for the pair 1 trigger.
     * @return Returns the <code>TriggerEvent</code> object that holds
     * the trigger data for the pair 1 trigger.
     */
    public TriggerEvent getPair1Stats() {
        return triggerStats[PAIR1];
    }
    
    /**
     * Gets the trigger data for the singles 0 trigger.
     * @return Returns the <code>TriggerEvent</code> object that holds
     * the trigger data for the singles 0 trigger.
     */
    public TriggerEvent getSingles0Stats() {
        return triggerStats[SINGLES0];
    }
    
    /**
     * Gets the trigger data for the singles 1 trigger.
     * @return Returns the <code>TriggerEvent</code> object that holds
     * the trigger data for the singles 1 trigger.
     */
    public TriggerEvent getSingles1Stats() {
        return triggerStats[SINGLES1];
    }
    
    /**
     * Gets the total number of events where the TI reported a trigger
     * of the specified type.
     * @param triggerID - The identifier for the type of trigger.
     * @param hierarchical - <code>true</code> returns only the number of
     * events where this trigger type was the <i>only</i> type seen by
     * the TI while <code>false</code> returns the number of events
     * that saw this trigger type without regards for other trigger
     * flags.
     * @return Returns the count as an <code>int</code>.
     */
    public int getTITriggers(int triggerID, boolean hierarchical) {
        // Verify the trigger type.
        validateTriggerType(triggerID);
        
        // Increment the counters.
        if(hierarchical) { return tiSeenHierarchical[triggerID]; }
        else { return tiSeenAll[triggerID]; }
    }
    
    /**
     * Increments the counts tracking the number of TI flags seen.
     * @param flags - An array of <code>boolean</code> values of size
     * six. This represents one flag for each possible TI trigger type.
     */
    public void sawTITriggers(boolean[] flags) {
        // There must be six trigger flags and the array must not be
        // null.
        if(flags == null) {
            throw new NullPointerException("TI trigger flags can not be null.");
        } if(flags.length != 6) {
            throw new IllegalArgumentException("TI trigger flags must be of size six.");
        }
        
        // Check each TI flag in the order of the flag hierarchy. The
        // first flag in the hierarchy that is true is recorded in the
        // hierarchical count. All flags are recorded in the all count.
        boolean foundHierarchical = false;
        if(flags[PAIR1]) {
            tiSeenAll[PAIR1]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[PAIR1]++;
                foundHierarchical = true;
            }
        } if(flags[PAIR0]) {
            tiSeenAll[PAIR0]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[PAIR0]++;
                foundHierarchical = true;
            }
        } if(flags[SINGLES1]) {
            tiSeenAll[SINGLES1]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[SINGLES1]++;
                foundHierarchical = true;
            }
        } if(flags[SINGLES0]) {
            tiSeenAll[SINGLES0]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[SINGLES0]++;
                foundHierarchical = true;
            }
        } if(flags[PULSER]) {
            tiSeenAll[PULSER]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[PULSER]++;
                foundHierarchical = true;
            }
        } if(flags[COSMIC]) {
            tiSeenAll[COSMIC]++;
            if(!foundHierarchical) {
                tiSeenHierarchical[COSMIC]++;
                foundHierarchical = true;
            }
        }
    }
    
    /**
     * Prints the trigger statistics to the terminal as a table.
     */
    public void printEfficiencyTable() {
        // Get the trigger statistics tables.
        int[][] seenStats = new int[6][4];
        int[][] matchedStats = new int[6][4];
        TriggerEvent[] triggerEvents = { getSingles0Stats(), getSingles1Stats(), getPair0Stats(), getPair1Stats() };
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 6; j++) {
                seenStats[j][i] = triggerEvents[i].getReconSimulatedTriggers(j);
                matchedStats[j][i] = triggerEvents[i].getMatchedReconSimulatedTriggers(j);
            }
        }
        
        // Define constant spacing variables.
        int columnSpacing = 3;
        
        // Define table headers.
        String sourceName = "Source";
        String seenName = "Trigger Efficiency";
        
        // Get the longest column header name.
        int longestHeader = -1;
        String[] headerNames = {
                TriggerDiagnosticUtil.TRIGGER_NAME[0],
                TriggerDiagnosticUtil.TRIGGER_NAME[1],
                TriggerDiagnosticUtil.TRIGGER_NAME[2],
                TriggerDiagnosticUtil.TRIGGER_NAME[3],
                "TI Highest Type"
        };
        for(String triggerName : headerNames) {
            longestHeader = ComponentUtils.max(longestHeader, triggerName.length());
        }
        longestHeader = ComponentUtils.max(longestHeader, sourceName.length());
        
        // Determine the spacing needed to display the largest numerical
        // cell value.
        int numWidth = -1;
        int longestCell = -1;
        for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
            for(int seenTriggerID = 0; seenTriggerID < 4; seenTriggerID++) {
                int valueSize = ComponentUtils.getDigits(seenStats[eventTriggerID][seenTriggerID]);
                int cellSize = valueSize * 2 + 13;
                if(cellSize > longestCell) {
                    longestCell = cellSize;
                    numWidth = valueSize;
                }
            }
        }
        
        // The total column width can then be calculated from the
        // longer of the header and cell values.
        int columnWidth = ComponentUtils.max(longestCell, longestHeader);
        int sourceWidth = ComponentUtils.max(
                TriggerDiagnosticUtil.TRIGGER_NAME[0].length(), TriggerDiagnosticUtil.TRIGGER_NAME[1].length(),
                TriggerDiagnosticUtil.TRIGGER_NAME[2].length(), TriggerDiagnosticUtil.TRIGGER_NAME[3].length(),
                TriggerDiagnosticUtil.TRIGGER_NAME[4].length(), TriggerDiagnosticUtil.TRIGGER_NAME[5].length(),
                sourceName.length() );
        
        // Calculate the total width of the table value header columns.
        int headerTotalWidth = (headerNames.length * columnWidth)
                + ((headerNames.length - 1) * columnSpacing);
        
        // Write the table header.
        String spacingText = ComponentUtils.getChars(' ', columnSpacing);
        System.out.println(ComponentUtils.getChars(' ', sourceWidth) + spacingText
                + getCenteredString(seenName, headerTotalWidth));
        
        // Create the format strings for the cell values.
        String headerFormat = "%-" + sourceWidth + "s" + spacingText;
        String cellFormat = "%" + numWidth + "d / %" + numWidth + "d (%7.3f)";
        String nullText = getCenteredString(ComponentUtils.getChars('-', numWidth) + " / "
                + ComponentUtils.getChars('-', numWidth) + " (  N/A  )", columnWidth) + spacingText;
        
        // Print the column headers.
        System.out.printf(headerFormat, sourceName);
        for(String header : headerNames) {
            System.out.print(getCenteredString(header, columnWidth) + spacingText);
        }
        System.out.println();
        
        // Write out the value columns.
        for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
            // Print out the row header.
            System.out.printf(headerFormat, TriggerDiagnosticUtil.TRIGGER_NAME[eventTriggerID]);
            
            // Print the cell values.
            for(int seenTriggerID = 0; seenTriggerID < 4; seenTriggerID++) {
                if(seenTriggerID == eventTriggerID) { System.out.print(nullText); }
                else {
                    String cellText = String.format(cellFormat, matchedStats[eventTriggerID][seenTriggerID],
                            seenStats[eventTriggerID][seenTriggerID],
                            (100.0 * matchedStats[eventTriggerID][seenTriggerID] / seenStats[eventTriggerID][seenTriggerID]));
                    System.out.print(getCenteredString(cellText, columnWidth) + spacingText);
                }
            }
            
            // Output the number of events that had only the trigger
            // type ID for the current trigger type flagged by the TI.
            System.out.print(getCenteredString("" + getTITriggers(eventTriggerID, true), columnWidth) + spacingText);
            
            // Start a new line.
            System.out.println();
        }
    }
    
    /**
     * Produces a <code>String</code> of the indicated length with the
     * text <code>value</code> centered in the middle. Extra length is
     * filled through spaces before and after the text.
     * @param value - The text to display.
     * @param width - The number of spaces to include.
     * @return Returns a <code>String</code> of the specified length,
     * or the argument text if it is longer.
     */
    private static final String getCenteredString(String value, int width) {
        // The method can not perform as intended if the argument text
        // exceeds the requested string length. Just return the text.
        if(width <= value.length()) {
            return value;
        }
        
        // Otherwise, get the amount of buffering needed to center the
        // text and add it around the text to produce the string.
        else {
            int buffer = (width - value.length()) / 2;
            return ComponentUtils.getChars(' ', buffer) + value
                    + ComponentUtils.getChars(' ', width - buffer - value.length());
        }
    }
    
    /**
     * Produces an exception if the argument trigger type is not of a
     * supported type.
     * @param triggerType - The trigger type to verify.
     */
    private static final void validateTriggerType(int triggerType) {
        if(triggerType < 0 || triggerType > 5) {
            throw new IndexOutOfBoundsException(String.format("Trigger type \"%d\" is not supported.", triggerType));
        }
    }
}