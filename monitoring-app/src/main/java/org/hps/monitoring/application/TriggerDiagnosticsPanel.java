package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.hps.analysis.trigger.DiagSnapshot;
import org.hps.monitoring.trigger.ClusterTablePanel;
import org.hps.monitoring.trigger.DiagnosticUpdatable;
import org.hps.monitoring.trigger.EfficiencyTablePanel;
import org.hps.monitoring.trigger.PairTablePanel;
import org.hps.monitoring.trigger.SinglesTablePanel;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * This is a panel containing the trigger diagnostics tables.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TriggerDiagnosticsPanel extends JPanel {

    JTabbedPane tabs = new JTabbedPane();
    ClusterTablePanel clusterPanel = new ClusterTablePanel();
    SinglesTablePanel singlesPanel = new SinglesTablePanel();
    PairTablePanel pairsPanel = new PairTablePanel();
    EfficiencyTablePanel efficiencyPanel = new EfficiencyTablePanel();
    
    List<DiagnosticUpdatable> updateList = new ArrayList<DiagnosticUpdatable>();
    
    TriggerDiagnosticsPanel() {
        setLayout(new BorderLayout());
                       
        tabs.addTab("Clusters", clusterPanel);
        tabs.addTab("Singles", singlesPanel);
        tabs.addTab("Pairs", pairsPanel);
        tabs.addTab("Efficiency", efficiencyPanel);
        
        updateList.add(clusterPanel);
        updateList.add(singlesPanel);
        updateList.add(pairsPanel);
        updateList.add(efficiencyPanel);
        
        add(tabs, BorderLayout.CENTER);
    }
        
    /**
     * Driver for updating the tables.
     */
    class TriggerDiagnosticGUIDriver extends Driver {

        // FIXME: Hard-coded collection name.
        private String diagnosticCollectionName = "DiagnosticSnapshot";
        
        @Override
        public void process(EventHeader event) {
            // Updates are only performed if a diagnostic snapshot object
            // exists. Otherwise, do nothing.
            if(event.hasCollection(DiagSnapshot.class, diagnosticCollectionName)) {
                // Get the snapshot collection.
                List<DiagSnapshot> snapshotList = event.get(DiagSnapshot.class, diagnosticCollectionName);
                
                // Get the snapshot. There will only ever be one.
                DiagSnapshot snapshot = snapshotList.get(0);
                
                // Update the GUI panels.
                for (DiagnosticUpdatable update : updateList) {
                    update.updatePanel(snapshot);
                }
            } 
        }
        
        void setDiagnosticCollectionName(String name) {
            diagnosticCollectionName = name;
        }
    }  
}
