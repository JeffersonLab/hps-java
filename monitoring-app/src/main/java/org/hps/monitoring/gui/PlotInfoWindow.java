package org.hps.monitoring.gui;

import hep.aida.IAxis;
import hep.aida.IBaseHistogram;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IDataPointSet;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.jfree.plotter.ObjectStyle;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.event.AIDAListener;
import hep.aida.ref.event.AIDAObservable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * <p>
 * This is a GUI component for showing the statistics and other information about an AIDA plot.
 * <p>
 * This information is updated dynamically via the <code>AIDAObserver</code> API on the AIDA object.
 */ 
// FIXME: Add addRows for all types of AIDA objects (only Histogram1D implemented so far). 
// FIXME: Columns disappear when rebuilding table.
// TODO: Add sorting of info table.
// TODO: Probably this should be moved out of monitoring application as it is generically applicable to AIDA objects.
public class PlotInfoWindow extends JFrame implements AIDAListener, ActionListener {

    JComboBox<Object> plotComboBox;
    JTable infoTable = new JTable();
    DefaultTableModel model;    
    JPanel contentPane = new JPanel();
    PlotterRegion currentRegion;
    Object currentObject;
    static final int INSET_SIZE = 5;
    static final int BORDER_SIZE = 10;

    static final String[] COLUMN_NAMES = { "Field", "Value" };

    static final String PLOT_SELECTED = "PLOT_SELECTED";
    
    Timer timer = new Timer();
           
    /**
     * Class constructor, which will setup the GUI components.
     */
    @SuppressWarnings("unchecked")
    PlotInfoWindow() {
        
        contentPane.setLayout(new GridBagLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        
        GridBagConstraints c;
        
        plotComboBox = new JComboBox<Object>();
        plotComboBox.setActionCommand(PLOT_SELECTED);        
        plotComboBox.setRenderer(new BasicComboBoxRenderer() {
            @SuppressWarnings("rawtypes")
            public Component getListCellRendererComponent(JList list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {                
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String title = getObjectTitle(value);
                    setText(value.getClass().getSimpleName() + " - " + title);
                } else {
                    setText("Click on a plot region ...");
                }
                return this;
            }
        });        
        plotComboBox.addActionListener(this);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, INSET_SIZE, 0);
        contentPane.add(plotComboBox, c);

        String data[][] = new String[0][0];
        model = new DefaultTableModel(data, COLUMN_NAMES);
        infoTable.setModel(model);
        
        // FIXME: Are these adequate column size settings?  Could prob be bigger...
        infoTable.getColumn("Field").setMinWidth(25);
        infoTable.getColumn("Value").setMinWidth(20);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(infoTable, c);
        
        setContentPane(contentPane);        
        setAlwaysOnTop(true);
        setResizable(false);
        this.pack();
    }
    
    /**
     * This method will be called when the backing AIDA object is updated and a 
     * state change is fired via the <code>AIDAObservable</code> API.  The table
     * is updated to reflect the new state of the object.
     * @param evt The EventObject pointing to the backing AIDA object.
     */
    @Override
    public void stateChanged(final EventObject evt) {
        
        // Make a timer task for running the update.
        TimerTask task = new TimerTask() {
            public void run() {
                // Is the state change from the current AIDAObservable?
                if (evt.getSource() != PlotInfoWindow.this.currentObject) {
                    // Assume this means that a different AIDAObservable was selected in the GUI.
                    return;
                }
                
                // Update the table values on the Swing EDT.
                runUpdateTable();
                
                // Set the observable to valid so subsequent state changes are received.
                ((AIDAObservable) currentObject).setValid((AIDAListener) PlotInfoWindow.this);
            }
        };        
        
        /* 
         * Schedule the task to run in ~0.5 seconds.  If the Runnable runs immediately, somehow the
         * observable state gets permanently set to invalid and additional state changes will not
         * be received! 
         */
        timer.schedule(task, 500);
    }

