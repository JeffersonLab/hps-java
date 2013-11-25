package org.jlab.coda.jevio.graphics;

import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EventListener;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;

import org.jlab.coda.jevio.*;
import org.jlab.coda.cMsg.cMsgException;

/**
 * This class creates the menus used in the GUI.
 * @author Heddle
 * @author Timmer
 */
public class EventTreeMenu {

    //----------------------
    // gui stuff
    //----------------------

	/** A menu item for selecting "next event". */
	private JMenuItem nextEventItem;

	/** A menu item for going to a specific event. */
	private JMenu gotoEventItem;

	/** Menu item for exporting file to XML. */
	private JMenuItem xmlExportItem;

    /** Menu item for opening event file. */
    private JMenuItem openEventFile;

    /** Menu item allowing configuration of cmsg and other event sources.  */
    private JMenu sourceConfig;

    /** Menu item setting the number of the event (from a file) to be displayed. */
    private JTextField eventNumberInput;

    /** The panel that holds the tree and all associated widgets. */
	private EventTreePanel eventTreePanel;

    //----------------------
    // file stuff
    //----------------------

    /**
     * Source of the evio events being displayed.
     * By default this GUI is setup to look at files.
     */
    private EventSource eventSource = EventSource.FILE;

    /** Last selected data file. */
    private String dataFilePath;

    /** Last selected dictionary file. */
    private String dictionaryFilePath;

    /** Last selected xml file to export event file into. */
    private String xmlFilePath;

    /** Filter so only files with specified extensions are seen in file viewer. */
    private FileNameExtensionFilter evioFileFilter;

    /** The reader object for the currently viewed evio file. */
    private EvioReader evioFileReader;

    //----------------------
    // dictionary stuff
    //----------------------

    /** Is the user-selected or cmsg/file-embedded dictionary currently used? */
    private boolean isUserDictionary;

    /** User-selected dictionary file. */
    private INameProvider userDictionary;

    /** Dictionary embedded with opened evio file. */
    private INameProvider fileDictionary;

    /** Dictionary embedded with cmsg message. */
    private INameProvider cmsgDictionary;

    /** Dictionary currently in use. */
    private INameProvider currentDictionary;

    //----------------------
    // cmsg stuff
    //----------------------

    /** Panel to handle input into CmsgHandler object. */
    private JPanel cmsgPanel;

    /** Object to handle cMsg communications. */
    private cMsgHandler cmsgHandler;

    /** Button to connect-to / disconnect-from cMsg server. */
    private JButton connectButton;

    /** Will pressing button connect-to or disconnect-from cMsg server? */
    private boolean buttonWillConnect = true;

    /** Thread to update cMsg queue size in GUI. */
    private UpdateThread cmsgUpdateThread;

    //----------------------------
    // General function
    //----------------------------
	/**
	 * Listener list for structures (banks, segments, tagsegments) encountered while processing an event.
	 */
	private EventListenerList evioListenerList;

    /**
     * This class is a thread which updates the number of events existing in the queue
     * and displays it every second.
     */
    private class UpdateThread extends Thread {
        // update queue size in GUI
        Runnable r = new Runnable() {
             public void run() {
                 if (cmsgHandler != null) {     //TODO: make sure all values updated properly
                     eventTreePanel.getEventInfoPanel().setNumberOfEvents(cmsgHandler.getQueueSize());
                 }
             }
        };

        // update queue size in GUI every 1 second
        public void run() {
            while (true) {
                if (isInterrupted()) { break; }
                SwingUtilities.invokeLater(r);
                try { Thread.sleep(1000); }
                catch (InterruptedException e) { break; }
            }
        }
    }



    /**
	 * Constructor. Holds the menus for a frame or internal frame that wants to manage a tree panel.
	 * @param eventTreePanel holds the tree and all associated the widgets.
	 */
	public EventTreeMenu(final EventTreePanel eventTreePanel) {
		this.eventTreePanel = eventTreePanel;
	}

    /**
     * Get the main event display panel.
     * @return main event display panel.
     */
    public EventTreePanel getEventTreePanel() {
        return eventTreePanel;
    }


