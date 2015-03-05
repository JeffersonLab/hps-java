package org.hps.analysis.trigger.event;

import java.util.List;

import org.hps.analysis.trigger.util.ComponentUtils;
import org.hps.analysis.trigger.util.Pair;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.readout.ecal.triggerbank.SSPNumberedTrigger;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;

public class TriggerEfficiencyModule {
	// Store the statistics.
	protected int[][] triggersSeenByType = new int[6][6];
	protected int[][] triggersMatchedByType = new int[6][6];
	
	/**
	 * Adds the number of matched triggers from the event to the total
	 * seen for each of the appropriate trigger types.
	 * @param eventTriggerType - The trigger type ID for the trigger that
	 * caused the event readout.
	 * @param event - A trigger statistical event.
	 */
	public void addEvent(int eventTriggerType, TriggerMatchEvent event) {
		// Iterate over the matched triggers and track how many were
		// found of each trigger type.
		List<Pair<Trigger<?>, SSPNumberedTrigger>> pairList = event.getMatchedReconPairs();
		for(Pair<Trigger<?>, SSPNumberedTrigger> pair : pairList) {
			// Update the appropriate counter based on the trigger type.
			int triggerType = getTriggerType(pair.getFirstElement());
			triggersMatchedByType[eventTriggerType][triggerType]++;
		}
	}
	
	/**
	 * Adds singles triggers to the list of triggers seen.
	 * @param eventTriggerType - The trigger type ID for the event
	 * trigger type.
	 * @param singlesTriggers - A list of size two containing the
	 * triggers seen for each of the two singles triggers.
	 */
	public void addSinglesTriggers(int eventTriggerType, List<List<? extends Trigger<?>>> singlesTriggers) {
		// Note the trigger type.
		int[] triggerType = { TriggerDiagnosticUtil.TRIGGER_SINGLES_1, TriggerDiagnosticUtil.TRIGGER_SINGLES_2 };
		
		// Track the total number of singles triggers seen.
		addTriggers(eventTriggerType, singlesTriggers, triggerType);
	}
	
	/**
	 * Adds pair triggers to the list of triggers seen.
	 * @param eventTriggerType - The trigger type ID for the event
	 * trigger type.
	 * @param pairTriggers - A list of size two containing the
	 * triggers seen for each of the two pair triggers.
	 */
	public void addPairTriggers(int eventTriggerType, List<List<? extends Trigger<?>>> pairTriggers) {
		// Note the trigger type.
		int[] triggerType = { TriggerDiagnosticUtil.TRIGGER_PAIR_1, TriggerDiagnosticUtil.TRIGGER_PAIR_2 };
		
		// Track the total number of singles triggers seen.
		addTriggers(eventTriggerType, pairTriggers, triggerType);
	}
	
	/**
	 * Clears the data stored in the module.
	 */
	public void clear() {
		triggersSeenByType = new int[6][6];
		triggersMatchedByType = new int[6][6];
	}
	
	@Override
	public TriggerEfficiencyModule clone() {
		// Create a new module.
		TriggerEfficiencyModule clone = new TriggerEfficiencyModule();
		
		// Clone the data.
		clone.triggersMatchedByType = triggersMatchedByType.clone();
		clone.triggersSeenByType = triggersSeenByType.clone();
		
		// Return the clone.
		return clone;
	}
	
	/**
	 * Gets the number of triggers matched in events that were caused
	 * by trigger <code>eventTriggerID</code> for <code>seenTriggerID
	 * </code> trigger.
	 * @param eventTriggerID - The trigger that caused the event.
	 * @param seenTriggerID - The trigger that was seen in the event.
	 * @return Returns the number of matches as an <code>int</code>.
	 */
	public int getTriggersMatched(int eventTriggerID, int seenTriggerID) {
		return triggersMatchedByType[eventTriggerID][seenTriggerID];
	}
	
	/**
	 * Gets the number of triggers seen in events that were caused
	 * by trigger <code>eventTriggerID</code> for <code>seenTriggerID
	 * </code> trigger.
	 * @param eventTriggerID - The trigger that caused the event.
	 * @param seenTriggerID - The trigger that was seen in the event.
	 * @return Returns the number of triggers as an <code>int</code>.
	 */
	public int getTriggersSeen(int eventTriggerID, int seenTriggerID) {
		return triggersSeenByType[eventTriggerID][seenTriggerID];
	}
	
