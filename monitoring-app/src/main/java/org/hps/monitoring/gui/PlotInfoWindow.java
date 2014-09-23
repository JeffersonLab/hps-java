package org.hps.monitoring.gui;

import hep.aida.IAxis;
import hep.aida.IBaseHistogram;
import hep.aida.IDataPointSet;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
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
import java.util.Timer;
import java.util.TimerTask;

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
 * Window for showing the statistics and other information about a plot.
 * <p>
 * This information will be dynamically updating using the <code>AIDAObserver</code> API
 * on the AIDA object.
 */ 
// FIXME: Add addRows for all types of AIDA objects (only Histogram1D implemented so far).
public class PlotInfoWindow extends JFrame implements AIDAListener, ActionListener {

    JComboBox<Object> plotComboBox;
    JTable infoTable = new JTable();
    DefaultTableModel model;    
    JPanel contentPane = new JPanel();
    PlotterRegion currentRegion;
    Object currentObject;
    static int INSET_SIZE = 5;

    static final String[] COLUMN_NAMES = { "Field", "Value" };

    static final String PLOT_SELECTED = "PLOT_SELECTED";
           
    @SuppressWarnings("unchecked")
    PlotInfoWindow() {
        
        contentPane.setLayout(new GridBagLayout());
        
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
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE); 
        contentPane.add(plotComboBox, c);

        String data[][] = new String[0][0];
        model = new DefaultTableModel(data, COLUMN_NAMES);
        infoTable.setModel(model);
        infoTable.getColumn("Field").setMinWidth(20);
        infoTable.getColumn("Value").setMinWidth(20);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, INSET_SIZE, 0);
        contentPane.add(infoTable, c);
        
        setContentPane(contentPane);        
        setAlwaysOnTop(true);
        //this.setResizable(false);
        this.pack();
    }
    
    /**
     * This method will be called when the backing AIDA object is updated,
     * so the information in the table should be changed to reflect its new state.
     * @param evt The EventObject pointing to the backing AIDA object.
     */
    @Override
    public void stateChanged(final EventObject evt) {        
        TimerTask task = new TimerTask() {
            public void run() {
                // Is this object connected to the correct AIDA observable?
                if (evt.getSource() != PlotInfoWindow.this.currentObject) {
                    // This should not ever happen but throw an error here just in case.
                    throw new RuntimeException("The AIDAObservable is not attached to the right object!");
                }
                
                // Run the method to update the table with new plot information on the EDT.
                runUpdateTable();
                
                // Set the observable to valid so we receive subsequent state changes.
                ((AIDAObservable) currentObject).setValid((AIDAListener) PlotInfoWindow.this);
            }
        };
        
        /* 
         * Schedule the task to run in ~0.5 seconds.  If this is run immediately, somehow the
         * observable state gets permanently set to invalid and we we will stop receiving any
         * state changes! 
         */
        new Timer().schedule(task, 500);
    }

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

    void setCurrentRegion(PlotterRegion region) {        
        if (region != currentRegion) {            
            currentRegion = region;
            if (currentRegion.title() != null)
                setTitle(currentRegion.title());
            updateComboBox();             
            setCurrentObject(plotComboBox.getSelectedItem());
            setupContentPane(); 
        }
    }

    void setupContentPane() {           
        
        plotComboBox.setSize(plotComboBox.getPreferredSize());
        infoTable.setSize(infoTable.getPreferredSize());
        int width = plotComboBox.getPreferredSize().width + INSET_SIZE * 2;
        int height = plotComboBox.getPreferredSize().height + infoTable.getPreferredSize().height + INSET_SIZE * 3;
        //System.out.println("contentPane");
        //System.out.println("  w: " + width);
        //System.out.println("  h: " + height);
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
        
    void updateTable() {
        model.setRowCount(0);
        model.setColumnIdentifiers(COLUMN_NAMES);                
        if (currentObject instanceof IHistogram1D) {            
            addRows((IHistogram1D)currentObject);
        }
    }
    
    void runUpdateTable() {
        SwingUtilities.invokeLater(new Runnable() { 
            public void run() {
                updateTable();
            }
        });
    }
    
    void updateComboBox() {
        plotComboBox.removeAllItems();
        for (ObjectStyle objectStyle : currentRegion.getObjectStyles()) {
            Object object = objectStyle.object();
            if (object instanceof IBaseHistogram) {
                this.plotComboBox.addItem(object);
            }
        }        
    }

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

    void addRow(String field, Object value) {
        model.insertRow(infoTable.getRowCount(), new Object[] { field, value });
    }
    
    void setCurrentObject(Object object) { 
        if (object == null)
            throw new IllegalArgumentException("The object arg is null!");       
        if (object == currentObject)
            return;        
        removeListener();
        currentObject = object;        
        updateTable();
        addListener();
    }
    
    void removeListener() {
        if (currentObject != null) {
            ((AIDAObservable)currentObject).removeListener(this);
        }
    }
    
    void addListener() {        
        if (currentObject instanceof AIDAObservable) {
            AIDAObservable observable = (AIDAObservable)currentObject;
            observable.addListener(this);
            observable.setValid(this);
            observable.setConnected(true);                        
        }        
    }      
}