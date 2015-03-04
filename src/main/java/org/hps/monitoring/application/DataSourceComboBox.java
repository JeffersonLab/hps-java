package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComboBox;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.record.enums.DataSourceType;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class DataSourceComboBox extends JComboBox<DataSourceItem> {

    DataSourceComboBox(ActionListener listener) {
        addActionListener(listener);
        setPreferredSize(new Dimension(400, this.getPreferredSize().height));        
    }
    
    static class DataSourceItem {
        
        File file;
        String name;
        DataSourceType type;
        
        DataSourceItem(String name, DataSourceType type) {
            this.type = type;
        }
        
        DataSourceItem(File file, DataSourceType type) {
            this.file = file;
            this.name = file.getName();
            this.file = file;
        }

        public String toString() {
            return name;
        }        
    }         
}
