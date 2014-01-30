package org.hps.monitoring.ecal;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;

import org.lcsim.event.EventHeader;
import org.lcsim.hps.recon.ecal.HPSCalorimeterHit;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;

/**
 * The class <code>Viewer</code> handles initialization of the calorimeter panel
 * with the proper settings, provides a window for it to live in, and feeds it
 * events.
 * 
 * @author Kyle McCarty
 **/
public class Viewer extends JFrame {
    // Java-suggested variable.
    private static final long serialVersionUID = -2022819652687941812L;
    // The calorimeter panel.
    private static final EcalPanel ecalPanel = new EcalPanel(46, 11);
    // The event data reader.
    private EventManager em = null;
    // Whether an LCIO event is being processed.
    private boolean processing = false;
    
    /**
     * <b>Viewer</b><br/><br/>
     * <code>public <b>Viewer</b>()</code><br/><br/>
     * Initializes the viewer window and calorimeter panel.
     **/
    public Viewer() {
        // Initialize the viewer window
        super();
        setTitle("HPS Ecal Cluster Viewer");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1060, 600));
        setMinimumSize(new Dimension(1060, 400));
        setLayout(null);
        
        // Set the scaling settings.
        ecalPanel.setMinimum(0.001);
        ecalPanel.setMaximum(3000);
        ecalPanel.setScalingLogarithmic();
        
        // Disable the crystals in the ecal panel along the beam gap.
        for (int i = -23; i < 24; i++) {
            ecalPanel.setCrystalEnabled(getPanelX(i), 5, false);
            if (i > -11 && i < -1) {
                ecalPanel.setCrystalEnabled(getPanelX(i), 4, false);
                ecalPanel.setCrystalEnabled(getPanelX(i), 6, false);
            }
        }
        
        // Make a key listener to change events.
        addKeyListener(new EcalListener());
        
        // Add the ecal pane
        add(ecalPanel);
        
        // Add a listener to update everything when the window changes size
        addComponentListener(new ResizeListener());
    }
    
    public void setSize(int width, int height) {
        super.setSize(width, height);
        resize();
    }
    
    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }
    
    /**
     * <b>setDataSouce</b><br/><br/>
     * <code>public void <b>setDataSource</b>(String filepath)</code><br/><br/>
     * Sets the viewer to read from the indicated data source.
     * @param filepath - The full path to the desired data file.
     * @throws IOException Occurs when there is an error opening the data file.
     **/
    public void setDataSource(String filepath) throws IOException {
        em = new EventManager(filepath);
    }
    
    public void displayLCIOEvent(EventHeader event, String ecalCollectionName, String clusterCollectionName) {
        // Make sure that a draw is not in process.
        if(processing) { return; }
        
        // Otherwise, we are now processing.
        processing = true;
        
        // Get the list of clusters and hits.
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollectionName);
        List<HPSCalorimeterHit> hits = event.get(HPSCalorimeterHit.class, ecalCollectionName);
        
        // Reset the calorimeter panel.
        ecalPanel.clearCrystals();
        
        // Add the calorimeter hits.
        for(HPSCalorimeterHit hit : hits) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            double energy = hit.getRawEnergy();
            ecalPanel.addCrystalEnergy(getPanelX(ix), getPanelY(iy), energy);
        }
        
        // Add clusters.
        for(HPSEcalCluster cluster : clusters) {
            HPSCalorimeterHit seed = (HPSCalorimeterHit) cluster.getSeedHit();
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            ecalPanel.setCrystalCluster(getPanelX(ix), getPanelY(iy), true);
        }
        
        // Redraw the panel.
        ecalPanel.redraw();
        
        // We are finished drawing.
        processing = false;
    }
    
    /**
     * <b>displayNextEvent</b><br/><br/>
     * <code>public void <b>displayNextEvent</b>()</code><br/><br/>
     * Feeds the calorimeter panel the data from the next event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public void displayNextEvent() throws IOException {
        // Clear the ecal panel.
        ecalPanel.clearCrystals();
        
        // If there is no data source, we can not do anything.
        if (em == null) { return; }
        
        // Otherwise, get the next event.
        em.readEvent();
        
        // Display it.
        for (EcalHit h : em.getHits()) {
            int ix = getPanelX(h.getX());
            int iy = getPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        for (Datum d : em.getClusters()) {
            int ix = getPanelX(d.getX());
            int iy = getPanelY(d.getY());
            ecalPanel.setCrystalCluster(ix, iy, true);
        }
        
        // Redraw the ecal panel.
        ecalPanel.redraw();
    }
    
    /**
     * <b>resize</b><br/><br/>
     * <code>private void <b>resize</b>()</code><br/><br/>
     * Handles proper resizing of the window and its components.
     **/
    private void resize() {
        // Size and position the calorimeter display
        ecalPanel.setLocation(0, 0);
        ecalPanel.setSize(getContentPane().getSize());
    }
    
    /**
     * <b>getPanelX</b><br/><br/>
     * <code>private int <b>getPanelX</b>(int ecalX)</code><br/><br/>
     * Converts the lcsim x-coordinate to the calorimeter panel's coordinate
     * system.
     * @param ecalX - An lcsim calorimeter x-coordinate.
     * @return Returns the x-coordinate in the calorimeter panel's coordinate
     * system as an <code>int</code>.
     **/
    private int getPanelX(int ecalX) {
        if (ecalX <= 0) {
            return ecalX + 23;
        } else {
            return ecalX + 22;
        }
    }
    
    /**
     * <b>getPanelY</b><br/><br/>
     * <code>private int <b>getPanelY</b>(int ecalY)</code><br/><br/>
     * Converts the lcsim y-coordinate to the calorimeter panel's coordinate
     * system.
     * @param ecalY - An lcsim calorimeter y-coordinate.
     * @return Returns the y-coordinate in the calorimeter panel's coordinate
     * system as an <code>int</code>.
     **/
    private int getPanelY(int ecalY) {
        return 5 - ecalY;
    }
    
    /**
     * The <code>EcalListener</code> class binds the enter key to the
     * <code>displayNextEvent</code> method.
     **/
    private class EcalListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
        }
        
        public void keyReleased(KeyEvent e) {
            // If enter was pressed, go to the next event.
            if (e.getKeyCode() == 10) {
                try {
                    displayNextEvent();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
        }
        
        public void keyTyped(KeyEvent e) {
        }
    }
    
    /**
     * The <code>ResizeListener</code> class ensures that the components remain
     * at the correct size and location when the window is resized.
     **/
    private class ResizeListener implements ComponentListener {
        public void componentResized(ComponentEvent e) { resize(); }
        
        public void componentHidden(ComponentEvent e) { }
        
        public void componentMoved(ComponentEvent e) { }
        
        public void componentShown(ComponentEvent e) { }
    }
}
