/**
 * 
 */
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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class ConditionsPanel extends JPanel {
    
    JList<String> conditionsList = new JList<String>();
    JTable conditionsTable = new JTable();
    Map<String, ConditionsCollectionTableModel> tableModels;
    
    ConditionsPanel() {
        super(new BorderLayout());
        
        conditionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conditionsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String tableName = (String) conditionsList.getSelectedValue();
                TableModel model = tableModels.get(tableName);
                conditionsTable.setModel(model);
                conditionsTable.setRowSorter(new TableRowSorter(model));
                conditionsTable.revalidate();
            }            
        });
        
        conditionsTable.setModel(new DefaultTableModel());
               
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, conditionsList, new JScrollPane(conditionsTable));
        splitPane.setResizeWeight(0.6);
        
        add(splitPane);
    }
    
    class ConditionsPanelListener implements ConditionsListener {

        @Override
        public void conditionsChanged(ConditionsEvent event) {

            DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();
            
            tableModels = new LinkedHashMap<String, ConditionsCollectionTableModel>();            
                         
            // Set list of table names.
            ConditionsRecordCollection records = 
                    manager.getCachedConditions(ConditionsRecordCollection.class, "conditions").getCachedData();
            records.sortByKey();
            conditionsList.removeAll();
            Set<String> tableNames = new LinkedHashSet<String>();
            for (ConditionsRecord record : records) {                
                tableNames.add(record.getTableName());
            }            
            conditionsList.setListData(tableNames.toArray(new String[] {}));
            
            // Create list of table models.            
            for (String tableName : tableNames) {
                ConditionsObjectCollection<?> collection = manager.getCachedConditions(
                                manager.findTableMetaData(tableName).getCollectionClass(), 
                                tableName).getCachedData();
                tableModels.put(tableName, new ConditionsCollectionTableModel(manager, collection));
            }
        }
    }
}