    /**
     * Implementation of <code>actionPerformed</code> to handle the selection of
     * a new object from the combo box. 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Was a new item selected in the combo box?
        if (PLOT_SELECTED.equals(e.getActionCommand())) {
            if (plotComboBox.getSelectedItem() != null) {
                // Set the current object from the combo box to update the GUI state.
                setCurrentObject(plotComboBox.getSelectedItem());
            }
        }
    }        
    
    /**
     * Get the title of an AIDA object.  Unfortunately there is 
     * not base type with this information.
     * @param object The AIDA object.
     * @return The title of the object from its title method 
     *          or value of its toString method, if none exists.
     */
    String getObjectTitle(Object object) {
        if (object instanceof IBaseHistogram) {
            return ((IBaseHistogram)object).title();
        } else if (object instanceof IDataPointSet) {
            return ((IDataPointSet)object).title();            
        } else if (object instanceof IFunction) {
            return ((IFunction)object).title();
        } else {
            return object.toString();
        }
    }

    /**
     * Set the current plotter region, which will rebuild the GUI accordingly.
     * @param region The current plotter region.
     */
    synchronized void setCurrentRegion(PlotterRegion region) {        
        if (region != currentRegion) {            
            currentRegion = region;
            if (currentRegion.title() != null)
                setTitle(currentRegion.title());
            updateComboBox();             
            setCurrentObject(plotComboBox.getSelectedItem());
            setupContentPane(); 
        }
    }

    /**
     * Configure the frame's content panel from current component settings.
     */
    void setupContentPane() {                   
        plotComboBox.setSize(plotComboBox.getPreferredSize());
        infoTable.setSize(infoTable.getPreferredSize());
        int width = plotComboBox.getPreferredSize().width;
        int height = plotComboBox.getPreferredSize().height + INSET_SIZE + infoTable.getPreferredSize().height;
        contentPane.setPreferredSize(
                new Dimension(
                        width,
                        height
                        ));
        contentPane.setSize(contentPane.getPreferredSize());
        contentPane.setMinimumSize(contentPane.getPreferredSize());
        this.pack();
        setVisible(true);      
    }
        
    /**
     * Update the info table from the state of the current AIDA object.
     */
    void updateTable() {
        model.setRowCount(0);
        model.setColumnIdentifiers(COLUMN_NAMES);                
        if (currentObject instanceof IHistogram1D) {            
            addRows((IHistogram1D)currentObject);
        } else if (currentObject instanceof IHistogram2D) {
            addRows((IHistogram2D)currentObject);
        } else if (currentObject instanceof ICloud2D) {
            addRows((ICloud2D)currentObject);
        } else if (currentObject instanceof ICloud1D) {
            if (((ICloud1D)currentObject).isConverted()) {
                addRows(((ICloud1D)currentObject).histogram());
            }
        }
    }
    
    /**
     * Run the {@link #updateTable()} method on the Swing EDT.
     */
    void runUpdateTable() {
        SwingUtilities.invokeLater(new Runnable() { 
            public void run() {
                updateTable();
            }
        });
    }
    
    /**
     * Update the combo box contents with the plots from the current region.
     */
    void updateComboBox() {
        plotComboBox.removeAllItems();
        List<Object> objects = currentRegion.getPlottedObjects();        
        for (Object object : objects) {
            if (isValidObject(object)) {
                this.plotComboBox.addItem(object);
            }
        }        
    }
    
