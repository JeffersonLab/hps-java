package org.hps.monitoring.application;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.hps.monitoring.application.DataSourceComboBox.DataSourceItem;
import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;
import org.hps.record.enums.DataSourceType;

/**
 * This is a combo box that shows the current data source such as an LCIO file, EVIO file or ET ring. It can also be
 * used to select a data source for the next monitoring session.
 * <p>
 * This component is not directly connected to an event loop, so it must catch changes to the configuration via property
 * change events and then update its state accordingly.
 * <p>
 * Only a single "global" ET item is kept in the list, and it is updated as changes are made to the configuration model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class DataSourceComboBox extends JComboBox<DataSourceItem> implements PropertyChangeListener, ActionListener {

    /**
     * This class represents a data source item in the combo box, which has a name for display, a full path to the file,
     * and an implicit type (EVIO, LCIO or ET).
     */
    static class DataSourceItem {

        /**
         * The name of the data source which will show in the drop down box.
         */
        private String name;

        /**
         * The full path used for the data source (for ET this is not used directly).
         */
        private final String path;

        /**
         * The implicit data type (EVIO, LCIO and ET).
         */
        private final DataSourceType type;

        /**
         * Create a data source item.
         *
         * @param path the data source path
         * @param name the data source name
         * @param type the data source type
         */
        DataSourceItem(final String path, final String name, final DataSourceType type) {
            if (path == null) {
                throw new IllegalArgumentException("path is null");
            }
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type is null");
            }
            this.type = type;
            this.name = name;
            this.path = path;
        }

        /**
         * Implementation of equals operation.
         *
         * @param object the other object
         */
        @Override
        public boolean equals(final Object object) {
            if (!(object instanceof DataSourceItem)) {
                return false;
            }
            final DataSourceItem otherItem = (DataSourceItem) object;
            return this.name == otherItem.name && this.path == otherItem.path && this.type == otherItem.type;
        }

        /**
         * Get the name of the source that is used as text in the drop down menu.
         *
         * @return the name of the data source
         */
        public String getName() {
            return this.name;
        }

        /**
         * Get the full path to the data source which is used as tool tip text (not used directly for ET sources).
         *
         * @return the full path to the data source
         */
        public String getPath() {
            return this.path;
        }

        /**
         * Convert this object to a string.
         *
         * @return this object converted to a string
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * The preferred width of this component in pixels.
     */
    private static final int PREFERRED_WIDTH = 510;

    /**
     * The backing configuration model.
     */
    private ConfigurationModel configurationModel;

    /**
     * Create a new data source combo box.
     *
     * @param configurationModel the underlying configuration data model
     * @param connectionModel the underlying connection status data model
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    DataSourceComboBox(final ConfigurationModel configurationModel, final ConnectionStatusModel connectionModel) {
        addActionListener(this);
        setActionCommand(Commands.DATA_SOURCE_CHANGED);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, this.getPreferredSize().height));
        setEditable(false);
        this.configurationModel = configurationModel;
        connectionModel.addPropertyChangeListener(this);
        configurationModel.addPropertyChangeListener(this);

        final ListCellRenderer renderer = new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                if (value instanceof DataSourceItem) {
                    setToolTipText(((DataSourceItem) value).getPath());
                } else {
                    setToolTipText(null);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

        };
        this.setRenderer(renderer);
    }

    /**
     * Handle action events.
     *
     * @param evt the <code>ActionEvent</code> to handle
     */
    @Override
    public void actionPerformed(final ActionEvent evt) {
        if (evt.getActionCommand().equals(Commands.DATA_SOURCE_CHANGED)) {
            try {
                // Update the model with data source settings.
                this.configurationModel.removePropertyChangeListener(this);
                final DataSourceItem item = (DataSourceItem) getSelectedItem();
                if (item != null) {
                    this.configurationModel.setDataSourceType(item.type);
                    if (item.type != DataSourceType.ET_SERVER) {
                        this.configurationModel.setDataSourcePath(item.getPath());
                    }
                }
            } finally {
                this.configurationModel.addPropertyChangeListener(this);
            }
        }
    }

    /**
     * Add a data source item with a specific type and path.
     *
     * @param path the data source path
     * @param type the data source type
     * @return the new data source item
     */
    DataSourceItem addDataSourceItem(final String path, final DataSourceType type) {
        final DataSourceItem newItem = new DataSourceItem(path, new File(path).getName(), type);
        addItem(newItem);
        return newItem;
    }

    /**
     * Add a data source item. Attempting to add an item that already exists will be ignored.
     */
    @Override
    public void addItem(final DataSourceItem item) {
        if (containsItem(item)) {
            return;
        }
        if (findItem(item.getPath()) == null) {
            super.addItem(item);
        }
    }

    /**
     * Return true if the (exact) given item exists in the combo box model.
     *
     * @param item the data source item
     * @return <code>true</code> if data source item exists in the model
     */
    boolean containsItem(final DataSourceItem item) {
        return ((DefaultComboBoxModel<DataSourceItem>) getModel()).getIndexOf(item) != -1;
    }

    /**
     * Find the single data source item for the ET configuration in the items.
     *
     * @return the data source item for the ET configuration or <code>null</code> if does not exist
     */
    DataSourceItem findEtItem() {
        for (int i = 0; i < this.getItemCount(); i++) {
            final DataSourceItem item = this.getItemAt(i);
            if (item.type == DataSourceType.ET_SERVER) {
                return item;
            }
        }
        return null;
    }

    /**
     * Find an item by its path.
     *
     * @param path the path of the item
     * @return the item or <code>null</code> if does not exist
     */
    DataSourceItem findItem(final String path) {
        for (int i = 0; i < this.getItemCount(); i++) {
            final DataSourceItem item = this.getItemAt(i);
            if (item.getPath().equals(path)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Handle property change events which is used to update the GUI from changes to the global configuration model.
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        this.configurationModel.removePropertyChangeListener(this);
        try {
            if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
                final ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
                if (status.equals(ConnectionStatus.DISCONNECTED)) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_PATH_PROPERTY)) {
                if (this.configurationModel.hasValidProperty(ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY)) {
                    final String path = this.configurationModel.getDataSourcePath();
                    final DataSourceType type = DataSourceType.getDataSourceType(path);
                    if (type.isFile()) {
                        DataSourceItem item = findItem(path);
                        if (item == null) {
                            item = addDataSourceItem(path, type);
                        }
                        if (this.configurationModel.getDataSourceType().isFile()) {
                            setSelectedItem(item);
                        }
                    }
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.DATA_SOURCE_TYPE_PROPERTY)) {
                if (this.configurationModel.getDataSourceType() == DataSourceType.ET_SERVER) {
                    DataSourceItem item = findEtItem();
                    if (item == null) {
                        item = new DataSourceItem(this.configurationModel.getEtPath(),
                                this.configurationModel.getEtPath(), DataSourceType.ET_SERVER);
                    }
                    setSelectedItem(item);
                } else {
                    if (this.configurationModel.hasValidProperty(ConfigurationModel.DATA_SOURCE_PATH_PROPERTY)) {
                        DataSourceItem item = findItem(this.configurationModel.getDataSourcePath());
                        if (item == null) {
                            item = addDataSourceItem(this.configurationModel.getDataSourcePath(),
                                    this.configurationModel.getDataSourceType());
                        }
                        setSelectedItem(item);
                    }
                }
            } else if (evt.getPropertyName().equals(ConfigurationModel.HOST_PROPERTY)) {
                updateEtItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.ET_NAME_PROPERTY)) {
                updateEtItem();
            } else if (evt.getPropertyName().equals(ConfigurationModel.PORT_PROPERTY)) {
                updateEtItem();
            }
        } finally {
            this.configurationModel.addPropertyChangeListener(this);
        }
    }

    /**
     * Set the currently selected item.
     *
     * @param object the currently selected item (should be an instance of this class)
     */
    @Override
    public void setSelectedItem(final Object object) {
        super.setSelectedItem(object);
        this.setToolTipText(((DataSourceItem) object).getPath());
    }

    /**
     * Update the path value of the current ET item from the current global configuration. There is only one ET item
     * present in the list at one time so property changes to ET configuration will trigger this method.
     */
    void updateEtItem() {
        DataSourceItem item = findEtItem();
        if (item == null) {
            item = new DataSourceItem(this.configurationModel.getEtPath(), this.configurationModel.getEtPath(),
                    DataSourceType.ET_SERVER);
            addItem(item);
        } else {
            item.name = this.configurationModel.getEtPath();
        }
    }
}
