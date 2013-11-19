package org.lcsim.hps.monitoring;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * This is the GUI component for displaying event information in real time.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: EventPanel.java,v 1.16 2013/11/05 17:15:04 jeremy Exp $
 */
class EventPanel extends FieldsPanel { 

    private JTextField eventCounterField; // number of events in this job
    private JTextField elapsedTimeField; // elapsed time between job start
    private JTextField avgEventRateField; // average event rate in this job
    private JTextField refreshField; // number of events to wait before updating GUI
    private JTextField badEventsField; // number of bad events where event event processing failed
    private JTextField sessionSuppliedField; // number of events supplied in this session; ignored by reset command
    private JTextField totalSuppliedField; // number of events supplied since the application started
    private JTextField maxEventsField; // maximum number of events to process before disconenct
    private JTextField runNumberField; // Run number from CODA Pre Start event.
    private JTextField runStartField; // Start of run date from CODA Pre Start event.
    private JTextField runEventsField; // Number of events in run.
    private JTextField runStopField; // End of run date from CODA End Event.
    
    private static final int defaultEventRefresh = 1;
    private int eventRefresh = defaultEventRefresh;
    private int eventCount;
    private int badEventCount;
    private int sessionSupplied;
    private int totalSupplied;
    private boolean updateEvent = true;
    
    private final static DecimalFormat rateFormat = new DecimalFormat("#.##");
    
    final static SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss");
 
    /**
     * Class constructor.
     */
    EventPanel() {
        
        super(new Insets(5, 5, 5, 5), false);

        setLayout(new GridBagLayout());

        elapsedTimeField = addField("Elapsed Time [seconds]", "0", 10);
        eventCounterField = addField("Events Processed", "0", 10);
        avgEventRateField = addField("Average Events Per Second", "0", 6);
        badEventsField = addField("Event Errors", "0", 8); 
        runNumberField = addField("Run Number", "", 8);
        runStartField = addField("Run Started", "", 22);
        runStopField = addField("Run Stopped", "", 22);
        runEventsField = addField("Events in Run", "", 10);
        sessionSuppliedField = addField("Session Supplied Events", "0", 8);
        totalSuppliedField = addField("Total Supplied Events", "0", 8);
        refreshField = addField("Event Refresh", Integer.toString(eventRefresh), 8);
        maxEventsField = addField("Max Events", "-1", 8);
    }
            
    /**
     * Get the event refresh rate.
     * @return The event refresh rate.
     */
    int getEventRefresh() {
    	return eventRefresh;
    }
    
    /**
     * Get the max events setting from its GUI component.
     * @return The max events.
     */
    int getMaxEvents() {
        return Integer.parseInt(maxEventsField.getText());
    }
    
    /**
     * Set the max events.
     * @param maxEvents The max events.
     */
    void setMaxEvents(final int maxEvents) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                maxEventsField.setText(Integer.toString(maxEvents));
            }
        });
    }
    
    /**
     * Set the run number.
     * @param runNumber The run number.
     */
    void setRunNumber(final int runNumber) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                runNumberField.setText(Integer.toString(runNumber));
            }
        });
    }
    
    /**
     * Set the number of events in the run.
     * @param eventCount The number of events in the run.
     */
    void setRunEventCount(final int eventCount) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                runEventsField.setText(Integer.toString(eventCount));
            }
        });
    }
    
    /**
     * Set the run start time in milliseconds since the epoch (Unix time).
     * @param millis The run start time.
     */
    void setRunStartTime(final long millis) {
        final Date d = new Date(millis);
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               runStartField.setText(dateFormat.format(d));
           }
        });
    }
    
    /**
     * Set the run end time in milliseconds.
     * @param millis The run end time.
     */
    void setRunEndTime(final long millis) {
        final Date d = new Date(millis);
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               runStopField.setText(dateFormat.format(d));
           }
        });
    }
    
    /**
     * Check if the panel should be updated for this event.
     */
    private void checkUpdateEvent() {
        updateEvent = ((eventCount % getEventRefresh()) == 0);
    }
    
    /**
     * At end of job, final event totals are pushed to the GUI. 
     */
    void endJob() {
        incrementEventCounts();
    }
    
    /**
     * Update the event counters.
     */
    void updateEventCount() {
        ++eventCount;
        ++sessionSupplied;
        ++totalSupplied;
        checkUpdateEvent();
        if (updateEvent) {
            incrementEventCounts();
        }
    }
    
    /**
     * Increment the event counts in the GUI.  This happens when the event
     * is flagged for updating.
     */
    private void incrementEventCounts() {
        Runnable r = new Runnable() {
            public void run() {
                eventCounterField.setText(Integer.toString(eventCount));
                sessionSuppliedField.setText(Integer.toString(sessionSupplied));
                totalSuppliedField.setText(Integer.toString(totalSupplied));
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Increment the number of bad events.
     */
    void updateBadEventCount() {
    	++badEventCount;
    	SwingUtilities.invokeLater(new Runnable() {
    	    public void run() {
    	        badEventsField.setText(Integer.toString(badEventCount));
    	    }
    	});
    }
    
    /**
     * Update the average event rate.
     * @param jobStartTime The start time of the job in milliseconds.
     */
    void updateAverageEventRate(long jobStartTime) {
        if (updateEvent) {
            final double jobTime = System.currentTimeMillis() - jobStartTime;
            if (jobTime > 0) {
                final double jobSeconds = jobTime / 1000;
                final double eventsPerSecond = eventCount / jobSeconds;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        avgEventRateField.setText(rateFormat.format(eventsPerSecond));
                    }
                });
            }
        }
    }
     
    /**
     * Set the elapsed time for the session in milliseconds.
     * @param time The elapsed time.
     */
    void setElapsedTime(final long time) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                elapsedTimeField.setText(Long.toString(time));
            }
        });       
    }
        
    /**
     * Set the event refresh rate.
     * @param eventRefresh The event refresh rate.
     */
    void setEventRefresh(final int eventRefresh) {
        this.eventRefresh = eventRefresh;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshField.setText(Integer.toString(eventRefresh));
            }
        });                
    }
        
    /**
     * Reset all the event counts.
     */
    synchronized void reset() {
        resetEventCount();
        resetBadEventCount();
        resetAverageEventRate();
        resetElapsedTime();
    }
	    
    /**
     * Reset the event counter.
     */
    private void resetEventCount() {
        eventCount = 0;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                eventCounterField.setText("0");
            }
        });  		
    }

    /**
     * Reset the elapsed time field.
     */
    private void resetElapsedTime() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                elapsedTimeField.setText("0");
            }
        });
    }   

    /**
     * Reset the average event rate.
     */
    private void resetAverageEventRate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                avgEventRateField.setText("0");
            }
        });	
    }

    /**
     * Reset the bad event count.
     */
    private void resetBadEventCount() {
        this.badEventCount = 0;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                badEventsField.setText("0");
            }
        });
    }

    /**
     * Reset the session supplied events.
     */
    void resetSessionSupplied() {
        this.sessionSupplied = 0;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sessionSuppliedField.setText("0");
            }
        });
    }	
}