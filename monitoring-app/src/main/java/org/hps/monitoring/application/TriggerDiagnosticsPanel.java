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

        /**
         * Default name of trigger diagnostics collection.
         */
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

        /**
         * Set the name of the trigger diagnostics collection.
         *
         * @param name the name of the trigger diagnostics collection
         */
        void setDiagnosticCollectionName(final String name) {
            this.diagnosticCollectionName = name;
        }
    }

    /**
     * The panel with cluster statistics.
     */
    private final ClusterTablePanel clusterPanel = new ClusterTablePanel();

    /**
     * The panel with efficiency statistics.
     */
    private final EfficiencyTablePanel efficiencyPanel = new EfficiencyTablePanel();

    /**
     * The panel with pairs statistics.
     */
    private final PairTablePanel pairsPanel = new PairTablePanel();

    /**
     * The panel for singles statistics.
     */
    private final SinglesTablePanel singlesPanel = new SinglesTablePanel();

    /**
     * The tabs containing the statistics panels.
     */
    private final JTabbedPane tabs = new JTabbedPane();

    /**
     * The list of objects that can be updated with trigger diagnostics.
     */
    private final List<DiagnosticUpdatable> updateList = new ArrayList<DiagnosticUpdatable>();

    /**
     * Class constructor.
     */
    TriggerDiagnosticsPanel() {
        this.setLayout(new BorderLayout());

        this.tabs.addTab("Clusters", this.clusterPanel);
        this.tabs.addTab("Singles", this.singlesPanel);
        this.tabs.addTab("Pairs", this.pairsPanel);
        this.tabs.addTab("Efficiency", this.efficiencyPanel);

        this.updateList.add(this.clusterPanel);
        this.updateList.add(this.singlesPanel);
        this.updateList.add(this.pairsPanel);
        this.updateList.add(this.efficiencyPanel);

        this.add(this.tabs, BorderLayout.CENTER);
    }
}
