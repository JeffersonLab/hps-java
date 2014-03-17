package org.hps.monitoring.ecal.event;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Event</code> stores all the information the <code>
 * CalorimeterPanel</code> needs to display an event.
 * @author Kyle McCarty
 */
public final class Event {
	private List<EcalHit> hitList;
	private List<Point> clusterList;
	private List<Association> connectList;
	
	/**
	 * <b>Event</b><br/><br/>
	 * <code>public <b>Event</b>()</code><br/><br/>
	 * Creates a new <code>Event</code>.
	 */
	public Event() { this(10, 10, 10); }
	
	/**
	 * <b>Event</b><br/><br/>
	 * <code>public <b>Event</b>(int hits, int clusters)</code><br/><br/>
	 * Creates a new <code>Event</code> and reserves spaces for the
	 * given number of hits and cluster centers.
	 * @param hits - The number of hits for which to reserve space.
	 * @param clusters - The number of cluster centers for which to
	 * reserve space.
	 */
	public Event(int hits, int clusters) { this(hits, clusters, 10); }
	
	/**
	 * <b>Event</b><br/><br/>
	 * <code>public <b>Event</b>(int hits, int clusters, int associations)</code><br/><br/>
	 * Creates a new <code>Event</code> and reserves spaces for the
	 * given number of hits, cluster centers, and crystal associations.
	 * @param hits - The number of hits for which to reserve space.
	 * @param clusters - The number of cluster centers for which to
	 * reserve space.
	 * @param associations - The number of crystal associations for
	 * which to reserve space.
	 */
	public Event(int hits, int clusters, int associations) {
		hitList = new ArrayList<EcalHit>(hits);
		clusterList = new ArrayList<Point>(clusters);
		connectList = new ArrayList<Association>(associations);
	}
	
	/**
	 * <b>Event</b><br/><br/>
	 * <code>public <b>Event</b>(List<EcalHit> hits, List<Point> clusters, List<Association> associations)</code><br/><br/>
	 * Creates a new <code>Event</code> and sets its contents to those
	 * of the given lists. The crystal association list will be empty.
	 * @param hits - The list of calorimeter hits.
	 * @param clusters - The list of cluster centers.
	 */
	public Event(List<EcalHit> hits, List<Point> clusters) {
		this(hits, clusters, new ArrayList<Association>());
	}
	
	/**
	 * <b>Event</b><br/><br/>
	 * <code>public <b>Event</b>(List<EcalHit> hits, List<Point> clusters, List<Association> associations)</code><br/><br/>
	 * Creates a new <code>Event</code> and sets its contents to those
	 * of the given lists.
	 * @param hits - The list of calorimeter hits.
	 * @param clusters - The list of cluster centers.
	 * @param associations - The list of crystal associations.
	 */
	public Event(List<EcalHit> hits, List<Point> clusters, List<Association> associations) {
		hitList = hits;
		clusterList = clusters;
		connectList = associations;
	}
	
	/**
	 * <b>addAssociation</b><br/><br/>
	 * <code>public void <b>addAssociation</b>(Association connectedCrystal)</code><br/><br/>
	 * Adds a crystal association to the event.
	 * @param connectedCrystal - The crystal association to add.
	 */
	public void addAssociation(Association connectedCrystal) {
		connectList.add(connectedCrystal);
	}
	
	/**
	 * <b>addCluster</b><br/><br/>
	 * <code>public void <b>addCluster</b>(Point cluster)</code><br/><br/>
	 * Adds a cluster center to the event.
	 * @param cluster - The cluster center to add.
	 */
	public void addCluster(Point cluster) { clusterList.add(cluster); }
	
	/**
	 * <b>addHit</b><br/><br/>
	 * <code>public void <b>addHit</b>(EcalHit hit)</code><br/><br/>
	 * Adds a calorimeter hit to the event.
	 * @param hit - The calorimeter hit to add.
	 */
	public void addHit(EcalHit hit) { hitList.add(hit); }
	
	/**
	 * <b>getAssociations</b><br/><br/>
	 * <code>public List<Association> <b>getAssociations</b>()</code><br/><br/>
	 * Gets the list of associated crystals for this event.
	 * @return Returns the associations in a <code>List</code>.
	 */
	public List<Association> getAssociations() { return connectList; }
	
	/**
	 * <b>getClusterCenters</b><br/><br/>
	 * <code>List<Cluster><b>getClusterCenters</b>()</code><br/><br/>
	 * Gets the list of cluster centers for this event.
	 * @return Returns the cluster centers in a <code>List</code>.
	 */
	public List<Point> getClusterCenters() { return clusterList; }
	
	/**
	 * <b>getHits</b><br/><br/>
	 * <code>public List<EcalHit> <b>getHits</b>()</code><br/><br/>
	 * Gets the list of calorimeter hits for this event.
	 * @return Returns the hits in a <code>List</code>.
	 */
	public List<EcalHit> getHits() { return hitList; }
}
