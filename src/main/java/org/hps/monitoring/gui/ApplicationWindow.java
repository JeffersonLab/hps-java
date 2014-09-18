package org.hps.monitoring.gui;

import java.awt.Dimension;

import javax.swing.JFrame;

/**
 * An abstract class for windows in the monitoring application.
 */
abstract class ApplicationWindow extends JFrame {
    
    WindowConfiguration defaultWindowConfiguration;
    WindowConfiguration currentWindowConfiguration;
    
    ApplicationWindow(String title) {
        this.setTitle(title);
    }
            
    final void updateWindowConfiguration(WindowConfiguration windowConfiguration) {
        currentWindowConfiguration = windowConfiguration;
        setSize(new Dimension(windowConfiguration.width, windowConfiguration.height));
        setLocation(windowConfiguration.x, windowConfiguration.y);
    }
    
    final void setDefaultWindowConfiguration(WindowConfiguration windowConfiguration) {
        defaultWindowConfiguration = windowConfiguration;
        updateWindowConfiguration(windowConfiguration);
    }
    
    final void resetWindowConfiguration() {
        if (defaultWindowConfiguration != null) {
            setDefaultWindowConfiguration(defaultWindowConfiguration);
            updateWindowConfiguration(defaultWindowConfiguration);
        }
    }
}