    /**
     * Switch between different event sources (file, cmsg, et).
     */
    private void setEventSource(EventSource source) {

        // do nothing if same source already selected
        if (source == eventSource) {
            return;
        }

        // remember the current source
        eventSource = source;

        // clear display of any event
        eventTreePanel.setEvent(null);

        switch (source) {

            case CMSG:

                // show "cMsg config" menu item
                sourceConfig.setEnabled(true);
                sourceConfig.removeAll();
                sourceConfig.add(cmsgPanel);
                sourceConfig.setText("cMsg config");

                // turn menu items off/on
                openEventFile.setEnabled(false);
                gotoEventItem.setEnabled(true);
                nextEventItem.setEnabled(true);
                if (xmlExportItem.isEnabled()) {
                    xmlExportItem.setEnabled(false);
                }

                // update event info
                eventTreePanel.getEventInfoPanel().setDisplay("cMsg messages", 0, 0, null);

                // start thread that tells how many messages are in queue
                cmsgUpdateThread = new UpdateThread();
                cmsgUpdateThread.start();

                break;

            case FILE:

                // interrupt cmsg queue message count thread
                if (cmsgUpdateThread != null) {
                    cmsgUpdateThread.interrupt();
                }

                // disconnect from cMsg server (thru GUI button so GUI consistant with program)
                if (!buttonWillConnect) {
                    connectButton.doClick();
                }

                EvioReader evioFile = evioFileReader;

                // turn menu items off/on
                sourceConfig.setText(" ");
                sourceConfig.setEnabled(false);
                openEventFile.setEnabled(true);
                if (evioFile == null) {
                    gotoEventItem.setEnabled(false);
                    nextEventItem.setEnabled(false);
                }
                else {
                    gotoEventItem.setEnabled(true);
                    nextEventItem.setEnabled(true);
                    xmlExportItem.setEnabled(true);
                }

                // remove any cmsg dictionary used last
                cmsgDictionary = null;

                // get values for display
                String fileName = "";
                String dictSource = "     ";
                int eventCount = 0;

                if (evioFile != null) {
                    // switch data back to last file (which is still loaded)
                    fileName = dataFilePath;

                    // switch dictionary back to last one loaded from a file
                    if (userDictionary != null) {
                        currentDictionary = userDictionary;
                        NameProvider.setProvider(currentDictionary);
                        dictSource = dictionaryFilePath;
                        eventTreePanel.getEventInfoPanel().setDictionary("from file");
                        isUserDictionary = true;
                    }

                    // get event count
                    try {
                        eventCount = evioFile.getEventCount();
                    }
                    catch (EvioException e) { /* should never happen */ }
                }

                // update display
                eventTreePanel.getEventInfoPanel().setDisplay(fileName, 0, eventCount, dictSource);

                break;

            case ET:

                if (cmsgUpdateThread != null) {
                    cmsgUpdateThread.interrupt();
                }
                if (!buttonWillConnect) {
                    connectButton.doClick();
                }

                sourceConfig.setText(" ");
                sourceConfig.setEnabled(false);
                eventTreePanel.getEventInfoPanel().setDisplay("", 0, 0, null);  //TODO: remove??
                if (xmlExportItem.isEnabled()) {
                    xmlExportItem.setEnabled(false);
                }
                eventTreePanel.getEventInfoPanel().setDisplay("ET buffers", 0, 0, null);
                
                break;
            
            default:
        }
    }

