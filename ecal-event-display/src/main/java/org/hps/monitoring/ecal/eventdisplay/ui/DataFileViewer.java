package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.hps.monitoring.ecal.eventdisplay.util.CrystalDataSet;
import org.hps.monitoring.ecal.eventdisplay.util.EcalWiringManager;

/**
 * Class <code>DataFileViewer</code> is the active variant of a data
 * viewer. It displays crystal hardware information read from a given
 * data file and displays it along with crystal energy and index data.
 */
public class DataFileViewer extends FileViewer {
    // Local variables.
    private static final long serialVersionUID = 1L;
    private final EcalWiringManager ewm;
    
    // Hardware display fields.
    private static final String[] fieldNames = {
        "APD Number", "Preamp Number", "LED Channel", "LED Driver",
        "FADC Slot", "FADC Channel", "Splitter Number", "HV Group",
        "Jout", "MB", "Channel", "Gain"
    };
    
    // Hardware display field indices.
    private static final int FIELD_APD = 0;
    private static final int FIELD_PREAMP = 1;
    private static final int FIELD_LED_CHANNEL = 2;
    private static final int FIELD_LED_DRIVER = 3;
    private static final int FIELD_FADC_SLOT = 4;
    private static final int FIELD_FADC_CHANNEL = 5;
    private static final int FIELD_SPLITTER = 6;
    private static final int FIELD_HV_GROUP = 7;
    private static final int FIELD_JOUT = 8;
    private static final int FIELD_MB = 9;
    private static final int FIELD_CHANNEL = 10;
    private static final int FIELD_GAIN = 11;
    
    // Filter panel components.
    private JFrame filterWindow;
    private CrystalFilterPanel filterPanel;
    
    /**
     * Initializes a new <code>DataFileViewer</code> that reads from
     * the given event manager for event data and the given hardware
     * data file for crystal hardware data readout.
     * @param dataSource - The manager for event data.
     * @param crystalDataFilePath - The data file for crystal hardware
     * information.
     * @throws IOException Occurs if there is an error reading from
     * either data source.
     */
    public DataFileViewer(File dataSource, String crystalDataFilePath) throws IOException {
        // Initialize the super class file.
        super(dataSource);
        
        // Load the crystal data mapping.
        ewm = new EcalWiringManager(crystalDataFilePath);
        
        // Add the crystal data fields.
        for(String fieldName : fieldNames) { addStatusField(fieldName); }
        
        // Instantiate the crystal filter panel.
        filterPanel = new CrystalFilterPanel(ewm);
        filterWindow = new JFrame("Event Display Crystal Filter");
        filterWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        filterWindow.add(filterPanel);
        filterWindow.pack();
        filterWindow.setResizable(false);
        
        // Add a new view menu option to display the filter panel.
        JMenuItem menuFilter = new JMenuItem("Show Filter", KeyEvent.VK_F);
        menuFilter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        menuFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { filterWindow.setVisible(true); }
        });
        menu[MENU_VIEW].addSeparator();
        menu[MENU_VIEW].add(menuFilter);
        
        // Add an action listener to note when the filter window applies
        // a crystal filter.
        filterPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Suppress panel redrawing until the highlights are set.
                ecalPanel.setSuppressRedraw(true);
                
                // Clear the panel highlighting.
                ecalPanel.clearHighlight();
                
                // If the filter panel is active, highlight the crystals
                // that passed the filter.
                if(filterPanel.isActive()) {
                    // Get the list of filtered crystals.
                    List<Point> filterList = filterPanel.getFilteredCrystals();
                    
                    // Highlight each of the filtered crystals.
                    for(Point crystal : filterList) {
                        ecalPanel.setCrystalHighlight(toPanelPoint(crystal), java.awt.Color.WHITE);
                    }
                }
                
                // Redraw the highlights.
                ecalPanel.setSuppressRedraw(false);
                ecalPanel.repaint();
            }
        });
        
        // Kill the filter window on system close.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { filterWindow.dispose(); }
        });
    }
    
    @Override
    protected void updateStatusPanel() {
        // Run the superclass method.
        super.updateStatusPanel();
        
        // Get the selected crystal.
        Point crystal = ecalPanel.getSelectedCrystal();
        
        // If a crystal is selected, display its data set.
        if(crystal != null) {
            // Get the LCSim coordinate system version of the crystal.
            Point lcsimCrystal = Viewer.toEcalPoint(crystal);
            
            // Get the hardware data set associated with the crystal.
            CrystalDataSet cds = ewm.getCrystalData(lcsimCrystal);
            
            // If the data set exists, update the all the fields.
            if(cds != null) {
                setStatusField(fieldNames[FIELD_APD], "" + cds.getAPDNumber());
                setStatusField(fieldNames[FIELD_PREAMP], cds.getPreamplifierNumber().toString());
                setStatusField(fieldNames[FIELD_LED_CHANNEL], "" + cds.getLEDChannel());
                setStatusField(fieldNames[FIELD_LED_DRIVER], "" + cds.getLEDDriver());
                setStatusField(fieldNames[FIELD_FADC_SLOT], "" + cds.getFADCSlot());
                setStatusField(fieldNames[FIELD_FADC_CHANNEL], "" + cds.getFADCChannel());
                setStatusField(fieldNames[FIELD_SPLITTER], "" + cds.getSplitterNumber());
                setStatusField(fieldNames[FIELD_HV_GROUP], "" + cds.getHighVoltageGroup());
                setStatusField(fieldNames[FIELD_JOUT], "" + cds.getJout());
                setStatusField(fieldNames[FIELD_MB], "" + cds.getMotherboard().toString());
                setStatusField(fieldNames[FIELD_CHANNEL], "" + cds.getChannel());
                setStatusField(fieldNames[FIELD_GAIN], "" + cds.getGain());
            }
        }
    }
}