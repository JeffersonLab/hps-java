package org.hps.monitoring.application;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;

/**
 * This is the panel with buttons for connecting or disconnecting from the session and controlling the application when
 * event processing is paused.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class EventButtonsPanel extends JPanel implements PropertyChangeListener {

    /**
     * Icon when application is connected to event processing session.
     */
    static final ImageIcon CONNECTED_ICON = getImageIcon("/monitoringButtonGraphics/connected-128.png");

    /**
     * Icon when application is disconnected from event processing.
     */
    static final ImageIcon DISCONNECTED_ICON = getImageIcon("/monitoringButtonGraphics/disconnected-128.png");

    /**
     * The icon size (width and height) in pixels.
     */
    private static final int ICON_SIZE = 24;

    /**
     * Get an image icon from a jar resource.
     *
     * @param resource the resource path
     * @return the image icon
     */
    static ImageIcon getImageIcon(final String resource) {
        Image image = null;
        try {
            image = ImageIO.read(EventButtonsPanel.class.getResource(resource));
            image = image.getScaledInstance(ICON_SIZE, ICON_SIZE, 0);
        } catch (final IOException e) {
        }
        return new ImageIcon(image);
    }

    /**
     * Button for connect and disconnect which is toggled depending on state.
     */
    private final JButton connectButton;

    /**
     * Button for getting next event when paused.
     */
    private final JButton nextButton;

    /**
     * Button for pausing the event processing.
     */
    private final JButton pauseButton;

    /**
     * Button for resuming the event processing.
     */
    private final JButton resumeButton;

    /**
     * Class constructor.
     *
     * @param connectionModel the global connection model
     * @param listener the action listener
     */
    EventButtonsPanel(final ConnectionStatusModel connectionModel, final ActionListener listener) {

        connectionModel.addPropertyChangeListener(this);

        setLayout(new FlowLayout());
        this.connectButton = addButton(DISCONNECTED_ICON, Commands.CONNECT, listener, true);
        this.resumeButton = addButton("/toolbarButtonGraphics/media/Play24.gif", Commands.RESUME, listener, false);
        this.pauseButton = addButton("/toolbarButtonGraphics/media/Pause24.gif", Commands.PAUSE, listener, false);
        this.nextButton = addButton("/toolbarButtonGraphics/media/StepForward24.gif", Commands.NEXT, listener, false);
    }

    /**
     * Add a button to the panel.
     *
     * @param icon the button's image icon
     * @param command the command for the action event
     * @param listener the action listener which handles action events
     * @param enabled <code>true</code> if button should be enabled initially
     * @return the new button object
     */
    private JButton addButton(final ImageIcon icon, final String command, final ActionListener listener,
            final boolean enabled) {
        final JButton button = new JButton();
        button.setIcon(icon);
        button.setEnabled(enabled);
        button.addActionListener(listener);
        button.setActionCommand(command);
        this.add(button);
        return button;
    }

    /**
     * Add a button to the panel.
     *
     * @param resource the resource for the image icon
     * @param actionCommand the command for the action event
     * @param listener the action listener which handles action events
     * @param enabled <code>true</code> if button should be enabled initially
     * @return the new button object
     */
    private JButton addButton(final String resource, final String actionCommand, final ActionListener listener,
            final boolean enabled) {
        return addButton(getImageIcon(resource), actionCommand, listener, enabled);
    }

    /**
     * Handle property change events to set status from changes to the connection status model.
     *
     * @param evt the <code>PropertyChangeEvent</code> to handle
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            setConnectionStatus((ConnectionStatus) evt.getNewValue());
        } else if (evt.getPropertyName().equals(ConnectionStatusModel.PAUSED_PROPERTY)) {
            setPaused((boolean) evt.getNewValue());
        }
    }

    /**
     * Set the connection status to update the button state.
     *
     * @param status the new connection status
     */
    private void setConnectionStatus(final ConnectionStatus status) {
        if (status.equals(ConnectionStatus.DISCONNECTED)) {
            this.nextButton.setEnabled(false);
            this.pauseButton.setEnabled(false);
            this.resumeButton.setEnabled(false);
            this.connectButton.setActionCommand(Commands.CONNECT);
            this.connectButton.setIcon(DISCONNECTED_ICON);
            this.connectButton.setToolTipText("Start new session");
            this.connectButton.setEnabled(true);
        } else if (status.equals(ConnectionStatus.DISCONNECTING)) {
            this.nextButton.setEnabled(false);
            this.pauseButton.setEnabled(false);
            this.resumeButton.setEnabled(false);
            this.connectButton.setEnabled(false);
        } else if (status.equals(ConnectionStatus.CONNECTED)) {
            this.nextButton.setEnabled(false);
            this.pauseButton.setEnabled(true);
            this.resumeButton.setEnabled(false);
            this.connectButton.setActionCommand(Commands.DISCONNECT);
            this.connectButton.setIcon(CONNECTED_ICON);
            this.connectButton.setToolTipText("Disconnect from session");
            this.connectButton.setEnabled(true);
        }
    }

    /**
     * Set pause mode.
     *
     * @param paused <code>true</code> to set paused state
     */
    private void setPaused(final boolean paused) {
        this.resumeButton.setEnabled(paused);
        this.pauseButton.setEnabled(!paused);
        this.nextButton.setEnabled(paused);
    }
}
