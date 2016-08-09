package org.hps.analysis.examples;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.math3.util.Pair;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripRunAndEventDriver extends Driver
{

    private String _eventListFileName;
    private int _nEventsToStrip;
    private int _nEventsStripped;
    Set<Pair<Integer, Integer>> _eventsToStrip = new LinkedHashSet<Pair<Integer, Integer>>();

    @Override
    protected void detectorChanged(Detector detector)
    {
        BufferedReader br = null;
        String currentLine = null;
        System.out.println("detector changed reading in "+_eventListFileName);
        try {
            br = new BufferedReader(new FileReader(_eventListFileName));
            while ((currentLine = br.readLine()) != null) {
                String runEvent = currentLine.trim();
                StringTokenizer st = new StringTokenizer(runEvent, " ");
                Integer run = Integer.parseInt(st.nextToken());
                Integer event = Integer.parseInt(st.nextToken());
                _eventsToStrip.add(new Pair(run, event));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            System.out.println("bad number format in skim list: " + currentLine);
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _nEventsToStrip = _eventsToStrip.size();
        System.out.println("preparing to strip "+_nEventsToStrip+" events");
    }

    @Override
    protected void process(EventHeader event)
    {
    }

    @Override
    protected void endOfData()
    {
        System.out.println("Stripped " + _nEventsStripped + " of " + _nEventsToStrip + " requested");
    }

    public void setEventListFileName(String s)
    {
        System.out.println("run and event list is "+s);
        _eventListFileName = s;
    }

}
