package org.hps.monitoring.ecal.eventdisplay.ui;

import org.hps.monitoring.ecal.eventdisplay.io.EventManager;
import org.hps.monitoring.ecal.eventdisplay.io.LCIOManager;
import org.hps.monitoring.ecal.eventdisplay.io.TextManager;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.ecal.eventdisplay.event.Association;
import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * Class <code>FileViewer</code> is an implementation of the <code>
 * Viewer</code> abstract class that reads events from a file data
 * source. Any file type can be used, so long as it has a manager
 * which implements the <code>EventManager</code> interface.
 * 
 * @author Kyle McCarty
 */
public class FileViewer extends Viewer {
    private static final long serialVersionUID = 3L;
    
    // Gets events from some file.
    protected EventManager em = null;
    
    // File chooser for opening new files.
    private JFileChooser fileChooser;
    
    // Map cluster location to a cluster object.
    private HashMap<Point, Cluster> clusterMap = new HashMap<Point, Cluster>();
    private HashMap<Point, Double> crystalTimeMap = new HashMap<Point, Double>();
    
    // Additional status display field names for this data type.
    private static final String[] fieldNames = { "Event Number", "Shared Hits", "Component Hits", "Hit Time", "Cluster Energy", "Cluster Time" };
    
    // Indices for the field values.
    private static final int EVENT_NUMBER   = 0;
    private static final int SHARED_HITS    = 1;
    private static final int COMPONENT_HITS = 2;
    private static final int HIT_TIME       = 3;
    private static final int CLUSTER_ENERGY = 4;
    private static final int CLUSTER_TIME   = 5;
    
