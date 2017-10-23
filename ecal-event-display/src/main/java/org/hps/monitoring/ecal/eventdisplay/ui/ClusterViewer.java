package org.hps.monitoring.ecal.eventdisplay.ui;

import org.hps.monitoring.ecal.eventdisplay.io.EventManager;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

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
@Deprecated
public class ClusterViewer extends ActiveViewer {

    private static final long serialVersionUID = 17058336873349781L;
    // Stores whether the background color is set or not.
    private boolean background = false;
    // Store the index in the buffer of the displayed event.
    private int bufferIndex;
    // Map cluster location to a cluster object.
    private HashMap<Point, Cluster> clusterMap = new HashMap<Point, Cluster>();
    // Store the cluster list for the currently displayed event.
    private List<Cluster> eventClusters;
    // Store the event energies in a buffer.
    private LinkedList<Double[][]> eventEnergyBuffer;
    // Store the event hits in a buffer.
    private LinkedList<List<EcalHit>> eventHitBuffer;
    // Store the size of the event window.
    private int eventWindow;
    // Additional status display field names for this data type.
    private static final String[] fieldNames = {"Shared Hits", "Component Hits", "Cluster Energy", "Buffer Index"};

    /**
     * <b>ClusterViewer</b><br/>
     * <br/>
     * <code>public <b>ClusterViewer</b>()</code><br/>
     * <br/>
     * Constructs a new <code>Viewer</code> for displaying data read
     * from a file.
     * 
     * @param dataSource - The <code>EventManager</code> responsible
     * for reading data from a file.
     * @throws NullPointerException Occurs if the event manager is <code>null</code>.
     */
    public ClusterViewer(EventManager dataSource, int eventWindow) throws NullPointerException {
        // Initialize the superclass.
        super(dataSource);

        // Add the additional fields.
        for (String field : fieldNames) {
            addStatusField(field);
        }

        // Define the event window and initialize the event data.
        this.eventWindow = eventWindow;
        eventEnergyBuffer = new LinkedList<Double[][]>();
        eventHitBuffer = new LinkedList<List<EcalHit>>();

        // Prepare the event buffer to display the first event.
        try {
            // Make an empty array. At the start, there are no previous
            // events to load.
            Double[][] emptyArray = new Double[46][11];
            for (int x = 0; x < 46; x++) {
                for (int y = 0; y < 11; y++) {
                    emptyArray[x][y] = new Double(0.0);
                }
            }

            // Populate the eventWindow before section of the buffer
            // with the empty events.
            for (int i = 0; i <= eventWindow; i++) {
                eventEnergyBuffer.addFirst(emptyArray);
                eventHitBuffer.addFirst(new ArrayList<EcalHit>());
            }

            // Fill the rest of the array with future events.
            for (int i = 0; i < eventWindow; i++) {
                em.nextEvent();
                eventEnergyBuffer.addFirst(toEnergyArray(em.getHits()));
                eventHitBuffer.addFirst(em.getHits());
            }
        } catch (java.io.IOException e) {
            System.exit(1);
        }

        // Make a key listener to change events.
        addKeyListener(new EcalKeyListener());
    }

    /**
     * Feeds the calorimeter panel the data from the next event.
     * 
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    @Override
    public void displayNextEvent() throws IOException {
        getEvent(true);
    }

    /**
     * Feeds the calorimeter panel the data from the previous event.
     * 
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    @Override
    public void displayPreviousEvent() throws IOException {
        getEvent(false);
    }

    /**
     * Generates a list of clusters from the list of hits in the event.
     * This was used as a debugging method for the current clustering
     * algorithm.
     * 
     * @return Returns a generated list of clusters.
     */
    public List<Cluster> getClusters() {
        // Get the set of hits in the middle of the buffer. This is
        // the "current" event.
        List<EcalHit> activeEvent = eventHitBuffer.get(eventWindow);

        // Store clusters.
        ArrayList<Cluster> clusterList = new ArrayList<Cluster>();

        // For each hit, check if it meets the criteria for a cluster.
        for (EcalHit hit : activeEvent) {
            // Track whether this hit is a cluster.
            boolean isCluster = true;

            // Track the current hit's cluster energy.
            double clusterEnergy = 0.0;

            // Convert the current hit to the proper coordinates.
            Point hitLoc = toPanelPoint(hit.getLocation());

            // Track which crystals are part of the cluster.
            HashSet<Point> componentSet = new HashSet<Point>();

            // Get the set of the current hit's neighbors.
            Set<Point> neighbors = ecalPanel.getNeighbors(hitLoc);

            // Loop through the buffer and perform comparisons.
            for (Double[][] event : eventEnergyBuffer) {
                // Increment the cluster energy by the hit's energy at
                // the current time in the buffer.
                clusterEnergy += event[hitLoc.x][hitLoc.y];

                // A hit must be larger than itself at all other times
                // stored in the buffer.
                if (event[hitLoc.x][hitLoc.y] > hit.getEnergy()) {
                    isCluster = false;
                    break;
                }

                // A hit must be larger than its immediate neighbors
                // at all times in the buffer as well.
                for (Point neighbor : neighbors) {
                    // Increment the cluster energy by the neighbor's
                    // energy at the current time in the buffer.
                    clusterEnergy += event[neighbor.x][neighbor.y];

                    // Check that the neighbor's energy is not higher
                    // than the present hit's.
                    if (event[neighbor.x][neighbor.y] > hit.getEnergy()) {
                        isCluster = false;
                        break;
                    }

                    // If this neighbor has a non-zero energy, it is
                    // a component of the potential cluster.
                    if (event[neighbor.x][neighbor.y] != 0) {
                        componentSet.add(neighbor);
                    }
                }
            }

            // If the current hit did not fail any of the preceding
            // checks, then it is a cluster and should be added to
            // the cluster list.
            if (isCluster) {
                Cluster cluster = new Cluster(hit.getLocation(), clusterEnergy);
                for (Point neighbor : componentSet) {
                    cluster.addComponentHit(toEcalPoint(neighbor));
                }
                clusterList.add(cluster);
            }
        }

        // Return the list of clusters.
        return clusterList;
    }

