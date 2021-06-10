package org.hps.monitoring.ecal.eventdisplay.event;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>Cluster</code> represents a cluster center and a
 * collection of additional hits that together form a hit cluster.
 */
public final class Cluster {
    private final Point center;
    private final double energy;
    private final double time;
    private ArrayList<Point> hitList = new ArrayList<Point>();
    private ArrayList<Point> shareList = new ArrayList<Point>();
    
    /**
     * Creates a new cluster. All clusters are required to have a
     * cluster center.
     * @param ix - The cluster center's x-index.
     * @param iy - The cluster center's y-index.
     */
    public Cluster(int ix, int iy) { this(new Point(ix, iy), Double.NaN, Double.NaN); }
    
    /**
     * Creates a new cluster. All clusters are required to have a seed
     * hit.
     * @param clusterCenter - The <code>Point</code> object indicating in
     * which crystal the seed hit occurred.
     */
    public Cluster(Point clusterCenter) { this(clusterCenter, Double.NaN, Double.NaN); }
    
    /**
     * Creates a new cluster. All clusters are required to have a
     * cluster center.
     * @param ix - The cluster center's x-index.
     * @param iy - The cluster center's y-index.
     * @param energy - The cluster's energy.
     */
    public Cluster(int ix, int iy, double energy) { this(new Point(ix, iy), energy, Double.NaN); }
    
    /**
     * Creates a new cluster. All clusters are required to have a
     * cluster center.
     * @param ix - The cluster center's x-index.
     * @param iy - The cluster center's y-index.
     * @param energy - The cluster's energy.
     * @param time - The cluster's time-stamp.
     */
    public Cluster(int ix, int iy, double energy, double time) { this(new Point(ix, iy), energy, time); }
    
    /**
     * Creates a new cluster. All clusters are required to have a seed
     * hit.
     * @param clusterCenter - The <code>Point</code> object indicating in
     * which crystal the seed hit occurred.
     * @param energy - The cluster's energy.
     */
    public Cluster(Point clusterCenter, double energy) {
        this(clusterCenter, energy, Double.NaN);
    }
    
    /**
     * Creates a new cluster. All clusters are required to have a seed
     * hit.
     * @param clusterCenter - The <code>Point</code> object indicating in
     * which crystal the seed hit occurred.
     * @param energy - The cluster's energy.
     * @param time - The cluster's time-stamp.
     */
    public Cluster(Point clusterCenter, double energy, double time) {
        center = clusterCenter;
        this.energy = energy;
        this.time = time;
    }
    
    /**
     * Adds an <code>Point</code> to the list of this cluster's
     * component hits.
     * @param ix - The component hit's x-coordinate.
     * @param iy - The component hit's y-coordinate.
     */
    public void addComponentHit(int ix, int iy) { hitList.add(new Point(ix, iy)); }
    
    /**
     * Adds an <code>Point</code> to the list of this cluster's
     * component hits.
     * @param eHit - The <code>Point</code> object indicating in which
     * crystal the hit occurred.
     */
    public void addComponentHit(Point eHit) { hitList.add(eHit); }
    
    /**
     * Adds an <code>Point</code> to the list of this cluster's shared
     * hits.
     * @param ix - The shared hit's x-coordinate.
     * @param iy - The shared hit's y-coordinate.
     */
    public void addSharedHit(int ix, int iy) { shareList.add(new Point(ix, iy)); }
    
    /**
     * Adds an <code>Point</code> to the list of this cluster's shared
     * hits.
     * @param eHit - The <code>Point</code> object indicating in which
     * crystal the hit occurred.
     */
    public void addSharedHit(Point eHit) { shareList.add(eHit); }
    
    /**
     * Gets the hit representing the cluster center.
     * @return Returns the cluster center hit as an <code>Point</code>.
     */
    public Point getClusterCenter() { return center; }
    
    /**
     * Gets the cluster's energy, if it was defined when the cluster
     * was constructed.
     * @return Returns the energy of the cluster if it was defined,
     * and <code>NaN</code> otherwise.
     */
    public double getClusterEnergy() { return energy; }
    
    /**
     * Gets the time stamp for the cluster in nanoseconds.
     * @return Returns the cluster's time stamp.
     */
    public double getClusterTime() { return time; }
    
    /**
     * Indicates how many component hits compose this cluster. Note
     * that this does not include the seed hit or shared hits.
     * @return Returns the number of component hits in the cluster
     * as an <code>int</code>.
     */
    public int getComponentHitCount() { return hitList.size(); }
    
    /**
     * Gets the list of hits that make up the cluster, exempting the
     * seed hit and shared hits.
     * @return Returns the cluster hits as a <code>List</code> object
     * composed of <code>Point</code> objects.
     */
    public List<Point> getComponentHits() { return hitList; }
    
    /**
     * Indicates how many total hits compose this cluster. This includes
     * component hits, shared hits, and the seed hit.
     * @return Returns the number of component hits in the cluster
     * as an <code>int</code>.
     */
    public int getHitCount() { return hitList.size() + shareList.size() + 1; }
    
    /**
     * Indicates how many shared hits compose this cluster. Note that
     * this does not include the seed hit or component hits.
     * @return Returns the number of shared hits in the cluster as an
     * <code>int</code>.
     */
    public int getSharedHitCount() { return shareList.size(); }
    
    /**
     * Gets the list of hits that make up the cluster, exempting the
     * seed hit and component hits.
     * @return Returns the shared hits as a <code>List</code> object
     * composed of <code>Point</code> objects.
     */
    public List<Point> getSharedHits() { return shareList; }
}
