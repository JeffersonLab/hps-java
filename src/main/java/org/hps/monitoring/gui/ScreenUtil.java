package org.hps.monitoring.gui;

import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;

/**
 * Miscellaneous utility methods for getting information about the graphics environment.
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
    
    static double getBoundsX(int index) {
        return ScreenUtil.getGraphicsDevice(index).getDefaultConfiguration().getBounds().getX();
    }
    
    static double getBoundsY(int index) {
        return ScreenUtil.getGraphicsDevice(index).getDefaultConfiguration().getBounds().getY();
    }
}