    @Override
    protected void updateStatusPanel() {
        super.updateStatusPanel();

        // Get the currently selected crystal.
        Point crystal = ecalPanel.getSelectedCrystal();

        // If the active crystal is not null, see if it is a cluster.
        if (crystal != null) {
            // Get the cluster associated with this point.
            Cluster activeCluster = clusterMap.get(crystal);

            // If the cluster is null, we set everything to undefined.
            if (activeCluster == null) {
                for (String field : fieldNames) {
                    setStatusField(field, StatusPanel.NULL_VALUE);
                }
            }

            // Otherwise, define the fields based on the cluster.
            else {
                // Get the shared and component hit counts.
                setStatusField(fieldNames[0], Integer.toString(activeCluster.getSharedHitCount()));
                setStatusField(fieldNames[1], Integer.toString(activeCluster.getComponentHitCount()));

                // Format the cluster energy, or account for it if it
                // doesn't exist.
                String energy;
                if (activeCluster.getClusterEnergy() != Double.NaN) {
                    DecimalFormat formatter = new DecimalFormat("0.####E0");
                    energy = formatter.format(activeCluster.getClusterEnergy());
                } else {
                    energy = "---";
                }
                setStatusField(fieldNames[2], energy);
            }
        }
        // Otherwise, clear the field values.
        else {
            for (String field : fieldNames) {
                setStatusField(field, StatusPanel.NULL_VALUE);
            }
        }

        // Write the current buffer index.

        setStatusField(fieldNames[3], Integer.toString(eventWindow - bufferIndex));
    }

    /**
     * <b>displayEvent</b><br/>
     * <br/>
     * <code>private void <b>displayEvent</b></code><br/>
     * <br/>
     * Displays the given lists of hits and clusters on the calorimeter
     * panel.
     * 
     * @param hitList - A list of hits for the current event.
     * @param clusterList - A list of clusters for the current event.
     */
    private void displayEvent(List<EcalHit> hitList, List<Cluster> clusterList) {
        // Suppress the calorimeter panel.
        ecalPanel.setSuppressRedraw(true);

        // Display the hits.
        for (EcalHit h : hitList) {
            int ix = toPanelX(h.getX());
            int iy = toPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }

        // Display the clusters.
        for (Cluster cluster : clusterList) {
            Point rawCluster = cluster.getClusterCenter();
            Point clusterCenter = toPanelPoint(rawCluster);
            ecalPanel.setCrystalCluster(clusterCenter.x, clusterCenter.y, true);

            // Add component hits to the calorimeter panel.
            for (Point ch : cluster.getComponentHits()) {
                ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(ch), HIGHLIGHT_CLUSTER_COMPONENT));
            }

