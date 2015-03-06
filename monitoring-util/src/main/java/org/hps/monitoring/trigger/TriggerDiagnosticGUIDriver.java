package org.hps.monitoring.trigger;

import java.util.List;

import javax.swing.JFrame;

import org.hps.analysis.trigger.DiagSnapshot;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

public class TriggerDiagnosticGUIDriver extends Driver {
	private JFrame window = new JFrame();
	private ClusterTablePanel clusterTable = new ClusterTablePanel();
	private SinglesTablePanel singlesTable = new SinglesTablePanel();
	private PairTablePanel pairTable = new PairTablePanel();
	private EfficiencyTablePanel efficiencyTable = new EfficiencyTablePanel();
	private String diagnosticCollectionName = "DiagnosticSnapshot";
	
	@Override
	public void startOfData() {
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(500, 400);
		//window.add(clusterTable);
		//window.add(singlesTable);
		//window.add(pairTable);
		window.add(efficiencyTable);
		window.setVisible(true);
	}
	
	@Override
	public void process(EventHeader event) {
		// Updates are only performed if a diagnostic snapshot object
		// exists. Otherwise, do nothing.
		if(event.hasCollection(DiagSnapshot.class, diagnosticCollectionName)) {
			// Get the snapshot collection.
			List<DiagSnapshot> snapshotList = event.get(DiagSnapshot.class, diagnosticCollectionName);
			
			// Get the snapshot. There will only ever be one.
			DiagSnapshot snapshot = snapshotList.get(0);
			
			// Feed it to the table.
			//clusterTable.updatePanel(snapshot);
			singlesTable.updatePanel(snapshot);
			pairTable.updatePanel(snapshot);
			efficiencyTable.updatePanel(snapshot);
		}
	}
	
	public void setDiagnosticCollectionName(String name) {
		diagnosticCollectionName = name;
	}
}
