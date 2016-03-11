package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JMenuItem;

import org.hps.monitoring.ecal.eventdisplay.io.EventManager;

/**
 * Abstract class <code>ActiveViewer</code> describes a <code>Viewer
 * </code> object that is connected to a static data source. It is
 * designed to instruct the data source when to provide new events.
 * <br/><br/>
 * <code>ActiveViewer</code> is now superseded by <code>FileViewer
 * </code>, which functions on its own. Any additional file types that
 * should be supported should be added directly to <code>FileViewer
 * </code> or a subclass. <code>ActiveViewer</code> will be removed
 * from coming releases.
 * 
 * @author Kyle McCarty
 */
@Deprecated
public abstract class ActiveViewer extends Viewer {
    private static final long serialVersionUID = 2L;
    // Gets events from some file.
    protected EventManager em;
    
    /**
     * Creates an active-type <code>Viewer</code> window which draws
     * events from the indicated data source with additional status
     * fields defined by the <code>fieldNames</code> argument.
     * @param em - The data source event manager.
     */
    public ActiveViewer(EventManager em) {
        // Pass any additional field values to the super class.
        super();
        
        // Set the data source.
        this.em = em;
        
        // Add an exit command to the file menu.
        JMenuItem menuExit = new JMenuItem("Exit", KeyEvent.VK_X);
        menuExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        menu[MENU_FILE].addSeparator();
        menu[MENU_FILE].add(menuExit);
        
        // Make a key listener to change events.
        addKeyListener(new EcalKeyListener());
    }
    
    /**
     * Feeds the calorimeter panel the data from the next event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public abstract void displayNextEvent() throws IOException;
    
    /**
     * Feeds the calorimeter panel the data from the previous event.
     * @throws IOException Occurs when there is an issue with reading the data file.
     **/
    public abstract void displayPreviousEvent() throws IOException;
    
    /**
     * The <code>EcalListener</code> class binds keys to actions.
     * Bound actions include:
     * [Right Arrow] :: Next event
     * [Left Arrow ] :: Previous event
     **/
    private class EcalKeyListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            // If right-arrow was pressed, go to the next event.
            if (e.getKeyCode() == 39) {
                try { displayNextEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // If left-arrow was pressed, go to the next event.
            else if (e.getKeyCode() == 37) {
                try { displayPreviousEvent(); }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }
            
            // Otherwise, print out the key code for the pressed key.
            else { System.out.printf("Key Code: %d%n", e.getKeyCode()); }
        }
    }
}