	/**
	 * Create the event menu.
	 *
	 * @return the event menu.
	 */
	public JMenu createEventMenu() {
		final JMenu menu = new JMenu(" Event ");

		// next event menu item
		ActionListener al_ne = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (eventSource) {
                    // If we're looking at a file, there are multiple events contained in it
                    case FILE:
                        if (evioFileReader != null) {
                            try {
                                EvioEvent event = evioFileReader.parseNextEvent();
                                if (event != null) {
                                    eventTreePanel.setEvent(event);
                                }
                            }
                            catch (EvioException e1) {
                                e1.printStackTrace();
                            }
                        }
                        break;

                    // If we're looking at cmsg messages, there is a queue of events
                    // extracted from the messages.
                    case CMSG:
                        if (cmsgHandler != null) {
                            // Get next event (messages not containing events are ignored).
                            EvioEvent event = cmsgHandler.getNextEvent();
                            if (event != null) {
                                eventTreePanel.setEvent(event);
                                // If there's a dictionary in this event, use it and keep track.
                                if (event.hasDictionaryXML()) {
                                    cmsgDictionary = NameProviderFactory.createNameProvider(event.getDictionaryXML());
                                    currentDictionary = cmsgDictionary;
                                    NameProvider.setProvider(currentDictionary);
                                    eventTreePanel.getEventInfoPanel().setDictionary("from cMsg message");
                                    eventTreePanel.refreshDescription();
                                    isUserDictionary = false;
                                }
                            }
                        }
                        break;

                    default:

                }
            }
        };
		nextEventItem = new JMenuItem("Next Event");
		nextEventItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		nextEventItem.addActionListener(al_ne);
		nextEventItem.setEnabled(false);
		menu.add(nextEventItem);

		// goto event dialog box (show with Ctrl-G)
        // This used to be a menu item accelerator, but put it here since gotoEventItem changed
        Action gotoEventAction = new AbstractAction("gotoEvent") {
            public void actionPerformed(ActionEvent e) {
                switch (eventSource) {
                    // If we're looking at a file, go to the specified event number
                    case FILE:
                        try {
                            String prompt = "Go to Event (1.." + evioFileReader.getEventCount() + ")";
                            String inputValue = JOptionPane.showInputDialog(prompt);
                            if (inputValue != null) {
                                int eventNum = Integer.parseInt(inputValue);
                                if ((eventNum > 0) && (eventNum <= evioFileReader.getEventCount())) {
                                    EvioEvent event = evioFileReader.gotoEventNumber(eventNum);
                                    if (event != null) {
                                        getEventTreePanel().setEvent(event);
                                    }
                                }
                            }
                        }
                        catch (Exception e1) {
                        }
                        break;

                    // If we're looking at cmsg messages, there is a queue of messages.
                    // Skip over (delete) events and go to the selected place in the queue. 
                    case CMSG:
                        if (cmsgHandler != null) {
                            int qsize = cmsgHandler.getQueueSize();
                            String prompt = "Go to Event (1.." + qsize + ")";
                            String inputValue = JOptionPane.showInputDialog(prompt);
                            if (inputValue != null) {
                                int eventNum = Integer.parseInt(inputValue);
                                if ((eventNum > 0) && (eventNum <= qsize)) {
                                    // delete messages before the one we want
                                    cmsgHandler.clearQueue(eventNum-1);

                                    // Get next event (messages not containing events are discarded from queue).
                                    EvioEvent event = cmsgHandler.getNextEvent();
                                    if (event != null) {
                                        eventTreePanel.setEvent(event);
                                        // If there's a dictionary in the this message, use it and keep track.
                                        if (event.hasDictionaryXML()) {
                                            cmsgDictionary = NameProviderFactory.createNameProvider(event.getDictionaryXML());
                                            currentDictionary = cmsgDictionary;
                                            NameProvider.setProvider(currentDictionary);
                                            eventTreePanel.getEventInfoPanel().setDictionary("from cMsg message");
                                            eventTreePanel.refreshDescription();
                                            isUserDictionary = false;
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    default:
                }
            }
        };
        // (can use any JComponent to do this)
        menu.getActionMap().put(gotoEventAction.getValue(Action.NAME), gotoEventAction);
        menu.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                                                                                       ActionEvent.CTRL_MASK),
                                                                "gotoEvent");
        // goto event menu item
        ActionListener al_numIn = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String num = eventNumberInput.getText();
                    if (num != null) {
                        int eventNum = Integer.parseInt(num);

                        switch (eventSource) {
                            // If we're looking at a file, go to the specified event number
                            case FILE:
                                if ((eventNum > 0) && (eventNum <= evioFileReader.getEventCount())) {
                                    EvioEvent event = evioFileReader.gotoEventNumber(eventNum);
                                    if (event != null) {
                                        eventTreePanel.setEvent(event);
                                    }
                                }
                                else {
                                    eventNumberInput.setText("");
                                }

                                break;

                            // If we're looking at cmsg messages, there is a queue of messages.
                            // Skip over (delete) events and go to the selected place in the queue.
                            case CMSG:
                                if (cmsgHandler != null) {
                                    int qsize = cmsgHandler.getQueueSize();
                                    if ((eventNum > 0) && (eventNum <= qsize)) {
                                        // delete messages before the one we want
                                        cmsgHandler.clearQueue(eventNum-1);

                                        // Get next event (messages not containing events are discarded from queue).
                                        EvioEvent event = cmsgHandler.getNextEvent();
                                        if (event != null) {
                                            eventTreePanel.setEvent(event);
                                            // If there's a dictionary in the this message, use it and keep track.
                                            if (event.hasDictionaryXML()) {
                                                cmsgDictionary = NameProviderFactory.createNameProvider(event.getDictionaryXML());
                                                currentDictionary = cmsgDictionary;
                                                NameProvider.setProvider(currentDictionary);
                                                eventTreePanel.getEventInfoPanel().setDictionary("from cMsg message");
                                                eventTreePanel.refreshDescription();
                                                isUserDictionary = false;
                                            }
                                        }
                                    }
                                }

                                break;

                            default:
                        }
                    }
                }
                catch (NumberFormatException e1) {
                    // bad entry in widget
                    eventNumberInput.setText("");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
        gotoEventItem = new JMenu("Go to Event ...            ");
        eventNumberInput = new JTextField();
        eventNumberInput.setPreferredSize(new Dimension(100,20));
        eventNumberInput.addActionListener(al_numIn);
        gotoEventItem.add(eventNumberInput);
		gotoEventItem.setEnabled(false);
		menu.add(gotoEventItem);

        menu.addSeparator();

        // Select between different evio event sources
        JLabel jl = new JLabel("Event Sources");
        jl.setBorder(new EmptyBorder(3,20,3,0));
        jl.setHorizontalTextPosition(JLabel.CENTER);
        menu.add(jl);

        JRadioButtonMenuItem fileItem = new JRadioButtonMenuItem("File");
        JRadioButtonMenuItem cmsgItem = new JRadioButtonMenuItem("cMsg");
        JRadioButtonMenuItem   etItem = new JRadioButtonMenuItem("ET (not available)");
        EmptyBorder eBorder = new EmptyBorder(3,30,3,0);
        fileItem.setBorder(eBorder);
        cmsgItem.setBorder(eBorder);
        etItem.setBorder(eBorder);

        // action listener for selecting cmsg source
        ActionListener cmsgListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cmsgPanel == null) {
                    cmsgPanel = createCmsgPanel();
                }
                setEventSource(EventSource.CMSG);
                // keep this menu up (displayed) so user can go to config item
                menu.doClick();
            }
        };
        cmsgItem.addActionListener(cmsgListener);

        // action listener for selecting file source
        ActionListener fileListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEventSource(EventSource.FILE);
                menu.doClick();
            }
        };
        fileItem.addActionListener(fileListener);

        // action listener for selecting ET source
        ActionListener etListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEventSource(EventSource.ET);
                menu.doClick();
             }
        };
        etItem.addActionListener(etListener);
        // etItem.setEnabled(false); // turn off ET source for now

        ButtonGroup group = new ButtonGroup();
        group.add(fileItem);
        group.add(cmsgItem);
        group.add(  etItem);
        // file source selected by default
        group.setSelected(fileItem.getModel(), true);

        menu.add(fileItem);
        menu.add(cmsgItem);
        menu.add(  etItem);

        menu.addSeparator();

        sourceConfig = new JMenu("");
        sourceConfig.setText(" ");
        sourceConfig.setEnabled(false);
        menu.add(sourceConfig);

		return menu;
	}

    /**
     * Create the view menu.
     *
     * @return the view menu.
     */
    public JMenu createViewMenu() {
        final JMenu menu = new JMenu(" View ");

        // ints-viewed-as-hex menu item
        ActionListener al_hex = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem item = (JMenuItem) e.getSource();
                String txt = item.getText();
                if (txt.equals("Hexidecimal")) {
                    eventTreePanel.setIntsInHex(true);
                    item.setText("Decimal");
                }
                else {
                    eventTreePanel.setIntsInHex(false);
                    item.setText("Hexidecimal");
                }
                eventTreePanel.refreshDisplay();
            }
        };

        JMenuItem hexItem = new JMenuItem("Hexidecimal");
        hexItem.addActionListener(al_hex);
        hexItem.setEnabled(true);
        menu.add(hexItem);


        // switch dictionary menu item
        ActionListener al_switchDict = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (switchDictionary()) {
                    eventTreePanel.refreshDisplay();
                }
            }
        };

        JMenuItem switchDictItem = new JMenuItem("Switch dictionary");
        switchDictItem.addActionListener(al_switchDict);
        switchDictItem.setEnabled(true);
        menu.add(switchDictItem);

        return menu;
    }


	/**
	 * Create the file menu.
	 *
	 * @return the file menu.
	 */
	public JMenu createFileMenu() {
		JMenu menu = new JMenu(" File ");

		// open event file menu item
		ActionListener al_oef = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenEventFile();
			}
		};
		openEventFile = new JMenuItem("Open Event File...");
		openEventFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		openEventFile.addActionListener(al_oef);
		menu.add(openEventFile);

		// open dictionary menu item
		ActionListener al_odf = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenDictionary();
			}
		};
		JMenuItem df_item = new JMenuItem("Open Dictionary...");
		df_item.addActionListener(al_odf);
		menu.add(df_item);

        // separator
		menu.addSeparator();

		// open dictionary menu item
		ActionListener al_xml = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportToXML();
			}
		};
		xmlExportItem = new JMenuItem("Export File to XML...");
		xmlExportItem.addActionListener(al_xml);
		xmlExportItem.setEnabled(false);
		menu.add(xmlExportItem);
		menu.addSeparator();

		// Quit menu item
		ActionListener al_exit = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		JMenuItem exit_item = new JMenuItem("Quit");
		exit_item.addActionListener(al_exit);
		menu.add(exit_item);

		return menu;
	}


    /**
     * Select and open an event file.
     */
    private void doOpenEventFile() {
        EvioReader eFile    = evioFileReader;
        EvioReader evioFile = openEventFile();
        // handle cancel button properly
        if (eFile == evioFile) {
            return;
        }
        nextEventItem.setEnabled(evioFile != null);
        gotoEventItem.setEnabled(evioFile != null);
        xmlExportItem.setEnabled(evioFile != null);
        eventTreePanel.setEvent(null);
        try {
            if (evioFile != null) {
                gotoEventItem.setText("Go to Event (1.." + evioFileReader.getEventCount() + ")");
            }
        }
        catch (EvioException e) {}
    }

    /**
     * Convenience method to open a file programmatically.
     * @param file the file to open
     */
    public void manualOpenEventFile(File file) {
        EvioReader eFile    = evioFileReader;
        EvioReader evioFile = openEventFile(file);
        // handle cancel button properly
        if (eFile == evioFile) {
            return;
        }
        nextEventItem.setEnabled(evioFile != null);
        gotoEventItem.setEnabled(evioFile != null);
        xmlExportItem.setEnabled(evioFile != null);
        eventTreePanel.setEvent(null);
        try {
            if (evioFile != null) {
                gotoEventItem.setText("Go to Event (1.." + evioFileReader.getEventCount() + ")");
            }
        }
        catch (EvioException e) {}
    }

	/**
	 * Select and open a dictionary.
	 */
	private void doOpenDictionary() {
        openDictionary();
	}


    /**
     * Create the panel/menuitem used to handle communications with a cmsg server.
     * @return the panel/menuitem used to handle communications with a cmsg server.
     */
    public JPanel createCmsgPanel() {
        // custom colors
        final Color darkGreen = new Color(0, 160,0);
        final Color darkRed   = new Color(160, 0, 0);

        // put in a default UDL for connection to cmsg server
        final JTextField UDL = new JTextField("cMsg://localhost/cMsg/myNameSpace");
        // put in a default subscription subject
        final JTextField Subject = new JTextField("evio");
        // put in a default subscription type (* means everything)
        final JTextField Type = new JTextField("*");

        UDL.setEditable(true);
        UDL.setMargin(new Insets(2, 5, 2, 5));

        // update subscription when hit enter
        ActionListener al_sub = new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 try {
                     cmsgHandler.subscribe(Subject.getText(), Type.getText());
                 }
                 catch (cMsgException e1) {
                     e1.printStackTrace();
                     Subject.setText("evio");
                 }
             }
        };
        Subject.addActionListener(al_sub);

        // update subscription when move mouse out of widget
        MouseListener ml_sub = new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                try {
                    if (cmsgHandler != null) cmsgHandler.subscribe(Subject.getText(), Type.getText());
                }
                catch (cMsgException e1) {
                    e1.printStackTrace();
                    Subject.setText("evio");
                }
            }
        };
        Subject.addMouseListener(ml_sub);
        Subject.setEditable(true);
        Subject.setMargin(new Insets(2, 5, 2, 5));

        // update subscription when hit enter
        ActionListener al_typ = new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  try {
                      cmsgHandler.subscribe(Subject.getText(), Type.getText());
                  }
                  catch (cMsgException e1) {
                      e1.printStackTrace();
                      Subject.setText("*");
                  }
              }
         };
        Type.addActionListener(al_typ);

        // update subscription when move mouse out of widget
        MouseListener ml_typ = new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                try {
                    if (cmsgHandler != null) cmsgHandler.subscribe(Subject.getText(), Type.getText());
                }
                catch (cMsgException e1) {
                    e1.printStackTrace();
                    Subject.setText("*");
                }
            }
        };
        Type.addMouseListener(ml_typ);
        Type.setEditable(true);
        Type.setMargin(new Insets(2, 5, 2, 5));

        final JLabel status = new JLabel("  Press button to connect to cMsg server  ");
        status.setVerticalTextPosition(SwingConstants.CENTER);
        status.setBorder(new LineBorder(Color.black));

        // button panel
        final JPanel p1 = new JPanel();

        // connect/disconnect button
        ActionListener al_con = new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 // get singleton object for cmsg communication
                 JButton button = (JButton) e.getSource();

                 if (button.getText().equals("Connect")) {
                     if (cmsgHandler == null) {
                         cmsgHandler = cMsgHandler.getInstance();
                     }

                     // connect to cmsg server, and subscribe to receive messages
                     try {
                         cmsgHandler.connect(UDL.getText());
                         cmsgHandler.subscribe(Subject.getText(), Type.getText());
                     }
                     catch (cMsgException e1) {
                         // handle failure
                         status.setForeground(Color.red);
                         status.setText(" Failed to connect to cmsg server");
                         return;
                     }

                     // success connecting to cmsg server
                     UDL.setEnabled(false);
                     status.setForeground(darkGreen);
                     status.setText(" Connected to cmsg server");
                     connectButton.setText("Disconnect");
                     buttonWillConnect = false;
                 }
                 else {
                     // disconnect from cmsg server
                     cmsgHandler.disconnect();
                     // reset sources
                     UDL.setEnabled(true);
                     status.setForeground(darkRed);
                     status.setText(" Disconnected from cmsg server");
                     connectButton.setText("Connect");
                     buttonWillConnect = true;
                 }
             }
        };
        connectButton = new JButton("Connect");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(al_con);
        connectButton.setEnabled(true);

        // clear queue button
        ActionListener al_cq = new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 cmsgHandler.clearQueue();
             }
        };
        final JButton clearQButton = new JButton("Clear Message Queue");
        clearQButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearQButton.addActionListener(al_cq);

        p1.setBorder(new EmptyBorder(2, 5, 2, 5));
        p1.setLayout(new GridLayout(0, 2));
        p1.add(connectButton);
        p1.add(clearQButton);

        // label panel
        JPanel p3 = new JPanel();
        p3.setLayout(new GridLayout(4, 0));
        JLabel label1 = new JLabel("UDL ");
        label1.setHorizontalAlignment(SwingConstants.RIGHT);
        p3.add(label1);
        JLabel label2 = new JLabel("Subject ");
        label2.setHorizontalAlignment(SwingConstants.RIGHT);
        p3.add(label2);
        JLabel label3 = new JLabel("Type ");
        label3.setHorizontalAlignment(SwingConstants.RIGHT);
        p3.add(label3);
        JLabel label4 = new JLabel("Status ");
        label4.setHorizontalAlignment(SwingConstants.RIGHT);
        p3.add(label4);

        // textfield panel
        JPanel p4 = new JPanel();
        p4.setLayout(new GridLayout(4, 0));
        p4.add(UDL);
        p4.add(Subject);
        p4.add(Type);
        p4.add(status);

        // keep left hand labels from growing & shrinking in X-axis
        Dimension d = p3.getPreferredSize();
        d.height    = p4.getPreferredSize().height;
        p3.setMaximumSize(d);

        // panel containing label & textfield panels
        JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
        p2.add(Box.createRigidArea(new Dimension(5,0)));
        p2.add(p3);
        p2.add(p4);
        p2.add(Box.createRigidArea(new Dimension(3,0)));

        // top-level panel
        JPanel ptop = new JPanel();
        ptop.setLayout(new BoxLayout(ptop, BoxLayout.Y_AXIS));
        ptop.add(Box.createRigidArea(new Dimension(0,3)));
        ptop.add(p2);
        ptop.add(Box.createRigidArea(new Dimension(0,5)));
        ptop.add(p1);
        ptop.add(Box.createRigidArea(new Dimension(0,5)));

        return ptop;
    }





