package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.hps.monitoring.ecal.eventdisplay.util.CrystalEvent;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalListener;
import org.hps.monitoring.ecal.eventdisplay.util.SettingsEvent;
import org.hps.monitoring.ecal.eventdisplay.util.SettingsListener;

/**
 * The abstract class <code>Viewer</code> handles initialization of the
 * calorimeter panel with the proper settings and provides a window for
 * it to live in. Subclasses of <code>Viewer</code> should implement a
 * means for events to be fed to the calorimeter display.
 */
public abstract class Viewer extends JFrame {
    // Serialization UID.
    private static final long serialVersionUID = 2L;
    
    // A map of field names to field indices.
    private Map<String, Integer> fieldMap = new HashMap<String, Integer>();
    
    // A list of crystal listeners attached to the viewer.
    private ArrayList<CrystalListener> listenerList = new ArrayList<CrystalListener>();
    
    // Menus and menu items.
    private final JMenuItem menuScreenshot;
    private final JCheckBoxMenuItem menuHighlight;
    private final JCheckBoxMenuItem menuBackground;
    private final JCheckBoxMenuItem menuMirrorX;
    private final JCheckBoxMenuItem menuMirrorY;
    private final JRadioButtonMenuItem[] menuScaling;
    
    // The default field names.
    private static final String[] defaultFields = { "x Index", "y Index", "Energy" };
    
    // The default crystal color.
    private static final Color DEFAULT_CRYSTAL_COLOR = Color.GRAY;
    
    // Indices for the field values.
    private static final int X_INDEX = 0;
    private static final int Y_INDEX = 1;
    private static final int CELL_VALUE = 2;
    private static final int MENU_ITEM_LIN_SCALE = 0;
    private static final int MENU_ITEM_LOG_SCALE = 1;
    protected static final int MENU_FILE = 0;
    protected static final int MENU_VIEW = 1;
    protected static final int MENU_SCALE = 2;
    
    /**
     * The root menu bar displayed by the <code>Viewer</code>.
     */
    protected final JMenuBar menuRoot;
    
    /**
     * The base menus used displayed by the <code>Viewer</code>. 
     */
    protected final JMenu[] menu;
    
    /**
     * Component that allows for scrolling functionality when there
     * are more status panel entries then can be displayed at once.
     */
    protected final JScrollPane statusScroller;
    
    /**
     * The component responsible for displaying status information 
     * about the currently selected crystal.
     */
    protected final ResizableFieldPanel statusPanel;
    
    /**
     * The panel displaying the calorimeter crystals and scale.
     */
    protected final CalorimeterPanel ecalPanel = new CalorimeterPanel(46, 11);
    
    /**
     * The default color for highlighting cluster components.
     */
    public static final Color HIGHLIGHT_CLUSTER_COMPONENT = Color.RED;
    
    /**
     * The default color for highlighting cluster shared hits.
     */
    public static final Color HIGHLIGHT_CLUSTER_SHARED = Color.YELLOW;
    
    /**
     * The default color for generic crystal highlighting.
     */
    public static final Color HIGHLIGHT_GENERIC = Color.WHITE;
    