    boolean isValidObject(Object object) {
        if (object == null)
            return false;
        if (object instanceof IBaseHistogram || object instanceof IFunction || object instanceof IDataPointSet) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add rows to the info table from the state of a 1D histogram.
     * @param histogram The AIDA object.
     */
    void addRows(IHistogram1D histogram) {
        addRow("title", histogram.title());
        addRow("bins", histogram.axis().bins());
        addRow("entries", histogram.entries());
        addRow("mean", String.format("%.10f%n", histogram.mean()));
        addRow("rms", String.format("%.10f%n", histogram.rms()));
        addRow("sum bin heights", histogram.sumBinHeights());
        addRow("max bin height", histogram.maxBinHeight());
        addRow("overflow entries", histogram.binEntries(IAxis.OVERFLOW_BIN));
        addRow("underflow entries", histogram.binEntries(IAxis.UNDERFLOW_BIN));
    }
    
    /**
     * Add rows to the info table from the state of a 2D histogram.
     * @param histogram The AIDA object.
     */
    void addRows(IHistogram2D histogram) {
        addRow("title", histogram.title());
        addRow("x bins", histogram.xAxis().bins());
        addRow("y bins", histogram.yAxis().bins());
        addRow("entries", histogram.entries());
        addRow("x mean", String.format("%.10f%n", histogram.meanX()));
        addRow("y mean", String.format("%.10f%n", histogram.meanY()));
        addRow("x rms", String.format("%.10f%n", histogram.rmsX()));
        addRow("y rms", String.format("%.10f%n", histogram.rmsY()));
        addRow("sum bin heights", histogram.sumBinHeights());
        addRow("max bin height", histogram.maxBinHeight());
        addRow("x overflow entries", histogram.binEntriesX(IAxis.OVERFLOW_BIN));
        addRow("y overflow entries", histogram.binEntriesY(IAxis.OVERFLOW_BIN));
        addRow("x underflow entries", histogram.binEntriesX(IAxis.UNDERFLOW_BIN));
        addRow("y underflow entries", histogram.binEntriesY(IAxis.UNDERFLOW_BIN));
    }
    
    /**
     * Add rows to the info table from the state of a 2D cloud.
     * @param cloud The AIDA object.
     */
    void addRows(ICloud2D cloud) {        
        addRow("title", cloud.title());
        addRow("entries", cloud.entries());
        addRow("max entries", cloud.maxEntries());
        addRow("x lower edge", cloud.lowerEdgeX());
        addRow("x upper edge", cloud.upperEdgeX());
        addRow("y lower edge", cloud.lowerEdgeY());        
        addRow("y upper edge", cloud.upperEdgeY());
        addRow("x mean", String.format("%.10f%n", cloud.meanX()));
        addRow("y mean", String.format("%.10f%n", cloud.meanY()));
        addRow("x rms", String.format("%.10f%n", cloud.rmsX()));
        addRow("y rms", String.format("%.10f%n", cloud.rmsY()));
    }

    /**
     * Add a row to the info table.
     * @param field The field name.
     * @param value The field value.
     */
    void addRow(String field, Object value) {
        model.insertRow(infoTable.getRowCount(), new Object[] { field, value });
    }
    
    /**
     * Set the current AIDA object that backs this GUI, i.e. an IHistogram1D etc.
     * @param object The backing AIDA object.
     */
    synchronized void setCurrentObject(Object object) {
                        
        if (object == null)
            throw new IllegalArgumentException("The object arg is null!");       

        if (object == currentObject)
            return;
       
        // Remove the AIDAListener from the previous object.
        removeListener();
                              
        // Set the current object reference.
        currentObject = object;        
        
        // Update the table immediately with information from the current object.
        // We need to wait for this the first time, so we know the preferred size 
        // of the table GUI component when resizing the content pane.
        updateTable();
        
        // Add an AIDAListener to the AIDA object via the AIDAObservable API.
        addListener();
    }
    
    /**
     * Remove this object as an <code>AIDAListener</code> on the current <code>AIDAObservable</code>.
     */
    void removeListener() {
        if (currentObject != null) {
            // Remove this object as a listener on the current observable.
            ((AIDAObservable)currentObject).removeListener(this);
        }
    }
    
    /**
     * Add this object as an <code>AIDAListener</code> on the current <code>AIDAObservable</code>.
     */
    void addListener() {        
        if (currentObject instanceof AIDAObservable) {
            // Setup a listener on the current AIDA object.
            AIDAObservable observable = (AIDAObservable)currentObject;
            observable.addListener(this);
            observable.setValid(this);
            observable.setConnected(true);                        
        }        
    }      
}