//    public boolean isUserDictionary() {
//         return isUserDictionary;
//     }
//
//     public void setUserDictionary(boolean userDictionary) {
//         isUserDictionary = userDictionary;
//     }
//
//     public INameProvider getUserDictionary() {
//         return userDictionary;
//     }
//
//
//     public INameProvider getFileDictionary() {
//         return fileDictionary;
//     }

    /**
     * Attempt to switch dictionaries. If using dictionary from file,
     * switch to user-selected dictionary if it exists, or vice versa.
     *
     * @return <code>true</code> if dictionary was switched, else <code>false</code>
     */
    public boolean switchDictionary() {
        // if switching from user-selected dictionary file to embedded file ..
        if (isUserDictionary) {
            switch (eventSource) {
                case FILE:
                    if (fileDictionary != null) {
                        currentDictionary = fileDictionary;
                        NameProvider.setProvider(currentDictionary);
                        eventTreePanel.refreshDescription();
                        eventTreePanel.getEventInfoPanel().setDictionary("from file");
                        isUserDictionary = false;
                        return true;
                    }
                    break;
                case CMSG:
                    if (cmsgDictionary != null) {
                        currentDictionary = cmsgDictionary;
                        NameProvider.setProvider(currentDictionary);
                        eventTreePanel.refreshDescription();
                        eventTreePanel.getEventInfoPanel().setDictionary("from cMsg message");
                        isUserDictionary = false;
                        return true;
                    }
            }
        }
        // if switching from embedded file to user-selected dictionary file ..
        else {
            if (userDictionary != null) {
                currentDictionary = userDictionary;
                NameProvider.setProvider(currentDictionary);
                eventTreePanel.refreshDescription();
                eventTreePanel.getEventInfoPanel().setDictionary(dictionaryFilePath);
                isUserDictionary = true;
                return true;
            }
        }
        return false;
    }

