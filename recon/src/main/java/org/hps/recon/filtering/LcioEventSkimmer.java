package org.hps.recon.filtering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Driver to skim selected events from LCIO files
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class LcioEventSkimmer extends Driver
{

    private Map<Integer, Set<Integer>> _eventsToSkimMap = new HashMap<Integer, Set<Integer>>();
    private boolean skipEvent = true;
    private int _numberOfEventsWritten;
    private String _inputFileName;
    private boolean _debug = false;

    @Override
    protected void startOfData()
    {
        try {
            if (_debug) {
                System.out.println(_inputFileName);
            }
            Scanner scan = new Scanner(new File(_inputFileName));
            while (scan.hasNextLine()&& !scan.nextLine().isEmpty()) {
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
    }

    @Override
    protected void process(EventHeader event)
    {
        skipEvent = true;
        int runNum = event.getRunNumber();
        int eventNum = event.getEventNumber();
        if(_eventsToSkimMap.containsKey(runNum))
        {
           if(_eventsToSkimMap.get(runNum).contains(eventNum)) skipEvent = false; 
        }
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

}
