package org.jlab.coda.jevio.graphics;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This is a panel that displays evio event info (event source, dictionary source, event number, and
 * number of events) at the top of the GUI. 
 */
public class EventInfoPanel extends JPanel {

    /**
     * A label for displaying the current event source name.
     */
    private NamedLabel eventSourceLabel;

    /**
     * A label for displaying the dictionary source.
     */
    private NamedLabel dictionaryLabel;

    /**
     * A label for displaying the number of events in an event file or the number of
     * cMsg messages in a queue.
     */
    private NamedLabel numEventsLabel;

    /**
     * A label for displaying the ordinal number of the event from an event file.
     */
    private NamedLabel eventNumberLabel;


    /**
     * Create the panel that goes in the north - top of the GUI. This will hold 4 labels.
     * One showing the current event source. The second showing the current dictionary source.
     * The third showing the event number, and the fourth showing the number of events.
     *
     * @return the panel.
     */
    public EventInfoPanel() {

        setLayout(new GridLayout(2, 1, 0, 3)); // rows, cols, hgap, vgap
        setBorder(new EmptyBorder(5, 200, 5, 0)); // top, left, bot, right

        eventSourceLabel = new NamedLabel("event source", "event_source", 430);
        dictionaryLabel  = new NamedLabel("dictionary", "event_source", 430);

        eventNumberLabel = new NamedLabel("event#", "num_events", 150);
        numEventsLabel   = new NamedLabel("num events", "num_events", 150);

        // limit size of labels
        Dimension d1 = eventSourceLabel.getPreferredSize();
        Dimension d2 = eventNumberLabel.getPreferredSize();

        eventSourceLabel.setMaximumSize(d1);
        dictionaryLabel.setMaximumSize(d1);
        eventNumberLabel.setMaximumSize(d2);
        numEventsLabel.setMaximumSize(d2);

        // two sub panels
        JPanel p0 = new JPanel();
        p0.setLayout(new BoxLayout(p0, BoxLayout.X_AXIS));
		p0.add(eventSourceLabel);
        p0.add(Box.createRigidArea(new Dimension(5,0)));
		p0.add(eventNumberLabel);

        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.add(dictionaryLabel);
        p1.add(Box.createRigidArea(new Dimension(5,0)));
		p1.add(numEventsLabel);

		add(p0);
		add(p1);
    }

    /**
     * Set this panel's displayed values.
     *
     * @param source source of viewed event.
     * @param eventNumber event number of event currently displayed.
     * @param numberOfEvents number of events if file, else 1.
     * @param dictionary dictionary source: name of dictionary file or,
     *                   "in message" if dictionary came from cMsg message.
     */
    public void setDisplay(String source, int eventNumber, int numberOfEvents, String dictionary) {
        if (source != null) {
            eventSourceLabel.setText(source);
        }
        if (dictionary != null) {
            dictionaryLabel.setText(dictionary);
        }
        eventNumberLabel.setText("" + eventNumber);
        numEventsLabel.setText("" + numberOfEvents);
    }

    /**
     * Set some of this panel's displayed values.
     *
     * @param source source of viewed event.
     * @param numberOfEvents number of events if file, else 1.
     */
    public void setDisplay(String source, int numberOfEvents) {
        if (source != null) {
            eventSourceLabel.setText(source);
        }
        numEventsLabel.setText("" + numberOfEvents);
    }

    /**
     * Set the displayed dictionary source value.
     * @param dictionary dictionary source.
     */
    public void setDictionary(String dictionary) {
        if (dictionary != null) {
            dictionaryLabel.setText(dictionary);
        }
    }

    /**
     * Get the displayed dictionary source value.
     * @return the displayed dictionary source value.
     */
    public String getDictionary() {
        return dictionaryLabel.getText();
    }

    /**
     * Set the displayed event source value.
     * @param source event source.
     */
    public void setSource(String source) {
        if (source != null) {
            eventSourceLabel.setText(source);
        }
    }

    /**
     * Get the displayed event source value.
     * @return the displayed event source value.
     */
    public String getSource() {
        return eventSourceLabel.getText();
    }

    /**
     * Set the displayed event number value.
     * @param eventNumber event number.
     */
    public void setEventNumber(int eventNumber) {
        if (eventNumber > -1) {
            eventNumberLabel.setText("" + eventNumber);
        }
    }

    /**
     * Get the displayed event number value.
     * @return the displayed event number value.
     */
    public int getEventNumber() {
        return Integer.parseInt(eventNumberLabel.getText());
    }

    /**
     * Set the displayed number-of-events value.
     * @param numberOfEvents number of events.
     */
    public void setNumberOfEvents(int numberOfEvents) {
        if (numberOfEvents > -1) {
            numEventsLabel.setText("" + numberOfEvents);
        }
    }

    /**
     * Get the displayed number-of-events value.
     * @return the displayed number-of-events value.
     */
    public int getNumberOfEvents() {
        return Integer.parseInt(numEventsLabel.getText());
    }

}