//    /**
//     * Get the filepath of file whose data is currently loaded.
//     * @return the filepath of file whose data is currently loaded.
//     */
//    public String getDataFilePath() {
//        return dataFilePath;
//    }

//    /**
//     * Get the filepath of dictionary file whose data is currently loaded.
//     * @return the filepath of dictionary file whose data is currently loaded.
//     */
//    public String getDictionaryFilePath() {
//        return dictionaryFilePath;
//    }


    /**
     * Select a file and then write into it the current event file in xml format.
     */
    public void exportToXML() {
        eventTreePanel.getProgressBar().setValue(0);
        JFileChooser chooser = new JFileChooser(xmlFilePath);
        chooser.setSelectedFile(null);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Evio Files", "xml");
        chooser.setFileFilter(filter);

        int returnVal = chooser.showSaveDialog(eventTreePanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            // remember which file was chosen
            File selectedFile = chooser.getSelectedFile();
            xmlFilePath = selectedFile.getAbsolutePath();

            if (selectedFile.exists()) {
                int answer = JOptionPane.showConfirmDialog(null, selectedFile.getPath()
                        + "  already exists. Do you want to overwrite it?", "Overwite Existing File?",
                        JOptionPane.YES_NO_OPTION);

                if (answer != JFileChooser.APPROVE_OPTION) {
                    return;
                }
            }

            // keep track of progress writing file
            final IEvioProgressListener progressListener = new IEvioProgressListener() {
                @Override
                public void completed(int num, int total) {
                    int percentDone = (int) ((100.0 * num) / total);
                    eventTreePanel.getProgressBar().setValue(percentDone);
                    eventTreePanel.getProgressBar().repaint();
                }
            };


            // do the xml processing in a separate thread.
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    evioFileReader.toXMLFile(xmlFilePath, progressListener);
                    eventTreePanel.getProgressBar().setValue(0);
                    JOptionPane.showMessageDialog(eventTreePanel, "XML Writing has completed.", "Done",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            };
            (new Thread(runner)).start();

        }
    }


    /**
     * Add a file extension for viewing evio format files in file chooser.
     * @param extension file extension to add
     */
    public void addEventFileExtension(String extension) {
        if (evioFileFilter == null) {
            evioFileFilter = new FileNameExtensionFilter("EVIO Event Files", "ev", "evt", "evio", extension);
        }
        else {
            String[] exts = evioFileFilter.getExtensions();
            String[] newExts = Arrays.copyOf(exts, exts.length + 1);
            newExts[exts.length] = extension;
            evioFileFilter = new FileNameExtensionFilter("EVIO Event Files", newExts);
        }
    }


    /**
     * Set all file extensions for viewing evio format files in file chooser.
     * @param extensions all file extensions
     */
    public void setEventFileExtensions(String[] extensions) {
        evioFileFilter = new FileNameExtensionFilter("EVIO Event Files", extensions);
    }


    /**
     * Select and open an event file.
     *
     * @return the opened file reader, or <code>null</code>
     */
    public EvioReader openEventFile() {
        eventTreePanel.getEventInfoPanel().setEventNumber(0);

        JFileChooser chooser = new JFileChooser(dataFilePath);
        chooser.setSelectedFile(null);
        if (evioFileFilter == null) {
            evioFileFilter = new FileNameExtensionFilter("EVIO Event Files", "ev", "evt", "evio");
        }
        chooser.setFileFilter(evioFileFilter);
        int returnVal = chooser.showOpenDialog(eventTreePanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            eventTreePanel.getHeaderPanel().setHeader(null);

            // remember which file was chosen
            File selectedFile = chooser.getSelectedFile();
            dataFilePath = selectedFile.getAbsolutePath();

            // set the text field
            eventTreePanel.getEventInfoPanel().setSource(dataFilePath);

            if (evioFileReader != null) {
                evioFileReader.close();
                eventTreePanel.getEventInfoPanel().setNumberOfEvents(0);
            }
            try {
                evioFileReader = new EvioReader(selectedFile);
                eventTreePanel.getEventInfoPanel().setNumberOfEvents(evioFileReader.getEventCount());
                if (evioFileReader.hasDictionaryXML()) {
                    fileDictionary = NameProviderFactory.createNameProvider(evioFileReader.getDictionaryXML());
                    currentDictionary = fileDictionary;
                    NameProvider.setProvider(currentDictionary);
                    isUserDictionary = false;
                    eventTreePanel.getEventInfoPanel().setDictionary("from file");
                }
            }
            catch (EvioException e) {
                evioFileReader = null;
                e.printStackTrace();
            }
            catch (IOException e) {
                evioFileReader = null;
                e.printStackTrace();
            }
        }
        connectEvioListeners();     // Connect Listeners to the parser.
        
        return evioFileReader;
    }


    /**
     * Open an event file using a given file.
     *
     * @param file the file to use, i.e., an event file
     * @return the opened file reader, or <code>null</code>
     */
    public EvioReader openEventFile(File file) {
        eventTreePanel.getEventInfoPanel().setEventNumber(0);

        eventTreePanel.getHeaderPanel().setHeader(null);

        // remember which file was chosen
        dataFilePath = file.getAbsolutePath();

        // set the text field
        eventTreePanel.getEventInfoPanel().setSource(dataFilePath);

        if (evioFileReader != null) {
            evioFileReader.close();
            eventTreePanel.getEventInfoPanel().setNumberOfEvents(0);
        }
        try {
            evioFileReader = new EvioReader(file);
            eventTreePanel.getEventInfoPanel().setNumberOfEvents(evioFileReader.getEventCount());
            if (evioFileReader.hasDictionaryXML()) {
                fileDictionary = NameProviderFactory.createNameProvider(evioFileReader.getDictionaryXML());
                currentDictionary = fileDictionary;
                NameProvider.setProvider(currentDictionary);
                isUserDictionary = false;
                eventTreePanel.getEventInfoPanel().setDictionary("from file");
            }
        }
        catch (EvioException e) {
            evioFileReader = null;
            e.printStackTrace();
        }
        catch (IOException e) {
            evioFileReader = null;
            e.printStackTrace();
        }
        connectEvioListeners();     // Connect Listeners to the parser.
        return evioFileReader;
    }


    /**
     * Get the EvioReader object so the file/buffer can be read.
     * @return  EvioReader object
     */
    public EvioReader getEvioFileReader() {
        return evioFileReader;
    }


    /**
     * Set the default directory in which to look for event files.
     * @param defaultDataDir default directory in which to look for event files
     */
    public void setDefaultDataDir(String defaultDataDir) {
        dataFilePath = defaultDataDir;
    }


    /**
     * Select and open a dictionary.
     * @return <code>true</code> if file was opened and dictionary loaded.
     */
    public boolean openDictionary() {
        JFileChooser chooser = new JFileChooser(dictionaryFilePath);
         chooser.setSelectedFile(null);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Dictionary Files", "xml", "dict", "txt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(eventTreePanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            // create a dictionary, set the global name provider
            userDictionary = NameProviderFactory.createNameProvider(selectedFile);
            currentDictionary = userDictionary;
            NameProvider.setProvider(currentDictionary);
            isUserDictionary = true;
            // remember which file was chosen
            dictionaryFilePath = selectedFile.getAbsolutePath();
            // set the text field
            eventTreePanel.getEventInfoPanel().setDictionary(dictionaryFilePath);
            eventTreePanel.refreshDescription();
            return true;
        }
        return false;
    }


    /**
     * Select and open a dictionary.
     * @param file file to open
     */
    public void openDictionaryFile(File file) {
        if (file != null) {
            userDictionary = NameProviderFactory.createNameProvider(file);
            currentDictionary = userDictionary;
            NameProvider.setProvider(currentDictionary);
            isUserDictionary = true;
            // remember which file was chosen
            dictionaryFilePath = file.getAbsolutePath();
            // set the text field
            eventTreePanel.getEventInfoPanel().setDictionary(dictionaryFilePath);
            eventTreePanel.refreshDescription();
        }
    }

	/**
	 * Add an Evio listener. Evio listeners listen for structures encountered when an event is being parsed.
	 * The listeners are passed to the EventParser once a file is opened.
	 * @param listener The Evio listener to add.
	 */
	public void addEvioListener(IEvioListener listener) {

		if (listener == null) {
			return;
		}

		if (evioListenerList == null) {
			evioListenerList = new EventListenerList();
		}

		evioListenerList.add(IEvioListener.class, listener);
	}
	/**
	 * Connect the listeners in the evioListenerList to the EventParser
	 */
	private void connectEvioListeners(){
		
		if (evioListenerList == null) {
			return;
		}

		EventParser parser = getEvioFileReader().getParser();
		
		EventListener listeners[] = evioListenerList.getListeners(IEvioListener.class);

		for (int i = 0; i < listeners.length; i++) {
			parser.addEvioListener((IEvioListener)listeners[i]);
		}		
	}

}
