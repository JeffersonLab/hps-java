package org.hps.users.kmccarty.triggerdiagnostics.ui;

import javax.swing.JTable;

import org.hps.users.kmccarty.triggerdiagnostics.DiagSnapshot;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerEfficiencyModule;
import org.hps.users.kmccarty.triggerdiagnostics.util.TriggerDiagnosticUtil;

public class EfficiencyTablePanel extends AbstractTablePanel implements DiagnosticUpdatable {
	// Static variables.
	private static final long serialVersionUID = 0L;
	
	// Table models.
	private TableTextModel localModel;
	private TableTextModel globalModel;
	
	/**
	 * Instantiates a new <code>EfficiencyTablePanel</code>.
	 */
	public EfficiencyTablePanel() {
		// Instantiate the superclass.
		super();
		
		// Set the orientation to vertical.
		setOrientation(ORIENTATION_VERTICAL);
	}
	
	@Override
	public void updatePanel(DiagSnapshot snapshot) {
		// If there is no snapshot, the tables should all display an
		// empty value.
		if(snapshot == null) {
			for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
				for(int seenTriggerID = 0; seenTriggerID < 6; seenTriggerID++) {
					localModel.setValueAt("--- / ---", eventTriggerID + 1, seenTriggerID + 1);
					globalModel.setValueAt("--- / ---", eventTriggerID + 1, seenTriggerID + 1);
				}
			}
		}
		
		// Otherwise, update the table cells from the snapshot data.
		else {
		// Get the efficiency modules.
			TriggerEfficiencyModule rmod = snapshot.efficiencyRunStatistics;
			TriggerEfficiencyModule lmod = snapshot.efficiencyLocalStatistics;
			
			// Determine the spacing needed to display the largest numerical
			// cell value.
			int numWidth = -1;
			for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
				for(int seenTriggerID = 0; seenTriggerID < 6; seenTriggerID++) {
					int rSize = ComponentUtils.getDigits(rmod.getTriggersSeen(eventTriggerID, seenTriggerID));
					int lSize = ComponentUtils.getDigits(lmod.getTriggersSeen(eventTriggerID, seenTriggerID));
					numWidth = ComponentUtils.max(numWidth, rSize, lSize);
				}
			}
			
			// Generate the format string for the cells.
			String nullText = String.format("%s / %s", ComponentUtils.getChars('-', numWidth),
					ComponentUtils.getChars('-', numWidth));
			String format = "%" + numWidth + "d / %" + numWidth + "d";
			
			// Update the table.
			for(int eventTriggerID = 0; eventTriggerID < 6; eventTriggerID++) {
				for(int seenTriggerID = 0; seenTriggerID < 6; seenTriggerID++) {
					if(eventTriggerID == seenTriggerID) {
						localModel.setValueAt(nullText, eventTriggerID + 1, seenTriggerID + 1);
					} else {
						localModel.setValueAt(String.format(format, lmod.getTriggersMatched(eventTriggerID, seenTriggerID),
								lmod.getTriggersSeen(eventTriggerID, seenTriggerID)), eventTriggerID + 1, seenTriggerID + 1);
						globalModel.setValueAt(String.format(format, rmod.getTriggersMatched(eventTriggerID, seenTriggerID),
								rmod.getTriggersSeen(eventTriggerID, seenTriggerID)), eventTriggerID + 1, seenTriggerID + 1);
					}
				}
			}
		}
	}
	
	@Override
	protected JTable[] initializeTables(Object... args) {
		// Get a shorter reference to the trigger name list.
		String[] triggerNames = TriggerDiagnosticUtil.TRIGGER_NAME;
		
		// Initialize the table models. There should be one row and
		// one column for each type of trigger plus an additional one
		// of each for headers.
		localModel = new TableTextModel(triggerNames.length + 1, triggerNames.length + 1);
		globalModel = new TableTextModel(triggerNames.length + 1, triggerNames.length + 1);
		
		// Set the column and row headers.
		for(int triggerType = 0; triggerType < triggerNames.length; triggerType++) {
			localModel.setValueAt(triggerNames[triggerType], triggerType + 1, 0);
			localModel.setValueAt(triggerNames[triggerType], 0, triggerType + 1);
			globalModel.setValueAt(triggerNames[triggerType], triggerType + 1, 0);
			globalModel.setValueAt(triggerNames[triggerType], 0, triggerType + 1);
		}
		
		// Create JTable objects to display the data.
		JTable localTable = new JTable(localModel);
		localTable.setRowSelectionAllowed(false);
		localTable.setColumnSelectionAllowed(false);
		localTable.setCellSelectionEnabled(false);
		localTable.setShowVerticalLines(false);
		
		JTable globalTable = new JTable(globalModel);
		globalTable.setRowSelectionAllowed(false);
		globalTable.setColumnSelectionAllowed(false);
		globalTable.setCellSelectionEnabled(false);
		globalTable.setShowVerticalLines(false);
		
		// Return the tables.
		return new JTable[] { localTable, globalTable };
	}
	
}