    /**
     * Initializes the viewer window and calorimeter panel.
     * @throws NullPointerException Occurs if any of the additional field
     * arguments are <code>null</code>.
     **/
    public Viewer() throws NullPointerException {
        // ==========================================================
        // ==== Initialize Base Component Properties ================
        // ==========================================================
        
        // Initialize the underlying JPanel.
        super();
        
        // Generate the status panel.
        statusPanel = new ResizableFieldPanel(100);
        statusPanel.setBackground(Color.WHITE);
        statusScroller = new JScrollPane(statusPanel);
        statusScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        statusScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        statusScroller.getVerticalScrollBar().setBlockIncrement(5);
        statusScroller.getVerticalScrollBar().setUnitIncrement(5);
        
        // Add the default fields.
        for(String field : defaultFields) { addStatusField(field); }
        
        // Set the scaling settings.
        ecalPanel.setScaleMinimum(0.001);
        ecalPanel.setScaleMaximum(3.0);
        ecalPanel.setScalingLinear();
        ecalPanel.addSettingsListener(new PropertyUpdater());
        
        // Disable the crystals in the calorimeter panel along the beam gap.
        for (int i = -23; i < 24; i++) {
            ecalPanel.setCrystalEnabled(toPanelX(i), 5, false);
            if (i > -11 && i < -1) {
                ecalPanel.setCrystalEnabled(toPanelX(i), 4, false);
                ecalPanel.setCrystalEnabled(toPanelX(i), 6, false);
            }
        }
        
        // Make a mouse motion listener to monitor mouse hovering.
        getContentPane().addMouseListener(new EcalMouseListener());
        getContentPane().addMouseMotionListener(new EcalMouseMotionListener());
        
        // Add the panels.
        add(ecalPanel);
        add(statusScroller);
        
        // Define viewer panel properties.
        setTitle("HPS Ecal Event Display");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1060, 600));
        setMinimumSize(new Dimension(1060, 525));
        setLayout(null);
        
        // Add a listener to update everything when the window changes size
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { resize(); }
        });

        // ==========================================================
        // ==== Initialize Menu Properties ==========================
        // ==========================================================
        
        // Create a menu bar to render panel options easily accessible.
        menuRoot = new JMenuBar();
        
        // Create the base menu options.
        int MENU_FILE = 0;
        int MENU_VIEW = 1;
        int MENU_SCALE = 2;
        menu = new JMenu[3];
        
        // ==== Instantiate the File Menu ===========================
        // ==========================================================
        
        // Define the file menu.
        menu[MENU_FILE] = new JMenu("File");
        menu[MENU_FILE].setMnemonic(KeyEvent.VK_F);
        
        // Define the screenshot menu item.
        menuScreenshot = new JMenuItem("Save Screenshot", KeyEvent.VK_S);
        menuScreenshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuScreenshot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { saveScreenshot(); }
        });
        menu[MENU_FILE].add(menuScreenshot);
        
        // ==== Instantiate the View Menu ===========================
        // ==========================================================
        
        // Define the view menu.
        menu[MENU_VIEW] = new JMenu("View");
        menu[MENU_VIEW].setMnemonic(KeyEvent.VK_V);
        
        // Define the background toggle menu item.
        menuBackground = new JCheckBoxMenuItem("Zero-Energy Color Mapping", usesZeroEnergyColorMapping());
        menuBackground.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
        menuBackground.setMnemonic(KeyEvent.VK_Z);
        menuBackground.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUseZeroEnergyColorMapping(menuBackground.isSelected());
            }
        });
        menu[MENU_VIEW].add(menuBackground);
        
        // Define the toggle highlighting menu item.
        menuHighlight = new JCheckBoxMenuItem("Highlight Active Crystal", usesCrystalHighlighting());
        menuHighlight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
        menuHighlight.setMnemonic(KeyEvent.VK_H);
        menuHighlight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUseCrystalHighlighting(!usesCrystalHighlighting());
            }
        });
        menu[MENU_VIEW].add(menuHighlight);
        
        // Define the mirror x-axis menu item.
        menuMirrorX = new JCheckBoxMenuItem("Mirror x-Axis", isMirroredX());
        menuMirrorX.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        menuMirrorX.setMnemonic(KeyEvent.VK_X);
        menuMirrorX.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { setMirrorX(!isMirroredX()); }
        });
        menu[MENU_VIEW].add(menuMirrorX);
        
        // Define the mirror y-axis menu item.
        menuMirrorY = new JCheckBoxMenuItem("Mirror y-Axis", isMirroredY());
        menuMirrorY.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.ALT_MASK));
        menuMirrorY.setMnemonic(KeyEvent.VK_Y);
        menuMirrorY.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { setMirrorY(!isMirroredY()); }
        });
        menu[MENU_VIEW].add(menuMirrorY);
        
        // ==== Instantiate the Scale Menu ==========================
        // ==========================================================
        
        // Define the scale menu button.
        menu[MENU_SCALE] = new JMenu("Scale");
        menu[MENU_SCALE].setMnemonic(KeyEvent.VK_S);
        
        // Attach the menu to the panel.
        setJMenuBar(menuRoot);
        
        // Define the linear/logarithmic menu items.
        menuScaling = new JRadioButtonMenuItem[2];
        menuScaling[MENU_ITEM_LIN_SCALE] = new JRadioButtonMenuItem("Linear Scale", usesLinearScale());
        menuScaling[MENU_ITEM_LOG_SCALE] = new JRadioButtonMenuItem("Logarithmic Scale", usesLogarithmicScale());
        menuScaling[MENU_ITEM_LIN_SCALE].setMnemonic(KeyEvent.VK_I);
        menuScaling[MENU_ITEM_LOG_SCALE].setMnemonic(KeyEvent.VK_O);
        menuScaling[MENU_ITEM_LIN_SCALE].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { scaleChange(MENU_ITEM_LIN_SCALE); }
        });
        menuScaling[MENU_ITEM_LOG_SCALE].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { scaleChange(MENU_ITEM_LOG_SCALE); }
        });
        menu[MENU_SCALE].add(menuScaling[MENU_ITEM_LIN_SCALE]);
        menu[MENU_SCALE].add(menuScaling[MENU_ITEM_LOG_SCALE]);
        
        // ==== Add the Menus to the Root ===========================
        // ==========================================================
        
        // Add the menu items to the root.
        for(JMenuItem menuItem : menu) { menuRoot.add(menuItem); }
    }
    
    /**
     * Adds the specified crystal listener to receive crystal events
     * from this component when the calorimeter panel's crystal status
     * is changed. If listener <code>cl</code> is <code>null</code>,
     * no exception is thrown and no action is performed. 
     * @param cl - The listener to add.
     */
    public final void addCrystalListener(CrystalListener cl) {
        if(cl != null) { listenerList.add(cl); }
    }
    
    /**
     * Gets all of the crystal listeners attached to this object.
     * @return Returns the crystal listeners as an array of <code>
     * CrystalListener</code> objects.
     */
    public final CrystalListener[] getCrystalListeners() {
        return listenerList.toArray(new CrystalListener[listenerList.size()]);
    }
    
    /**
     * Indicates whether the menu bar is visible or not.
     * @return Returns <code>true</code> if the menu bar is visible
     * and <code>false</code> otherwise.
     */
    public boolean isMenuVisible() {
        return menuRoot.isVisible();
    }
    
    /**
     * Indicates whether the calorimeter panel displays the x-axis
     * in mirrored orientation or not. Mirrored orientation displays
     * positive LCSim coordinates on the left-hand side and negative
     * coordinates on the right-hand side. Regular orientation goes
     * from negative coordinates on the left-hand side to positive
     * coordinates on the right-hand side.
     * @return Returns <code>true</code> if the panel is using mirrored
     * coordinates and <code>false</code> if the panel is using regular
     * coordinates.
     */
    public boolean isMirroredX() {
        return ecalPanel.isMirroredX();
    }
    
    /**
     * Indicates whether the calorimeter panel displays the y-axis
     * in mirrored orientation or not. Mirrored orientation displays
     * positive LCSim coordinates on the bottom side and negative
     * coordinates on the top side. Regular orientation goes from
     * negative coordinates on the bottom side to positive coordinates
     * on the top side.
     * @return Returns <code>true</code> if the panel is using mirrored
     * coordinates and <code>false</code> if the panel is using regular
     * coordinates.
     */
    public boolean isMirroredY() {
        return ecalPanel.isMirroredY();
    }
    
    /**
     * Removes the specified crystal listener so that it no longer
     * receives crystal events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this
     * component. If listener <code>cl</code> is <code>null</code>, no
     * exception is thrown and no action is performed. 
     * @param cl - The listener to remove.
     */
    public final void removeCrystalListener(CrystalListener cl) {
        if(cl != null) { listenerList.remove(cl); }
    }
    
    /**
     * Sets the menu bar to be either visible or hidden.
     * @param isVisible - <code>true</code> indicates that the menu is
     * visible and <code>false</code> that it is not.
     */
    public final void setMenuVisible(boolean isVisible) {
        menuRoot.setVisible(isVisible);
    }
    
    /**
     * Sets whether to mirror the x-axis on the calorimeter display.
     * @param state - <code>true</code> indicates that the axis should
     * be mirrored and <code>false</code> that it should not.
     */
    public void setMirrorX(boolean state) {
        ecalPanel.setMirrorX(state);
        updateStatusPanel();
    }
    
    /**
     * Sets whether to mirror the y-axis on the calorimeter display.
     * @param state - <code>true</code> indicates that the axis should
     * be mirrored and <code>false</code> that it should not.
     */
    public void setMirrorY(boolean state) {
        ecalPanel.setMirrorY(state);
        updateStatusPanel();
    }
    
    /**
     * Sets the value of the indicated status field on the calorimeter
     * display.
     * @param fieldName - The name of the field to set.
     * @param value - The value to display in relation to the field.
     * @throws NoSuchElementException Occurs if an invalid field name
     * is provided for argument <code>fieldName</code>.
     */
    public final void setStatusField(String fieldName, String value) throws NoSuchElementException {
        // Get the index for the indicated field.
        Integer index = fieldMap.get(fieldName);
        
        // If it is null, the field does not exist.
        if(index == null) { throw new NoSuchElementException("Field \"" + fieldName + "\" does not exist."); }
        
        // Otherwise, set the field.
        else { statusPanel.setFieldValue(index, value); }
    }
    
    /**
     * Sets whether the calorimeter panel will highlight any crystals
     * that the mouse cursor passes over.
     * @param state - <code>true</code> indicates that crystals will
     * be highlighted and <code>false</code> that they will not.
     */
    public void setUseCrystalHighlighting(boolean state) {
        ecalPanel.setSelectionHighlighting(state);
    }
    
    /**
     * Sets whether zero-energy crystals should be rendered as grey or
     * whether they should use the minimum-energy color from the color
     * scale.
     * @param state - <code>false</code> indicates that zero-energy 
     * crystals will be rendered in grey and <code>true</code> that
     * they will be rendered as per the color scale.
     */
    public void setUseZeroEnergyColorMapping(boolean state) {
        if(state) { ecalPanel.setDefaultCrystalColor(null); }
        else { ecalPanel.setDefaultCrystalColor(DEFAULT_CRYSTAL_COLOR); }
    }
    
    /**
     * Sets the calorimeter panel to be displayed using a linear scale.
     */
    public void setUseLinearScale() { ecalPanel.setScalingLinear(); }
    
    /**
     * Sets the calorimeter panel to be displayed using a logarithmic
     * scale.
     */
    public void setUseLogarithmicScale() { ecalPanel.setScalingLogarithmic(); }
    
    /**
     * Converts the calorimeter panel's coordinate pair to the LCSim
     * coordinate system.
     * @param panelPoint - A calorimeter panel coordinate pair..
     * @return Returns the coordinate pair in LCSim's coordinate system
     * as an <code>int</code>.
     **/
    public static final Point toEcalPoint(Point panelPoint) {
        // Convert the point coordinates.
        int ix = toEcalX(panelPoint.x);
        int iy = toEcalY(panelPoint.y);
        
        // Return the new point.
        return new Point(ix, iy);
    }
    
    /**
     * Converts the panel x-coordinate to the calorimeter's
     * coordinate system.
     * @param panelX - A panel x-coordinate.
     * @return Returns the x-coordinate in the calorimeter's
     * coordinate system as an <code>int</code>.
     */
    public static final int toEcalX(int panelX) {
        if(panelX > 22) { return panelX - 22; }
        else { return panelX - 23; }
    }
    
    /**
     * Converts the panel y-coordinate to the calorimeter's
     * coordinate system.
     * @param panelY - A panel y-coordinate.
     * @return Returns the y-coordinate in the calorimeter's
     * coordinate system as an <code>int</code>.
     */
    public static final int toEcalY(int panelY) { return 5 - panelY; }
    
    /**
     * Converts the LCSim coordinate pair to the calorimeter panel's
     * coordinate system.
     * @param ecalPoint - An LCSim calorimeter coordinate pair..
     * @return Returns the coordinate pair in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
    public static final Point toPanelPoint(Point ecalPoint) {
        // Convert the point coordinates.
        int ix = toPanelX(ecalPoint.x);
        int iy = toPanelY(ecalPoint.y);
        
        // Return the new point.
        return new Point(ix, iy);
    }
    
    /**
     * Converts the LCSim x-coordinate to the calorimeter panel's
     * coordinate system.
     * @param ecalX - An LCSim calorimeter x-coordinate.
     * @return Returns the x-coordinate in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
    public static final int toPanelX(int ecalX) {
        if (ecalX <= 0) { return ecalX + 23; }
        else { return ecalX + 22; }
    }
    
    /**
     * Converts the LCSim y-coordinate to the calorimeter panel's
     * coordinate system.
     * @param ecalY - An LCSim calorimeter y-coordinate.
     * @return Returns the y-coordinate in the calorimeter panel's
     * coordinate system as an <code>int</code>.
     **/
    public static final int toPanelY(int ecalY) { return 5 - ecalY; }
    
    /**
     * Indicates whether crystal will be highlighted when the cursor
     * passes over them or not.
     * @return Returns <code>true</code> if crystals will be highlighted
     * and <code>false</code> if they will not.
     */
    public boolean usesCrystalHighlighting() {
        return ecalPanel.isSelectionEnabled();
    }
    
    /**
     * Indicates whether zero-energy crystals are colored using the
     * standard color scale or rendered in grey.
     * @return Returns <code>false</code> if zero-energy crystal will
     * be rendered in grey and <code>true</code> if they will use the
     * regular color scale.
     */
    public boolean usesZeroEnergyColorMapping() {
        return (ecalPanel.getDefaultCrystalColor() == null);
    }
    
    /**
     * Indicates whether the panel is scaled linearly.
     * @return Returns <code>true</code> if the scaling is linear and
     * <code>false</code> if the scaling is logarithmic.
     */
    public boolean usesLinearScale() {
        return ecalPanel.isScalingLinear();
    }
    
    /**
     * Indicates whether the panel is scaled logarithmically.
     * @return Returns <code>true</code> if the scaling is logarithmic
     * and <code>false</code> if the scaling is linear.
     */
    public boolean usesLogarithmicScale() {
        return ecalPanel.isScalingLogarithmic();
    }
    
    /**
     * Adds a new field to the status panel.
     * @param fieldName - The name to display for the field and that
     * links to the field when calling <code>setStatusField</code>.
     */
    protected final void addStatusField(String fieldName) {
        fieldMap.put(fieldName, statusPanel.getFieldCount());
        statusPanel.addField(fieldName);
    }
    
    /**
     * Inserts the field at the indicated location on the status panel.
     * @param index - The index at which to insert the field.
     * @param fieldName - The name to display for the field and that
     * links to the field when calling <code>setStatusField</code>.
     */
    protected final void insertStatusField(int index, String fieldName) {
        statusPanel.insertField(index, fieldName);
        fieldMap = statusPanel.getFieldNameIndexMap();
    }
    
    /**
     * Updates the information on the status panel to match that of
     * the calorimeter panel's currently selected crystal.
     */
    protected void updateStatusPanel() {
        // Get the currently selected crystal.
        Point crystal = ecalPanel.getSelectedCrystal();
        
        // If the crystal is null, there is no selection.
        if(crystal == null || ecalPanel.isCrystalDisabled(crystal.x, crystal.y)) {
            statusPanel.clearFields();
        }
        
        // Otherwise, write the crystal's data to the panel.
        else {
            setStatusField(defaultFields[X_INDEX], String.valueOf(toEcalX(crystal.x)));
            setStatusField(defaultFields[Y_INDEX], String.valueOf(toEcalY(crystal.y)));
            DecimalFormat formatter = new DecimalFormat("0.####E0");
            String energy = formatter.format(ecalPanel.getCrystalEnergy(crystal.x, crystal.y));
            setStatusField(defaultFields[CELL_VALUE], energy);
        }
    }
    
    /**
     * Handles proper resizing of the window and its components.
     */
    private void resize() {
        // Define the size constants.
        int statusHeight = 125;
        
        // Size and position the calorimeter display.
        ecalPanel.setLocation(0, 0);
        ecalPanel.setSize(getContentPane().getWidth(), getContentPane().getHeight() - statusHeight);
        
        // Size and position the status panel.
        statusScroller.setLocation(0, ecalPanel.getHeight());
        statusScroller.setSize(getContentPane().getWidth(), statusHeight);
        statusPanel.setSize(statusScroller.getViewport().getSize());
    }
    
    /**
     * Saves a screenshot to the application root directory.
     */
    private void saveScreenshot() {
        // Make a new buffered image on which to draw the content pane.
        BufferedImage screenshot = new BufferedImage(getContentPane().getWidth(),
                getContentPane().getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        // Paint the content pane to image.
        getContentPane().paint(screenshot.getGraphics());
        
        JFileChooser chooser = new JFileChooser();
        if(chooser.showSaveDialog(this) == JFileChooser.CANCEL_OPTION) {
            return;
        }
        
        // Parse the file name and make sure that it ends in .PNG.
        String filepath = chooser.getSelectedFile().getAbsolutePath();
        int index = filepath.lastIndexOf('.');
        if(index == -1) { filepath = filepath + ".png"; }
        else {
            if(filepath.substring(index + 1).compareTo("png") != 0) {
                filepath = filepath.substring(0, index) + ".png";
            }
        }
        
        // Get the lowest available file name.
        File imageFile = new File(filepath);
        
        // Save the image to a PNG file.
        try { ImageIO.write(screenshot, "PNG", imageFile); }
        catch(IOException ioe) {
            System.err.println("Error saving file \"" + imageFile.getName() + "\".");
        }
        System.out.println("Screenshot saved to: " + imageFile.getAbsolutePath());
    }
    
    /**
     * Handles events generated by the scaling options radio buttons
     * in the scaling menu.
     * @param activatingIndex - The index of the radio button that
     * triggered the event.
     */
    private void scaleChange(int activatingIndex) {
        // If neither radio box is selected, then whichever caused the
        // activation event was unselected. It should be selected again
        // and the event ignored.
        if(!menuScaling[MENU_ITEM_LIN_SCALE].isSelected() && !menuScaling[MENU_ITEM_LOG_SCALE].isSelected()) {
            menuScaling[activatingIndex].setSelected(true);
        }
        
        // Otherwise, whichever did not activate the event should be
        // unselected and the scaling changed accordingly.
        else {
            // If linear scaling activated the event...
            if(activatingIndex == MENU_ITEM_LIN_SCALE) {
                menuScaling[MENU_ITEM_LIN_SCALE].setSelected(true);
                menuScaling[MENU_ITEM_LOG_SCALE].setSelected(false);
                setUseLinearScale();
            }
            
            // If logarithmic scaling activated the event...
            else {
                menuScaling[MENU_ITEM_LIN_SCALE].setSelected(false);
                menuScaling[MENU_ITEM_LOG_SCALE].setSelected(true);
                setUseLogarithmicScale();
            }
        }
    }
    
    /**
     * The <code>EcalMouseListener</code> handles removing highlighting
     * and crystal field information when the cursor leaves the window.
     * It also triggers crystal click events.
     */
    private class EcalMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            // If there is a selected crystal, trigger a crystal click event.
            if(ecalPanel.getSelectedCrystal() != null) {
                // Get the selected crystal.
                Point crystal = ecalPanel.getSelectedCrystal();
                
                // Construct a crystal event.
                CrystalEvent ce = new CrystalEvent(Viewer.this, crystal);
                
                // Loop through all the crystal listeners and trigger them.
                for(CrystalListener cl : listenerList) { cl.crystalClicked(ce); }
            }
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            ecalPanel.clearSelectedCrystal();
            statusPanel.clearFields();
        }
    }
    
    /**
     * The <code>EcalMouseMotionListener</code> handles updating of
     * the highlighted crystal and status panel information when the
     * mouse moves over the window. Additionally triggers crystal
     * activation and deactivation events.
     */
    private class EcalMouseMotionListener extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            // Get the panel coordinates.
            int x = e.getX();
            int y = e.getY();
            
            // Get the crystal index for these coordinates.
            Point crystal = ecalPanel.getCrystalID(x, y);
            
            // If either of the crystal indices are negative, then
            // the mouse is not in a crystal and the selection should
            // be cleared.
            boolean validCrystal = (crystal != null);
            
            // Get the currently selected calorimeter crystal.
            Point curCrystal = ecalPanel.getSelectedCrystal();
            
            // Perform event comparison checks.
            boolean[] nullCrystal = { !validCrystal, curCrystal == null };
            boolean[] disabledCrystal = { true, true };
            if(!nullCrystal[0]) { disabledCrystal[0] = ecalPanel.isCrystalDisabled(crystal); }
            if(!nullCrystal[1]) { disabledCrystal[1] = ecalPanel.isCrystalDisabled(curCrystal); }
            boolean sameCrystal = true;
            if(validCrystal) { sameCrystal = crystal.equals(curCrystal); }
            
            // If the crystals are the same, there are no events to throw.
            if(!sameCrystal) {
                // If the new crystal is non-null and enabled, throw an event.
                if(!nullCrystal[0] && !disabledCrystal[0]) { throwActivationEvent(crystal); }
                
                // If the old crystal is non-null and enabled, throw an event.
                if(!nullCrystal[1] && !disabledCrystal[1]) { throwDeactivationEvent(curCrystal); }
            }
            
            // If the crystal is valid, then set the selected crystal
            // to the current one.
            if(validCrystal) { ecalPanel.setSelectedCrystal(crystal); }
            
            // Otherwise, clear the selection.
            else { ecalPanel.clearSelectedCrystal(); }
            
            // Update the status panel.
            updateStatusPanel();
        }
        
        /**
         * Triggers crystal activation events on all listeners for
         * this component.
         * @param activatedCrystal - The panel coordinates for the
         * activated crystal.
         */
        private void throwActivationEvent(Point activatedCrystal) {
            // Create a crystal event.
            CrystalEvent ce = new CrystalEvent(Viewer.this, activatedCrystal);
            
            // Throw the event with every listener.
            for(CrystalListener cl : listenerList) { cl.crystalActivated(ce); }
        }
        
        /**
         * Triggers crystal deactivation events on all listeners for
         * this component.
         * @param deactivatedCrystal - The panel coordinates for the
         * deactivated crystal.
         */
        private void throwDeactivationEvent(Point deactivatedCrystal) {
            // Create a crystal event.
            CrystalEvent ce = new CrystalEvent(Viewer.this, deactivatedCrystal);
            
            // Throw the event with every listener.
            for(CrystalListener cl : listenerList) { cl.crystalDeactivated(ce); }
        }
    }
    
    /**
     * Updates the settings panel whenever a tracked property in the
     * calorimeter panel is updated.
     * 
     */
    private class PropertyUpdater implements SettingsListener {
        @Override
        public void settingChanged(SettingsEvent e) {
            // If the highlighting behavior has changed...
            if(e.getID() == SettingsEvent.PROPERTY_HOVER_HIGHLIGHT) {
                menuHighlight.setSelected(ecalPanel.isSelectionEnabled());
            }
            
            // If the scaling type has changed...
            else if(e.getID() == SettingsEvent.PROPERTY_SCALE_TYPE) {
                menuScaling[MENU_ITEM_LIN_SCALE].setSelected(ecalPanel.isScalingLinear());
                menuScaling[MENU_ITEM_LOG_SCALE].setSelected(ecalPanel.isScalingLogarithmic());
            }
            
            // If the x-axis orientation has changed...
            else if(e.getID() == SettingsEvent.PROPERTY_X_ORIENTATION) {
                menuMirrorX.setSelected(ecalPanel.isMirroredX());
            }
            
            // If the y-axis orientation has changed...
            else if(e.getID() == SettingsEvent.PROPERTY_Y_ORIENTATION) {
                menuMirrorY.setSelected(ecalPanel.isMirroredY());
            }
            
            // If the zero-energy color mapping has changed...
            else if(e.getID() == SettingsEvent.PROPERTY_ZERO_ENERGY_COLOR) {
                menuBackground.setSelected(ecalPanel.getDefaultCrystalColor() == null);
            }
        }
    }
}