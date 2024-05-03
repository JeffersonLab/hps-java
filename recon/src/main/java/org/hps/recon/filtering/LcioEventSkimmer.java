package org.hps.recon.filtering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;


import hep.physics.vec.BasicHep3Vector;

// Reconstructed objects
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Cluster;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

/**
 * Driver to skim selected events from LCIO files
 */
public class LcioEventSkimmer extends Driver
{

    private Map<Integer, Set<Integer>> _eventsToSkimMap = new HashMap<Integer, Set<Integer>>();
    private boolean skipEvent = true;
    private int _numberOfEventsWritten;
    private String _inputFileName = "";
    private boolean _debug = false;
    private String _particlesCollectionName = "FinalStateParticles_KF";
    private String _selection = "V0";

    @Override
    protected void startOfData()
    {

	//Check if the input file list is empty:

	if (!_inputFileName.isEmpty()) {
	    
	    try {
		if (_debug) {
		    System.out.println(_inputFileName);
		}
		Scanner scan = new Scanner(new File(_inputFileName));
		while (scan.hasNextInt()) {
		    int runNum = scan.nextInt();
		    int eventNum = scan.nextInt();
		    if (_debug) {
			System.out.println("run: " + runNum + " event " + eventNum);
		    }
		    if (_eventsToSkimMap.containsKey(runNum)) {
			_eventsToSkimMap.get(runNum).add(eventNum);
		    } else {
			_eventsToSkimMap.put(runNum, new TreeSet<Integer>());
			_eventsToSkimMap.get(runNum).add(eventNum);
		    }
		}
		scan.close();
	    } catch (FileNotFoundException ex) {
		Logger.getLogger(LcioEventSkimmer.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    if (_debug) {
		System.out.println(_eventsToSkimMap);
	    }
	} // input list name is not empty.
    }

    @Override
    protected void process(EventHeader event)
    {
        skipEvent = true;
	int runNum = event.getRunNumber();
        int eventNum = event.getEventNumber();
	
	
	//Keep events according to the external list
	if (!_inputFileName.isEmpty()) {
	    if (_eventsToSkimMap.containsKey(runNum)) {
		if (_eventsToSkimMap.get(runNum).contains(eventNum)) {
		    skipEvent = false;
		}
	    }
	}
	
	//Keep events if they pass an event selection
	List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class,_particlesCollectionName);
	
	//Skip event

	skipEvent = selectEvent(particles);
	
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Selected " + _numberOfEventsWritten + " events");
    }

    public void setRunAndEventsToStripFileName(String s)
    {
        _inputFileName = s;
    }

    public void setDebug(boolean b)
    {
        _debug = b;
    }

    public void setParticlesCollectionName(String s){
	_particlesCollectionName = s;
    }

    public void setSelection(String s) {
	_selection = s;
    }
	

    // V0: select for 1e+ in the event
    //                Clusters in the fiducial region
    //                10+ hits on track
    //                p > 1GeV
    
    private boolean selectEvent(List<ReconstructedParticle> particles) {
	
	boolean skipEvent = true;
	
	if (_selection == "V0") {
	    
	    if (particles.size() < 2)
		return skipEvent;

	    boolean found_positron = false;
	    for (ReconstructedParticle particle : particles) {
		if (particle.getCharge() > 0) {
		    found_positron = true;
		    break;
		}
	    }
	    
	    if (!found_positron)
		return skipEvent;
	    
	    for (ReconstructedParticle particle : particles) {
		
		// Get the cluster from the particle and check that it's in the fiducial region
		
		if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
		    return skipEvent;
		
		Cluster cluster = particle.getClusters().get(0);
		
		if (!TriggerModule.inFiducialRegion(cluster))
		    return skipEvent;
		
		Track track = particle.getTracks().get(0);
		TrackState trackState = track.getTrackStates().get(0);
		double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
		
		if (track.getTrackerHits().size() < 10 || trackp < 1.)
		    return skipEvent;
		
	    }
	    
	    // If event survived, do not skip
	    skipEvent = false;

	} // V0 selection
	
	return skipEvent;
    }
} // LcioEventSkimmer
