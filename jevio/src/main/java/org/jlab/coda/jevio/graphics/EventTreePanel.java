package org.jlab.coda.jevio.graphics;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import org.jlab.coda.jevio.*;

/**
 * This is a simple GUI that displays an evio event in a tree.
 * It allows the user to open event files and dictionaries,
 * and go event-by-event showing the event in a JTree.
 * @author Heddle
 */
@SuppressWarnings("serial")
public class EventTreePanel extends JPanel implements TreeSelectionListener {


    /**
     * Panel for displaying header information.
     */
    private HeaderPanel headerPanel = new HeaderPanel();

    /**
     * Panel for displaying event information.
     */
    private EventInfoPanel eventInfoPanel = new EventInfoPanel();

	/**
	 * The actual graphical tree object.
	 */
	private JTree tree;

    /**
	 * The current event.
	 */
	private EvioEvent event;

	/**
	 * Text area shows data values for selected nodes.
	 */
	private JTextArea textArea;

    /**
     * A shared progress bar.
     */
    private JProgressBar progressBar;

    /**
     * View ints in hexadecimal or decimal?
     */
    private boolean intsInHex;



    /**
	 * Constructor for a simple tree viewer for evio files.
	 */
	public EventTreePanel() {
		setLayout(new BorderLayout());
		// add all the components
		addComponents();
	}

    /**
     * Set wether integer data is displayed in hexidecimal or decimal.
     * @param intsInHex if <code>true</code> then display as hex, else deciaml
     */
    public void setIntsInHex(boolean intsInHex) {
        this.intsInHex = intsInHex;
    }


    /**
     * Get the panel displaying the event information.
     * @return
     */
    public EventInfoPanel getEventInfoPanel() {
        return eventInfoPanel;
    }

    /**
     * Get the panel displaying header information.
     * @return
     */
    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    /**
     * Get the progress bar.
     * @return the progress bar.
     */
    public JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Refresh textArea display.
     */
    public void refreshDisplay() {
        valueChanged(null);
    }

    /**
     * Refresh description (dictionary) display.
     */
    public void refreshDescription() {
        headerPanel.setDescription(event);
    }

    /**
     * Add the components to this panel.
	 */
	protected void addComponents() {
		add(eventInfoPanel, BorderLayout.NORTH);
		add(createTree(), BorderLayout.CENTER);
        add(createTextArea(), BorderLayout.WEST);

        // define south
		progressBar = new JProgressBar();
		progressBar.setBorder(BorderFactory.createTitledBorder(null, "progress", TitledBorder.LEADING,
				              TitledBorder.TOP, null, Color.blue));
        progressBar.setPreferredSize(new Dimension(200, 20));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(headerPanel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.WEST);

        add(panel, BorderLayout.SOUTH);
	}