            // Add shared hits to the calorimeter panel.
            for (Point sh : cluster.getSharedHits()) {
                ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(sh), HIGHLIGHT_CLUSTER_SHARED));
            }
        }

        // Stop suppressing the panel and order it to redraw.
        ecalPanel.setSuppressRedraw(false);
        ecalPanel.repaint();

        // Update the status panel to account for the new event.
        updateStatusPanel();
    }

    /**
     * <b>getEvent</b><br/>
     * <br/>
     * <code>private void <b>getEvent</b>(boolean forward)</code><br/>
     * <br/>
     * Reads either the next or the previous event from the event manager.
     * 
     * @param forward - Whether the event data should be read forward
     * or backward.
     * @throws IOException Occurs when there is an issue with reading the data file.
     */
    private void getEvent(boolean forward) throws IOException {
        // Clear the calorimeter panel.
        ecalPanel.clearCrystals();

        // If there is no data source, we can not do anything.
        if (em == null) {
            return;
        }

        // Otherwise, get the next event.
        if (forward) {
            em.nextEvent();
        } else {
            em.previousEvent();
        }

        // Remove the last buffer event and add the new one.
        if (forward) {
            eventEnergyBuffer.removeLast();
            eventHitBuffer.removeLast();
            eventEnergyBuffer.addFirst(toEnergyArray(em.getHits()));
            eventHitBuffer.addFirst(em.getHits());
        } else {
            eventEnergyBuffer.removeFirst();
            eventHitBuffer.removeFirst();
            eventEnergyBuffer.addLast(toEnergyArray(em.getHits()));
            eventHitBuffer.addLast(em.getHits());
        }

        // Determine if any of the hits in the active event are
        // clusters. The active event is the event in the middle of
        // the event buffer (i.e. index eventWindow).
        eventClusters = getClusters();

        // Load the cluster map.
        clusterMap.clear();
        for (Cluster c : eventClusters) {
            clusterMap.put(toPanelPoint(c.getClusterCenter()), c);
        }

        // Update the displayed buffer index.
        bufferIndex = eventWindow;

        // Display it.
        displayEvent(eventHitBuffer.get(eventWindow), eventClusters);
    }

    /**
     * Gets the energy that should be stored in each crystal of the
     * calorimeter.
     * 
     * @param hits - The list of hits for the event.
     * @return Returns the energy of each crystal as an array of <code>
     * Double</code> objects.
     */
    private Double[][] toEnergyArray(List<EcalHit> hits) {
        // Define the energy array.
        Double[][] energy = new Double[46][11];
        for (int x = 0; x < energy.length; x++) {
            for (int y = 0; y < energy[x].length; y++) {
                energy[x][y] = new Double(0);
            }
        }

        // For each hit, place its energy in the array.
        for (EcalHit hit : hits) {
            // Get the converted crystal index.
            Point panelLoc = toPanelPoint(hit.getLocation());

            // Add the energy to the array.
            energy[panelLoc.x][panelLoc.y] += hit.getEnergy();
        }

        // Return the resulting array.
        return energy;
    }

    /**
     * The <code>EcalListener</code> class binds keys to actions.
     * Bound actions include:
     * [Right Arrow] :: Next event (stand-alone mode only)
     * [Left Arrow ] :: Previous event (stand-alone mode only)
     * b :: Toggle color-mapping for 0 energy crystals
     * h :: Toggle selected crystal highlighting
     * l :: Toggle logarithmic versus linear scaling
     * s :: Saves the current display to a file
     **/
    private class EcalKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // If right-arrow was pressed, go to the next event.
            if (e.getKeyCode() == 39) {
                try {
                    displayNextEvent();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }

            // If left-arrow was pressed, go to the previous event.
            else if (e.getKeyCode() == 37) {
                // try { displayPreviousEvent(); }
                // catch (IOException ex) {
                // System.err.println(ex.getMessage());
                // System.exit(1);
                // }
            }

            // If the down-arrow was pressed, move down a time step in
            // the buffer and display it.
            else if (e.getKeyCode() == 40) {
                if (bufferIndex == eventHitBuffer.size() - 1) {
                    return;
                } else {
                    bufferIndex++;
                    ecalPanel.clearCrystals();
                    displayEvent(eventHitBuffer.get(bufferIndex), eventClusters);
                }
            }

            // If the up-arrow was pressed, move up a time step in
            // the buffer and display it.
            else if (e.getKeyCode() == 38) {
                if (bufferIndex == 0) {
                    return;
                } else {
                    bufferIndex--;
                    ecalPanel.clearCrystals();
                    displayEvent(eventHitBuffer.get(bufferIndex), eventClusters);
                }
            }

            // 'b' toggles the default white background.
            else if (e.getKeyCode() == 66) {
                if (background) {
                    ecalPanel.setDefaultCrystalColor(null);
                } else {
                    ecalPanel.setDefaultCrystalColor(Color.GRAY);
                }
                background = !background;
            }

            // 'h' toggles highlighting the crystal under the cursor.
            else if (e.getKeyCode() == 72) {
                ecalPanel.setSelectionHighlighting(!ecalPanel.isSelectionEnabled());
            }

            // 'l' toggles linear or logarithmic scaling.
            else if (e.getKeyCode() == 76) {
                if (ecalPanel.isScalingLinear()) {
                    ecalPanel.setScalingLogarithmic();
                } else {
                    ecalPanel.setScalingLinear();
                }
            }

            // 's' saves the panel to a file.
            else if (e.getKeyCode() == 83) {
                // Make a new buffered image on which to draw the content pane.
                BufferedImage screenshot = new BufferedImage(getContentPane().getWidth(), getContentPane().getHeight(),
                        BufferedImage.TYPE_INT_ARGB);

                // Paint the content pane to image.
                getContentPane().paint(screenshot.getGraphics());

                // Get the lowest available file name.
                int fileNum = 0;
                File imageFile = new File("screenshot_" + fileNum + ".png");
                while (imageFile.exists()) {
                    fileNum++;
                    imageFile = new File("screenshot_" + fileNum + ".png");
                }

                // Save the image to a PNG file.
                try {
                    ImageIO.write(screenshot, "PNG", imageFile);
                } catch (IOException ioe) {
                    System.err.println("Error saving file \"screenshot.png\".");
                }
                System.out.println("Screenshot saved to: " + imageFile.getAbsolutePath());
            }

            // Otherwise, print out the key code for the pressed key.
            else {
                System.out.printf("Key Code: %d%n", e.getKeyCode());
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
}
