package org.hps.monitoring.application;

import hep.aida.IAxis;
import hep.aida.IBaseHistogram;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IDataPointSet;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.event.AIDAListener;
import hep.aida.ref.event.AIDAObservable;
import hep.aida.ref.function.FunctionChangedEvent;
import hep.aida.ref.function.FunctionDispatcher;
import hep.aida.ref.function.FunctionListener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * <p>
 * This is a GUI component for showing the statistics and other information about an AIDA plot when it is clicked on in
 * the monitoring application.
 * <p>
 * The information in the table is updated dynamically via the <code>AIDAObserver</code> API on the AIDA object.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class PlotInfoPanel extends JPanel implements AIDAListener, ActionListener, FunctionListener, AddActionListener {

    /**
     * The names of the two columns.
     */
    private static final String[] COLUMN_NAMES = {"Field", "Value"};

    /**
     * The maximum number of rows in the table.
     */
    private static final int MAX_ROWS = 13;

    /**
     * Minimum height of panel in pixels.
     */
    private static final int MIN_HEIGHT = 300;

    /**
     * Minimum width of panel in pixels.
     */
    private static final int MIN_WIDTH = 400;

    /**
     * The currently selected AIDA object.
     */
    private Object currentObject;

    /**
     * The currently selected plotter region.
     */
    private PlotterRegion currentRegion;

    /**
     * The table showing plot statistics.
     */
    private final JTable infoTable = new JTable();

    /**
     * The default table model.
     */
    private DefaultTableModel model;

    /**
     * The combo box for selecting the object from the region.
     */
    private JComboBox<Object> plotComboBox;

    /**
     * The button for saving plot graphics..
     */
    private JButton saveButton;

    /**
     * The timer for updating the table.
     */
    private final Timer timer = new Timer();

    /**
     * Class constructor, which will setup the GUI components.
     */
    @SuppressWarnings({"unchecked"})
    PlotInfoPanel() {

        this.setLayout(new FlowLayout(FlowLayout.CENTER));

        final JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
        leftPanel.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        final Dimension filler = new Dimension(0, 10);

        // Save button.
        this.saveButton = new JButton("Save Current Plot Tab ...");
        this.saveButton.setActionCommand(Commands.SAVE_SELECTED_PLOTS);
        this.saveButton.setAlignmentX(CENTER_ALIGNMENT);
        leftPanel.add(this.saveButton);

        leftPanel.add(new Box.Filler(filler, filler, filler));

        // Combo box for selecting plotted object.
        this.plotComboBox = new JComboBox<Object>() {

            @Override
            public Dimension getMaximumSize() {
                final Dimension max = super.getMaximumSize();
                max.height = this.getPreferredSize().height;
                return max;
            }
        };
        this.plotComboBox.setActionCommand(Commands.PLOT_SELECTED);
        this.plotComboBox.setAlignmentX(CENTER_ALIGNMENT);
        this.plotComboBox.setRenderer(new BasicComboBoxRenderer() {

            @Override
            @SuppressWarnings("rawtypes")
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    final String title = PlotInfoPanel.this.getObjectTitle(value);
                    this.setText(value.getClass().getSimpleName() + " - " + title);
                } else {
                    this.setText("Click on a plot region ...");
                }
                return this;
            }
        });
        this.plotComboBox.addActionListener(this);
        leftPanel.add(this.plotComboBox);

        leftPanel.add(new Box.Filler(filler, filler, filler));

        // Table with plot info.
        final String[][] data = new String[0][0];
        this.model = new DefaultTableModel(data, COLUMN_NAMES);
        this.model.setColumnIdentifiers(COLUMN_NAMES);
        this.infoTable.setModel(this.model);
        ((DefaultTableModel) this.infoTable.getModel()).setRowCount(MAX_ROWS);
        this.infoTable.setAlignmentX(CENTER_ALIGNMENT);
        leftPanel.add(this.infoTable);

        this.add(leftPanel);
    }

    /**
     * Implementation of <code>actionPerformed</code> to handle the selection of a new object from the combo box.
     *
     * @param e the {@link java.awt.event.ActionEvent} to handle
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        // Is there a new item selected in the combo box?
        if (Commands.PLOT_SELECTED.equals(e.getActionCommand())) {
            if (this.plotComboBox.getSelectedItem() != null) {
                // Set the current object from the combo box value, to update the GUI state.
                this.setCurrentObject(this.plotComboBox.getSelectedItem());
            }
        }
    }

    /**
     * Assign an action listener to certain components.
     *
     * @param listener the action listener to add
     */
    @Override
    public void addActionListener(final ActionListener listener) {
        this.saveButton.addActionListener(listener);
    }

    /**
     * Add this object as an <code>AIDAListener</code> on the current <code>AIDAObservable</code>.
     */
    private void addListener() {
        if (this.currentObject instanceof AIDAObservable && !(this.currentObject instanceof FunctionDispatcher)) {
            // Setup a listener on the current AIDA object.
            final AIDAObservable observable = (AIDAObservable) this.currentObject;
            observable.addListener(this);
            observable.setValid(this);
            observable.setConnected(true);
        } else if (this.currentObject instanceof IFunction) {
            if (this.currentObject instanceof FunctionDispatcher) {
                ((FunctionDispatcher) this.currentObject).addFunctionListener(this);
            }
        }
    }

    /**
     * Add a row to the info table.
     *
     * @param field the field name
     * @param value the field value
     */
    private void addRow(final String field, final Object value) {
        this.model.insertRow(this.infoTable.getRowCount(), new Object[] {field, value});
    }

    /**
     * Add rows to the info table from the state of a 2D cloud.
     *
     * @param cloud the AIDA object
     */
    private void addRows(final ICloud2D cloud) {
        this.addRow("title", cloud.title());
        this.addRow("entries", cloud.entries());
        this.addRow("max entries", cloud.maxEntries());
        this.addRow("x lower edge", cloud.lowerEdgeX());
        this.addRow("x upper edge", cloud.upperEdgeX());
        this.addRow("y lower edge", cloud.lowerEdgeY());
        this.addRow("y upper edge", cloud.upperEdgeY());
        this.addRow("x mean", String.format("%.10f%n", cloud.meanX()));
        this.addRow("y mean", String.format("%.10f%n", cloud.meanY()));
        this.addRow("x rms", String.format("%.10f%n", cloud.rmsX()));
        this.addRow("y rms", String.format("%.10f%n", cloud.rmsY()));
    }

    /**
     * Add rows to the info table from the state of a function.
     *
     * @param function the AIDA function object
     */
    private void addRows(final IFunction function) {
        this.addRow("title", function.title());

        // Add generically the values of all function parameters.
        for (final String parameter : function.parameterNames()) {
            this.addRow(parameter, function.parameter(parameter));
        }
    }

    /**
     * Add rows to the info table from the state of a 1D histogram.
     *
     * @param histogram the AIDA object
     */
    private void addRows(final IHistogram1D histogram) {
        this.addRow("title", histogram.title());
        this.addRow("bins", histogram.axis().bins());
        this.addRow("entries", histogram.entries());
        this.addRow("mean", String.format("%.10f%n", histogram.mean()));
        this.addRow("rms", String.format("%.10f%n", histogram.rms()));
        this.addRow("sum bin heights", histogram.sumBinHeights());
        this.addRow("max bin height", histogram.maxBinHeight());
        this.addRow("overflow entries", histogram.binEntries(IAxis.OVERFLOW_BIN));
        this.addRow("underflow entries", histogram.binEntries(IAxis.UNDERFLOW_BIN));
    }

    /**
     * Add rows to the info table from the state of a 2D histogram.
     *
     * @param histogram the AIDA object
     */
    private void addRows(final IHistogram2D histogram) {
        this.addRow("title", histogram.title());
        this.addRow("x bins", histogram.xAxis().bins());
        this.addRow("y bins", histogram.yAxis().bins());
        this.addRow("entries", histogram.entries());
        this.addRow("x mean", String.format("%.10f%n", histogram.meanX()));
        this.addRow("y mean", String.format("%.10f%n", histogram.meanY()));
        this.addRow("x rms", String.format("%.10f%n", histogram.rmsX()));
        this.addRow("y rms", String.format("%.10f%n", histogram.rmsY()));
        this.addRow("sum bin heights", histogram.sumBinHeights());
        this.addRow("max bin height", histogram.maxBinHeight());
        this.addRow("x overflow entries", histogram.binEntriesX(IAxis.OVERFLOW_BIN));
        this.addRow("y overflow entries", histogram.binEntriesY(IAxis.OVERFLOW_BIN));
        this.addRow("x underflow entries", histogram.binEntriesX(IAxis.UNDERFLOW_BIN));
        this.addRow("y underflow entries", histogram.binEntriesY(IAxis.UNDERFLOW_BIN));
    }

    /**
     * Callback for updating from changed to <code>IFunction</code> object.
     *
     * @param event the change event (unused in this method)
     */
    @Override
    public void functionChanged(final FunctionChangedEvent event) {
        try {
            this.runUpdateTable();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the title of an AIDA object. Unfortunately there is no base type with this information, so it is read
     * manually from each possible type.
     *
     * @param object the AIDA object
     * @return the title of the object from its title method or value of its <code>toString</code> method, if none
     * exists
     */
    private String getObjectTitle(final Object object) {
        if (object instanceof IBaseHistogram) {
            return ((IBaseHistogram) object).title();
        } else if (object instanceof IDataPointSet) {
            return ((IDataPointSet) object).title();
        } else if (object instanceof IFunction) {
            return ((IFunction) object).title();
        } else {
            return object.toString();
        }
    }

    /**
     * Return <code>true</code> if the object is a valid AIDA object.
     *
     * @param object the object
     * @return <code>true</code> if object is a valid AIDA object
     */
    private boolean isValidObject(final Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof IBaseHistogram || object instanceof IFunction || object instanceof IDataPointSet) {
            return true;
        }
        return false;
    }

    /**
     * Remove this as a listener on the current AIDA object.
     */
    private void removeListener() {
        if (this.currentObject != null) {
            if (this.currentObject instanceof AIDAObservable && !(this.currentObject instanceof IFunction)) {
                // Remove this object as an listener on the AIDA observable.
                ((AIDAObservable) this.currentObject).removeListener(this);
            } else if (this.currentObject instanceof FunctionDispatcher) {
                // Remove this object as function listener.
                ((FunctionDispatcher) this.currentObject).removeFunctionListener(this);
            }
        }
    }

    /**
     * Run the {@link #updateTable()} method on the Swing EDT.
     */
    private void runUpdateTable() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                PlotInfoPanel.this.updateTable();
            }
        });
    }

    /**
     * Set the current AIDA object that backs this GUI, i.e. an IHistogram1D or other plot object etc.
     *
     * @param object the backing AIDA object
     */
    private synchronized void setCurrentObject(final Object object) {

        if (object == null) {
            throw new IllegalArgumentException("The object arg is null!");
        }

        if (object == this.currentObject) {
            return;
        }

        // Remove the AIDAListener from the previous object.
        this.removeListener();

        // Set the current object reference.
        this.currentObject = object;

        // Update the table immediately with information from the current object.
        // We need to wait for this the first time, so we know the preferred size
        // of the table GUI component when resizing the content pane.
        this.updateTable();

        // Add an AIDAListener to the AIDA object via the AIDAObservable API.
        this.addListener();
    }

    /**
     * Set the current plotter region, which will rebuild the GUI accordingly.
     *
     * @param region The current plotter region.
     */
    synchronized void setCurrentRegion(final PlotterRegion region) {
        if (region != this.currentRegion) {
            this.currentRegion = region;
            this.updateComboBox();
            this.setCurrentObject(this.plotComboBox.getSelectedItem());
            this.setupContentPane();
        }
    }

    /**
     * Configure the frame's content panel from current component settings.
     */
    private void setupContentPane() {
        this.plotComboBox.setSize(this.plotComboBox.getPreferredSize());
        this.infoTable.setSize(this.infoTable.getPreferredSize());
        this.setVisible(true);
    }

    /**
     * This method will be called when the backing AIDA object is updated and a state change is fired via the
     * <code>AIDAObservable</code> API. The table is updated to reflect the new state of the object.
     *
     * @param evt the EventObject pointing to the backing AIDA object
     */
    @Override
    public void stateChanged(final EventObject evt) {

        // Make a timer task for running the update.
        final TimerTask task = new TimerTask() {

            @Override
            public void run() {

                // Is the state change from the current AIDAObservable?
                if (evt.getSource() != PlotInfoPanel.this.currentObject) {
                    // Assume this means that a different AIDAObservable was selected in the GUI.
                    return;
                }

                // Update the table values on the Swing EDT.
                PlotInfoPanel.this.runUpdateTable();

                // Set the observable to valid so subsequent state changes are received.
                ((AIDAObservable) PlotInfoPanel.this.currentObject).setValid(PlotInfoPanel.this);
            }
        };

        /*
         * Schedule the task to run in ~0.5 seconds. If the Runnable runs immediately, somehow the observable state gets
         * permanently set to invalid and additional state changes will not be received!
         */
        this.timer.schedule(task, 500);
    }

    /**
     * Update the combo box contents with the plots from the current region.
     */
    private void updateComboBox() {
        this.plotComboBox.removeAllItems();
        final List<Object> objects = this.currentRegion.getPlottedObjects();
        for (final Object object : objects) {
            if (this.isValidObject(object)) {
                this.plotComboBox.addItem(object);
            }
        }
    }

    /**
     * Update the info table from the state of the current AIDA object.
     */
    private void updateTable() {
        this.model.setRowCount(0);
        if (this.currentObject instanceof IHistogram1D) {
            this.addRows((IHistogram1D) this.currentObject);
        } else if (this.currentObject instanceof IHistogram2D) {
            this.addRows((IHistogram2D) this.currentObject);
        } else if (this.currentObject instanceof ICloud2D) {
            this.addRows((ICloud2D) this.currentObject);
        } else if (this.currentObject instanceof ICloud1D) {
            if (((ICloud1D) this.currentObject).isConverted()) {
                this.addRows(((ICloud1D) this.currentObject).histogram());
            }
        } else if (this.currentObject instanceof IFunction) {
            this.addRows((IFunction) this.currentObject);
        }
    }
}