	/**
	 * Prints the trigger statistics to the terminal as a table.
	 */
	public void printModule() {
		// Define constant spacing variables.
		int columnSpacing = 3;
		
		// Define table headers.
		String sourceName = "Source";
		String seenName = "Trigger Efficiency";
		
		// Get the longest column header name.
		int longestHeader = -1;
		for(String triggerName : TriggerDiagnosticUtil.TRIGGER_NAME) {
			longestHeader = ComponentUtils.max(longestHeader, triggerName.length());
		}
		longestHeader = ComponentUtils.max(longestHeader, sourceName.length());
		
		// Determine the spacing needed to display the largest numerical
		// cell value.
		int numWidth = -1;
		int longestCell = -1;
		for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
			for(int seenTriggerID = 0; seenTriggerID < 6; seenTriggerID++) {
				int valueSize = ComponentUtils.getDigits(triggersSeenByType[eventTriggerID][seenTriggerID]);
				int cellSize = valueSize * 2 + 3;
				if(cellSize > longestCell) {
					longestCell = cellSize;
					numWidth = valueSize;
				}
			}
		}
		
		// The total column width can then be calculated from the
		// longer of the header and cell values.
		int columnWidth = ComponentUtils.max(longestCell, longestHeader);
		
		// Calculate the total width of the table value header columns.
		int headerTotalWidth = (TriggerDiagnosticUtil.TRIGGER_NAME.length * columnWidth)
				+ ((TriggerDiagnosticUtil.TRIGGER_NAME.length - 1) * columnSpacing);
		
		// Write the table header.
		String spacingText = ComponentUtils.getChars(' ', columnSpacing);
		System.out.println(ComponentUtils.getChars(' ', columnWidth) + spacingText
				+ getCenteredString(seenName, headerTotalWidth));
		
		// Create the format strings for the cell values.
		String headerFormat = "%-" + columnWidth + "s" + spacingText;
		String cellFormat = "%" + numWidth + "d / %" + numWidth + "d";
		String nullText = getCenteredString(ComponentUtils.getChars('-', numWidth) + " / "
				+ ComponentUtils.getChars('-', numWidth), columnWidth) + spacingText;
		
		// Print the column headers.
		System.out.printf(headerFormat, sourceName);
		for(String header : TriggerDiagnosticUtil.TRIGGER_NAME) {
			System.out.print(getCenteredString(header, columnWidth) + spacingText);
		}
		System.out.println();
		
		// Write out the value columns.
		for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
			// Print out the row header.
			System.out.printf(headerFormat, TriggerDiagnosticUtil.TRIGGER_NAME[eventTriggerID]);
			
			// Print the cell values.
			for(int seenTriggerID = 0; seenTriggerID < 6; seenTriggerID++) {
				if(seenTriggerID == eventTriggerID) { System.out.print(nullText); }
				else {
					String cellText = String.format(cellFormat, triggersMatchedByType[eventTriggerID][seenTriggerID],
							triggersSeenByType[eventTriggerID][seenTriggerID]);
					System.out.print(getCenteredString(cellText, columnWidth) + spacingText);
				}
			}
			
			// Start a new line.
			System.out.println();
		}
	}
	
	/**
	 * Adds triggers in a generic way to the number of triggers seen.
	 * @param eventTriggerType - The trigger type ID for the event
	 * trigger type.
	 * @param triggerList - A list of size two containing the
	 * triggers seen for each of the two triggers of its type.
	 * @param triggerTypeID - The two trigger IDs corresponding to the
	 * list entries.
	 */
	private void addTriggers(int eventTriggerType, List<List<? extends Trigger<?>>> triggerList, int[] triggerTypeID) {
		// Track the total number of singles triggers seen.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			List<? extends Trigger<?>> triggers = triggerList.get(triggerNum);
			triggersSeenByType[eventTriggerType][triggerTypeID[triggerNum]] += triggers.size();
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
	 * Gets the trigger type identifier from a trigger object.
	 * @param trigger - A trigger.
	 * @return Returns the trigger type ID of the argument trigger.
	 */
	private static final int getTriggerType(Trigger<?> trigger) {
		// Choose the appropriate trigger type ID based on the class
		// of the trigger.
		if(trigger instanceof PairTrigger) {
			// Use the trigger number to determine which of the two
			// triggers this is. Note that this assumes that the trigger
			// number is stored as either 0 or 1.
			if(trigger.getTriggerNumber() == 0) {
				return TriggerDiagnosticUtil.TRIGGER_PAIR_1;
			} else {
				return TriggerDiagnosticUtil.TRIGGER_PAIR_2;
			}
		} else if(trigger instanceof SinglesTrigger) {
			// Use the trigger number to determine which of the two
			// triggers this is. Note that this assumes that the trigger
			// number is stored as either 0 or 1.
			if(trigger.getTriggerNumber() == 0) {
				return TriggerDiagnosticUtil.TRIGGER_SINGLES_1;
			} else {
				return TriggerDiagnosticUtil.TRIGGER_SINGLES_2;
			}
		}
		
		// If the trigger type is not supported, throw an exception.
		throw new IllegalArgumentException(String.format("Trigger type \"%s\" is not supported.",
				trigger.getClass().getSimpleName()));
	}
}
