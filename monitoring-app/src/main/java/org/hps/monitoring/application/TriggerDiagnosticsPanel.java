package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;
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
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class TriggerDiagnosticsPanel extends JPanel {

    /**
     * Driver for updating the tables.
     */
    class TriggerDiagnosticGUIDriver extends Driver {

        // FIXME: Hard-coded collection name.
        private String diagnosticCollectionName = "DiagnosticSnapshot";

        @Override
        public void process(final EventHeader event) {
            // Updates are only performed if a diagnostic snapshot object
            // exists. Otherwise, do nothing.
            if (event.hasCollection(DiagnosticSnapshot.class, this.diagnosticCollectionName)) {
                // Get the snapshot collection.
                final List<DiagnosticSnapshot> snapshotList = event.get(DiagnosticSnapshot.class,
                        this.diagnosticCollectionName);

                // Update the GUI panels.
                for (final DiagnosticUpdatable update : TriggerDiagnosticsPanel.this.updateList) {
                    update.updatePanel(snapshotList.get(1), snapshotList.get(0));
                }
            }
        }

        void setDiagnosticCollectionName(final String name) {
            this.diagnosticCollectionName = name;
        }
    }

    ClusterTablePanel clusterPanel = new ClusterTablePanel();
    EfficiencyTablePanel efficiencyPanel = new EfficiencyTablePanel();
    PairTablePanel pairsPanel = new PairTablePanel();
    SinglesTablePanel singlesPanel = new SinglesTablePanel();

    JTabbedPane tabs = new JTabbedPane();

    List<DiagnosticUpdatable> updateList = new ArrayList<DiagnosticUpdatable>();

    TriggerDiagnosticsPanel() {
        setLayout(new BorderLayout());

        this.tabs.addTab("Clusters", this.clusterPanel);
        this.tabs.addTab("Singles", this.singlesPanel);
        this.tabs.addTab("Pairs", this.pairsPanel);
        this.tabs.addTab("Efficiency", this.efficiencyPanel);

        this.updateList.add(this.clusterPanel);
        this.updateList.add(this.singlesPanel);
        this.updateList.add(this.pairsPanel);
        this.updateList.add(this.efficiencyPanel);

        add(this.tabs, BorderLayout.CENTER);
    }
}
