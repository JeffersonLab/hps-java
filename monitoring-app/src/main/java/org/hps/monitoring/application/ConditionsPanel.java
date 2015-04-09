package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;

/**
 * The component for showing conditions tables in the monitoring app.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsPanel extends JPanel {

    /**
     * The listener for updating the panel when conditions are changed.
     */
    class ConditionsPanelListener implements ConditionsListener {

        @Override
        public void conditionsChanged(final ConditionsEvent event) {

            final DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();

            ConditionsPanel.this.tableModels = new LinkedHashMap<String, ConditionsCollectionTableModel>();

            // Set list of table names.
            final ConditionsRecordCollection records = manager.getCachedConditions(ConditionsRecordCollection.class,
                    "conditions").getCachedData();
            records.sortByKey();
            ConditionsPanel.this.conditionsList.removeAll();
            final Set<String> tableNames = new LinkedHashSet<String>();
            for (final ConditionsRecord record : records) {
                tableNames.add(record.getTableName());
            }
            ConditionsPanel.this.conditionsList.setListData(tableNames.toArray(new String[] {}));

            // Create list of table models.
            for (final String tableName : tableNames) {
                final ConditionsObjectCollection<?> collection = manager.getCachedConditions(
                        manager.findTableMetaData(tableName).getCollectionClass(), tableName).getCachedData();
                ConditionsPanel.this.tableModels
                .put(tableName, new ConditionsCollectionTableModel(manager, collection));
            }
        }
    }

    private final JList<String> conditionsList = new JList<String>();

    private final JTable conditionsTable = new JTable();

    private Map<String, ConditionsCollectionTableModel> tableModels;

    ConditionsPanel() {
        super(new BorderLayout());

        this.conditionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.conditionsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                final String tableName = ConditionsPanel.this.conditionsList.getSelectedValue();
                final TableModel model = ConditionsPanel.this.tableModels.get(tableName);
                ConditionsPanel.this.conditionsTable.setModel(model);
                ConditionsPanel.this.conditionsTable.setRowSorter(new TableRowSorter(model));
                ConditionsPanel.this.conditionsTable.revalidate();
            }
        });

        this.conditionsTable.setModel(new DefaultTableModel());

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.conditionsList, new JScrollPane(
                this.conditionsTable));
        splitPane.setResizeWeight(0.6);

        this.add(splitPane);
    }
}