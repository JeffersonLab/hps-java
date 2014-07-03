package org.hps.monitoring.gui;

import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;

/**
 * Miscellaneous utility methods for getting information about the graphics environment.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class ScreenUtil {

    static GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    private ScreenUtil() {
    }

    static int getScreenWidth() {
        return graphicsDevice.getDisplayMode().getWidth();
    }

    static int getScreenHeight() {
        return graphicsDevice.getDisplayMode().getHeight();
    }

    static void printGraphicsConfig() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
        System.out.println("printing graphics config ...");
        for (GraphicsDevice graphicsDevice : graphicsDevices) {
            System.out.println(graphicsDevice.getDisplayMode().getWidth() + " x " + graphicsDevice.getDisplayMode().getHeight());
        }
    }
    
    static void printComponentInfo(Component component) {
        if (component instanceof JFrame) {
            System.out.println(((JFrame)component).getTitle());
        } else {
            System.out.println(component);
        }
        System.out.println("location: " + component.getLocation().getX() + ", " + component.getLocation().getY());
        System.out.println("size: " + component.getSize().getWidth() + " x " + component.getSize().getHeight());
        System.out.println();
    }
    
    static GraphicsDevice getGraphicsDevice(int index) {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[index];
    }
    
    static Rectangle getScreenBounds(int index) {
        return ScreenUtil.getGraphicsDevice(index).getDefaultConfiguration().getBounds();
    }

    // TODO: Add multi-monitor config ...
    // http://stackoverflow.com/questions/4627553/java-show-jframe-in-a-specific-screen-in-dual-monitor-configuration
    // Should put main panel on half of screen #1 and system status panel in right half of same screen.
    // The plot panel should fill screen #2.
    
    // TODO: Add single monitor config.
    // Should have main panel in upper left, system status panel in lower left, and plots on right.
}