	/**
	 * Create the tree that will display the event. What is actually
     * returned is the scroll pane that contains the tree.
	 *
	 * @return the scroll pane holding the event tree.
	 */
	private JScrollPane createTree() {
		tree = new JTree();
		tree.setModel(null);

		tree.setBorder(BorderFactory.createTitledBorder(null, "Tree representation of the EVIO event",
				TitledBorder.LEADING, TitledBorder.TOP, null, Color.blue));

		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setShowsRootHandles(true);
		tree.setEditable(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().setView(tree);
		return scrollPane;
	}

	/**
	 * Create the text area that will display structure data. What is actually
     * returned is the scroll pane that contains the text area.
	 *
	 * @return the scroll pane holding the text area.
	 */
	private JScrollPane createTextArea() {

		textArea = new JTextArea();
		textArea.setBorder(BorderFactory.createTitledBorder(null, "Array Data",
                                                            TitledBorder.LEADING,
                                                            TitledBorder.TOP,
                                                            null, Color.blue));
        textArea.setEditable(false);
        
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().setView(textArea);
        // Borderlayout respects preferred width in east/west,
        // but ignores height -- so use this to set width only.
        // Don't use "setPreferredSize" on textArea or it messes up the scrolling.
        scrollPane.setPreferredSize(new Dimension(200,1000));

		return scrollPane;
	}


	/**
	 * Selection event on our tree.
	 *
	 * @param treeSelectionEvent the causal event.
	 */
	@Override
	public void valueChanged(TreeSelectionEvent treeSelectionEvent) {

		BaseStructure structure = (BaseStructure) tree.getLastSelectedPathComponent();
		textArea.setText("");
        int blankLineEveryNth = 5; // put in a blank line after every Nth element listed

		if (structure == null) {
			return;
		}
		headerPanel.setHeader(structure);

		if (structure.isLeaf()) {

            int lineCounter=1, index=1;
			BaseStructureHeader header = structure.getHeader();

			switch (header.getDataType()) {
			case DOUBLE64:
				double doubledata[] = structure.getDoubleData();
				if (doubledata != null) {
					for (double d : doubledata) {
						String s = String.format("[%02d]  %15.11e", index++, d);
						textArea.append(s);
                        if (lineCounter < doubledata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
					}
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case FLOAT32:
				float floatdata[] = structure.getFloatData();
				if (floatdata != null) {
					for (float d : floatdata) {
						String s = String.format("[%02d]  %10.6e", index++, d);
						textArea.append(s);
                        if (lineCounter < floatdata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
					}
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case LONG64:
			case ULONG64:
				long longdata[] = structure.getLongData();
				if (longdata != null) {
					for (long i : longdata) {
						String s;
                        if (intsInHex) {
                            s = String.format("[%02d]  %#018X", index++, i);
                        }
                        else {
                            s = String.format("[%02d]  %d", index++, i);
                        }
						textArea.append(s);
                        if (lineCounter < longdata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
					}
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case INT32:
			case UINT32:
				int intdata[] = structure.getIntData();
				if (intdata != null) {
					for (int i : intdata) {
						String s;
                        if (intsInHex) {
                            s = String.format("[%02d]  %#010X", index++, i);
                        }
                        else {
                            s = String.format("[%02d]  %d", index++, i);
                        }
                        textArea.append(s);
                        if (lineCounter < intdata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
                    }
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case SHORT16:
			case USHORT16:
				short shortdata[] = structure.getShortData();
				if (shortdata != null) {
					for (short i : shortdata) {
						String s;
                        if (intsInHex) {
                            s = String.format("[%02d]  %#06X", index++, i);
                        }
                        else {
                            s = String.format("[%02d]  %d", index++, i);
                        }
						textArea.append(s);
                        if (lineCounter < shortdata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
					}
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case CHAR8:
			case UCHAR8:
				byte bytedata[] = structure.getByteData();
				if (bytedata != null) {
					for (byte i : bytedata) {
						String s = String.format("[%02d]  %d", index++, i);
						textArea.append(s);
                        if (lineCounter < bytedata.length) {
                            if (lineCounter % blankLineEveryNth == 0) {
                                textArea.append("\n\n");
                            }
                            else {
                                textArea.append("\n");
                            }
                            lineCounter++;
                        }
					}
				}
				else {
					textArea.append("null data\n");
				}
				break;

			case CHARSTAR8:
                String stringdata[] = structure.getStringData();
                for (String str : stringdata) {
                    String s = String.format("[%02d]  %s\n", index++, str);
                    textArea.append(s != null ? s : "null data\n");
                }
				break;

            case COMPOSITE:
                try {
                    CompositeData[] cData = structure.getCompositeData();
                    if (cData != null) {
                        for (int i=0; i < cData.length; i++) {
                            textArea.append("composite data object ");
                            textArea.append(i + ":\n");
                            textArea.append(cData[i].toString(intsInHex));
                            textArea.append("\n\n");
                        }
                    }
                    else {
                        textArea.append("null data\n");
                    }
                }
                catch (EvioException e) {
                    // internal format error
                }
                break;

            }

		}
		tree.repaint();
	}



	/**
	 * Get the currently displayed event.
	 *
	 * @return the currently displayed event.
	 */
	public EvioEvent getEvent() {
		return event;
	}

	/**
	 * Set the currently displayed event.
	 *
	 * @param event the currently displayed event.
	 */
	public void setEvent(EvioEvent event) {
		this.event = event;
		if (event != null) {
			tree.setModel(event.getTreeModel());
			headerPanel.setHeader(event);
			eventInfoPanel.setEventNumber(event.getEventNumber());
		    expandAll();
        }
		else {
			tree.setModel(null);
			headerPanel.setHeader(null);
            eventInfoPanel.setEventNumber(0);
		}
	}


    /**
     * Expand all nodes.
     */
    public void expandAll() {
        if (tree != null) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
        }
    }


}