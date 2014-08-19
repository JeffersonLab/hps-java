package org.hps.monitoring.ecal.event;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * The class <code>Cluster</code> represents a cluster center and a
 * collection of additional hits that together form a hit cluster.
 * 
 * @author Kyle McCarty
 */
public final class Cluster {
	private final Point center;
	private final double energy;
	private ArrayList<Point> hitList = new ArrayList<Point>();
	private ArrayList<Point> shareList = new ArrayList<Point>();
	
	/**
	 * <b>Cluster</b><br/><br/>
	 * <code>public <b>Cluster</b>(int ix, int iy)</code><br/><br/>
	 * Creates a new cluster. All clusters are required to have a
	 * cluster center.
	 * @param ix - The cluster center's x-index.
	 * @param iy - The cluster center's y-index.
	 */
	public Cluster(int ix, int iy) { this(new Point(ix, iy), Double.NaN); }
	
	/**
	 * <b>Cluster</b><br/><br/>
	 * <code>public <b>Cluster</b>(Point clusterCenter)</code><br/><br/>
	 * Creates a new cluster. All clusters are required to have a seed
	 * hit.
	 * @param clusterCenter - The <code>Point</code> object indicating in
	 * which crystal the seed hit occurred.
	 */
	public Cluster(Point clusterCenter) { this(clusterCenter, Double.NaN); }
	
	/**
	 * <b>Cluster</b><br/><br/>
	 * <code>public <b>Cluster</b>(int ix, int iy)</code><br/><br/>
	 * Creates a new cluster. All clusters are required to have a
	 * cluster center.
	 * @param ix - The cluster center's x-index.
	 * @param iy - The cluster center's y-index.
	 * @param energy - The cluster's energy.
	 */
	public Cluster(int ix, int iy, double energy) { this(new Point(ix, iy), energy); }
	
	/**
	 * <b>Cluster</b><br/><br/>
	 * <code>public <b>Cluster</b>(Point clusterCenter)</code><br/><br/>
	 * Creates a new cluster. All clusters are required to have a seed
	 * hit.
	 * @param clusterCenter - The <code>Point</code> object indicating in
	 * which crystal the seed hit occurred.
	 * @param energy - The cluster's energy.
	 */
	public Cluster(Point clusterCenter, double energy) {
		center = clusterCenter;
		this.energy = energy;
	}
	
	/**
	 * <b>addComponentHit</b><br/><br/>
	 * <code>public void <b>addComponentHit</b>(int ix, int iy)</code><br/><br/>
	 * Adds an <code>Point</code> to the list of this cluster's
	 * component hits.
	 * @param ix - The component hit's x-coordinate.
	 * @param iy - The component hit's y-coordinate.
	 */
	public void addComponentHit(int ix, int iy) { hitList.add(new Point(ix, iy)); }
	
	/**
	 * <b>addComponentHit</b><br/><br/>
	 * <code>public void <b>addComponentHit</b>(Point eHit)</code><br/><br/>
	 * Adds an <code>Point</code> to the list of this cluster's
	 * component hits.
	 * @param eHit - The <code>Point</code> object indicating in which
	 * crystal the hit occurred.
	 */
	public void addComponentHit(Point eHit) { hitList.add(eHit); }
	
	/**
	 * <b>addSharedHit</b><br/><br/>
	 * <code>public void <b>addSharedHit</b>(int ix, int iy)</code><br/><br/>
	 * Adds an <code>Point</code> to the list of this cluster's shared
	 * hits.
	 * @param ix - The shared hit's x-coordinate.
	 * @param iy - The shared hit's y-coordinate.
	 */
	public void addSharedHit(int ix, int iy) { shareList.add(new Point(ix, iy)); }
	
	/**
	 * <b>addSharedHit</b><br/><br/>
	 * <code>public void <b>addSharedHit</b>(Point eHit)</code><br/><br/>
	 * Adds an <code>Point</code> to the list of this cluster's shared
	 * hits.
	 * @param eHit - The <code>Point</code> object indicating in which
	 * crystal the hit occurred.
	 */
	public void addSharedHit(Point eHit) { shareList.add(eHit); }
	
	/**
	 * <b>getClusterCenter</b><br/><br/>
	 * <code>public Point <b>getClusterCenter</b>()</code><br/><br/>
	 * Gets the hit representing the cluster center.
	 * @return Returns the cluster center hit as an <code>Point</code>.
	 */
	public Point getClusterCenter() { return center; }
	
	/**
	 * <b>getClusterEnergy</b><br/><br/>
	 * <code>public double <b>getClusterEnergy</b>()</code><br/><br/>
	 * Gets the cluster's energy, if it was defined when the cluster
	 * was constructed.
	 * @return Returns the energy of the cluster if it was defined,
	 * and <code>NaN</code> otherwise.
	 */
	public double getClusterEnergy() { return energy; }
	
	/**
	 * <b>getComponentHitCount</b><br/><br/>
	 * <code>public int <b>getComponentHitCount</b>()</code><br/><br/>
	 * Indicates how many component hits compose this cluster. Note
	 * that this does not include the seed hit or shared hits.
	 * @return Returns the number of component hits in the cluster
	 * as an <code>int</code>.
	 */
	public int getComponentHitCount() { return hitList.size(); }
	
	/**
	 * <b>getComponentHits</b><br/><br/>
	 * <code>public List<Point> <b>getComponentHits</b>()</code><br/><br/>
	 * Gets the list of hits that make up the cluster, exempting the
	 * seed hit and shared hits.
	 * @return Returns the cluster hits as a <code>List</code> object
	 * composed of <code>Point</code> objects.
	 */
	public List<Point> getComponentHits() { return hitList; }
	
	/**
	 * <b>getHitCount</b><br/><br/>
	 * <code>public int <b>getHitCount</b>()</code><br/><br/>
	 * Indicates how many total hits compose this cluster. This includes
	 * component hits, shared hits, and the seed hit.
	 * @return Returns the number of component hits in the cluster
	 * as an <code>int</code>.
	 */
	public int getHitCount() { return hitList.size() + shareList.size() + 1; }
	
	/**
	 * <b>getSharedHitCount</b><br/><br/>
	 * <code>public int <b>getSharedHitCount</b>()</code><br/><br/>
	 * Indicates how many shared hits compose this cluster. Note that
	 * this does not include the seed hit or component hits.
	 * @return Returns the number of shared hits in the cluster as an
	 * <code>int</code>.
	 */
	public int getSharedHitCount() { return shareList.size(); }
	
	/**
	 * <b>getSharedHits</b><br/><br/>
	 * <code>public List<Point> <b>getSharedHits</b>()</code><br/><br/>
	 * Gets the list of hits that make up the cluster, exempting the
	 * seed hit and component hits.
	 * @return Returns the shared hits as a <code>List</code> object
	 * composed of <code>Point</code> objects.
	 */
	public List<Point> getSharedHits() { return shareList; }
}
