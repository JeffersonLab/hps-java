package org.hps.monitoring.gui;

import javax.swing.JFrame;


/**
 * Simple class for encapsulating the width, height, x position, and
 * y position of a GUI component in the app.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class WindowConfiguration {
    
    int width;
    int height;
    int x;
    int y;
    
    WindowConfiguration(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
    
    WindowConfiguration(String configuration) {
        String[] splited = configuration.split(" ");
        if (splited.length != 4)
            throw new IllegalArgumentException("Bad configuration string format: " + configuration);
        width = Integer.parseInt(splited[0]);
        height = Integer.parseInt(splited[1]);
        x = Integer.parseInt(splited[2]);
        y = Integer.parseInt(splited[3]);
    }
    
    WindowConfiguration(JFrame frame) {
        width = frame.getWidth();
        height = frame.getHeight();
        x = frame.getLocation().x;
        y = frame.getLocation().y;
    }
    
    public String toString() {
        return width + " " + height + " " + x + " " + y;
    }
}
