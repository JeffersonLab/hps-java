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
 * This is the panel with buttons for connecting or disconnecting from the session
 * and controlling the application from pause mode.
 */
class EventButtonsPanel extends JPanel implements PropertyChangeListener {

    JButton nextButton;
    JButton pauseButton;
    JButton connectButton;
    JButton resumeButton;
    
    static final ImageIcon connectedIcon = getImageIcon("/monitoringButtonGraphics/connected-128.png");
    static final ImageIcon disconnectedIcon = getImageIcon("/monitoringButtonGraphics/disconnected-128.png");
    
    EventButtonsPanel(ConnectionStatusModel connectionModel, ActionListener listener) {
        
        connectionModel.addPropertyChangeListener(this);
        
        setLayout(new FlowLayout());
        connectButton = addButton(disconnectedIcon, Commands.CONNECT, listener, true);
        resumeButton = addButton("/toolbarButtonGraphics/media/Play24.gif", Commands.RESUME, listener, false);
        pauseButton = addButton("/toolbarButtonGraphics/media/Pause24.gif", Commands.PAUSE, listener, false);
        nextButton = addButton("/toolbarButtonGraphics/media/StepForward24.gif", Commands.NEXT, listener, false);
    }
           
    final JButton addButton(ImageIcon icon, String command, ActionListener listener, boolean enabled) {
        JButton button = new JButton();
        button.setIcon(icon);
        button.setEnabled(enabled);
        button.addActionListener(listener);
        button.setActionCommand(command);
        this.add(button);
        return button;
    }
    
    final JButton addButton(String resource, String actionCommand, ActionListener listener, boolean enabled) {
        return addButton(getImageIcon(resource), actionCommand, listener, enabled);
    }
        
    static ImageIcon getImageIcon(String resource) {
        Image image = null;
        try {
            image = ImageIO.read(EventButtonsPanel.class.getResource(resource));
            image = image.getScaledInstance(24, 24, 0);
        } catch (IOException e) {            
        }
        return new ImageIcon(image);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            setConnectionStatus((ConnectionStatus) evt.getNewValue());
        } else if (evt.getPropertyName().equals(ConnectionStatusModel.PAUSED_PROPERTY)) {
            setPaused((boolean) evt.getNewValue());
        }
    }
    
    void setConnectionStatus(final ConnectionStatus status) {
        if (status.equals(ConnectionStatus.DISCONNECTED)) {
            nextButton.setEnabled(false);
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            connectButton.setActionCommand(Commands.CONNECT);
            connectButton.setIcon(disconnectedIcon);
            connectButton.setToolTipText("Start new session");
            connectButton.setEnabled(true);
        } else if (status.equals(ConnectionStatus.DISCONNECTING)) {
            nextButton.setEnabled(false);
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            connectButton.setEnabled(false);
        } else if (status.equals(ConnectionStatus.CONNECTED)) {
            nextButton.setEnabled(false);            
            pauseButton.setEnabled(true);
            resumeButton.setEnabled(false);
            connectButton.setActionCommand(Commands.DISCONNECT);
            connectButton.setIcon(connectedIcon);
            connectButton.setToolTipText("Disconnect from session");
            connectButton.setEnabled(true);
        }
    }       
    
    void setPaused(final boolean paused) {
        resumeButton.setEnabled(paused);
        pauseButton.setEnabled(!paused);
        nextButton.setEnabled(paused);
    }
}