    /**
     * Constructs a new <code>Viewer</code> for displaying data read
     * from a file.
     * @param dataSource - The <code>EventManager</code> responsible
     * for reading data from a file.
     * @throws NullPointerException Occurs if the event manager is
     * <code>null</code>.
     */
    public FileViewer(File dataSource) throws NullPointerException {
        // Initialize the superclass.
        super();
        
        // Make a key listener to change events.
        addKeyListener(new EcalKeyListener());
        
        // Add additional fields.
        insertStatusField(0, fieldNames[0]);
        for(int index = 1; index < fieldNames.length; index++) {
            addStatusField(fieldNames[index]);
        }
        
        // Initialize the file chooser.
        fileChooser = new JFileChooser(new File("D:\\cygwin64\\home\\Kyle\\background\\compiled\\output\\")); 
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        //fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("LCIO Files", "lcio", "slcio"));
        
        // Add an open file option to the file menu.
        JMenuItem menuOpen = new JMenuItem("Open File", KeyEvent.VK_O);
        menuOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open the file chooser and note what action was taken.
                int result = fileChooser.showOpenDialog(FileViewer.this);
                
                // If a file was selected, load it.
                if(result == JFileChooser.APPROVE_OPTION) {
                    // Get the selected data file.
                    File dataFile = fileChooser.getSelectedFile();
                    
                    // Load the indicated file.
                    openDataSource(dataFile);
                }
            }
        });
        menu[MENU_FILE].addSeparator();
        menu[MENU_FILE].add(menuOpen);
        
        // Add an exit command to the file menu.
        JMenuItem menuExit = new JMenuItem("Exit", KeyEvent.VK_X);
        menuExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        menu[MENU_FILE].add(menuExit);
        
        // Set the calorimeter panel to display a logarithmic scale.
        setUseLogarithmicScale();
        
        // Set the data source.
        if(dataSource != null) { openDataSource(dataSource); }
    }
    
    /**
     * Feeds the calorimeter panel the data from the next event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     */
    public void displayNextEvent() throws IOException { getEvent(true); }
    
    /**
     * Feeds the calorimeter panel the data from the previous event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     */
    public void displayPreviousEvent() throws IOException { getEvent(false); }
    
    @Override
    protected void updateStatusPanel() {
        // Update the superclass status fields.
        super.updateStatusPanel();
        
        // Get the currently selected crystal.
        Point crystal = ecalPanel.getSelectedCrystal();
        
        // If the active crystal is not null, see if it is a cluster.
        if(crystal != null) {
            // Get the cluster associated with this point.
            Cluster activeCluster = clusterMap.get(crystal);
            
            // If the cluster is null, we set everything to undefined.
            if(activeCluster == null) {
                for(String field : fieldNames) { setStatusField(field, ResizableFieldPanel.NULL_VALUE); }
            }
            
            // Otherwise, define the fields based on the cluster.
            else {
                // Get the shared and component hit counts.
                setStatusField(fieldNames[SHARED_HITS], Integer.toString(activeCluster.getSharedHitCount()));
                setStatusField(fieldNames[COMPONENT_HITS], Integer.toString(activeCluster.getComponentHitCount()));
                
                // Format the cluster energy, or account for it if it
                // doesn't exist.
                String energy;
                if(activeCluster.getClusterEnergy() != Double.NaN) {
                    DecimalFormat formatter = new DecimalFormat("0.####E0");
                    energy = formatter.format(activeCluster.getClusterEnergy());
                }
                else { energy = ResizableFieldPanel.NULL_VALUE; }
                setStatusField(fieldNames[CLUSTER_ENERGY], energy);
                
                // Format the cluster time, or account for it if it
                // doesn't exist.
                String time;
                if(activeCluster.getClusterTime() != Double.NaN) {
                    time = Double.toString(activeCluster.getClusterTime());
                }
                else { time = ResizableFieldPanel.NULL_VALUE; }
                setStatusField(fieldNames[CLUSTER_TIME], time);
            }
            
            // If there is a hit time associated with this point, it should
            // be displayed.
            Double hitTime = crystalTimeMap.get(crystal);
            if(hitTime != null) {
                setStatusField(fieldNames[HIT_TIME], Double.toString(hitTime));
            } else {
                setStatusField(fieldNames[HIT_TIME], ResizableFieldPanel.NULL_VALUE);
            }
        }
        // Otherwise, clear the field values.
        else { for(String field : fieldNames) { setStatusField(field, ResizableFieldPanel.NULL_VALUE); } }
        
        // Set the event number.
        if(em != null) { setStatusField(fieldNames[EVENT_NUMBER], Integer.toString(em.getEventNumber())); }
        else { setStatusField(fieldNames[EVENT_NUMBER], ResizableFieldPanel.NULL_VALUE); }
    }
    
    /**
     * Displays the given lists of hits and clusters on the calorimeter
     * panel.
     * @param hitList - A list of hits for the current event.
     * @param clusterList  - A list of clusters for the current event.
     */
    private void displayEvent(List<EcalHit> hitList, List<Cluster> clusterList) {
        // Suppress the calorimeter panel's redrawing.
        ecalPanel.setSuppressRedraw(true);
        
        // Display the hits.
        for (EcalHit h : hitList) {
            int ix = toPanelX(h.getX());
            int iy = toPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        
        // Display the clusters.
        for(Cluster cluster : clusterList) {
            Point rawCluster = cluster.getClusterCenter();
            Point clusterCenter = toPanelPoint(rawCluster);
            ecalPanel.setCrystalCluster(clusterCenter.x, clusterCenter.y, true);
            
            // Add component hits to the calorimeter panel.
            for(Point ch : cluster.getComponentHits()) {
                ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(ch), HIGHLIGHT_CLUSTER_COMPONENT));
            }
            
            // Add shared hits to the calorimeter panel.
            for(Point sh : cluster.getSharedHits()) {
                ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(sh), HIGHLIGHT_CLUSTER_SHARED));
            }
        }
        
        // Stop suppressing the redraw and order the panel to update.
        ecalPanel.setSuppressRedraw(false);
        ecalPanel.repaint();
        
        // Update the status panel to account for the new event.
        updateStatusPanel();
    }
    
    /**
     * Reads either the next or the previous event from the event manager.
     * @param forward - Whether the event data should be read forward
     * or backward.
     * @throws IOException Occurs when there is an issue with reading the data file.
     */
    private void getEvent(boolean forward) throws IOException {
        // Clear the calorimeter panel.
        ecalPanel.clearCrystals();
        
        // If there is no data source, we can not do anything.
        if (em == null) { return; }
        
        // Otherwise, get the next event.
        if(forward) { em.nextEvent(); }
        else { em.previousEvent(); }
        
        // Load the cluster map.
        clusterMap.clear();
        for(Cluster c : em.getClusters()) { clusterMap.put(toPanelPoint(c.getClusterCenter()), c); }
        
        // Load hit time map.
        crystalTimeMap.clear();
        for(EcalHit hit : em.getHits()) {
            crystalTimeMap.put(new Point(toPanelX(hit.getX()), toPanelY(hit.getY())), hit.getTime());
        }
        
        // Display it.
        displayEvent(em.getHits(), em.getClusters());
    }
    
    /**
     * Loads a data source for viewing, if it is a supported data type.
     * @param dataFile - The file to load.
     * @return Returns <code>true</code> if the file was successfully
     * loaded and <code>false</code> if it was not.
     */
    private boolean openDataSource(File dataFile) {
        // Get the file extension.
        int point = dataFile.getName().lastIndexOf('.');
        String extension = "";
        if(point != -1 && point != dataFile.getName().length()) {
            extension = dataFile.getName().substring(point + 1);
        }
        
        // Open the file with a manager appropriate to it.
        EventManager manager = null;
        if(extension.compareToIgnoreCase("lcio") == 0 || extension.compareToIgnoreCase("slcio") == 0) {
            // Try to open it.
            try { manager = new LCIOManager(dataFile); }
            catch(IOException exc) {
                System.err.println("Error reading data file.");
            }
        }
        else if(extension.compareToIgnoreCase("txt") == 0) {
            try { manager = new TextManager(dataFile); }
            catch(IOException exc) {
                System.err.println("Error reading data file.");
            }
        }
        else { System.err.println("Unrecognized file type."); }
        
        // If the file was successfully loaded, set it as
        // the current data manager and display the first
        // event.
        if(manager != null) {
            em = manager;
            displayEvent(manager.getHits(), manager.getClusters());
            setTitle("HPS Ecal Event Display - " + dataFile.getName());
            displayEvent(em.getHits(), em.getClusters());
            updateStatusPanel();
            return true;
        }
        
        // Otherwise, report that the file could not be opened.
        setTitle("HPS Ecal Event Display");
        updateStatusPanel();
        return false;
    }
    
    /**
     * The <code>EcalListener</code> class binds keys to actions.
     * Bound actions include:
     * [Right Arrow] :: Next event
     * [Left Arrow ] :: Previous event
     **/
    private class EcalKeyListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            // If right-arrow was pressed, go to the next event.
            if (e.getKeyCode() == 39) {
                try { displayNextEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // If left-arrow was pressed, go to the next event.
            else if (e.getKeyCode() == 37) {
                try { displayPreviousEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // Otherwise, print out the key code for the pressed key.
            else { System.out.printf("Key Code: %d%n", e.getKeyCode()); }
        }
    }
}