package org.hps.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Lightweight driver for stepping through events in offline analysis, without
 * running the MonitoringApp.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: RunControlDriver.java,v 1.1 2013/10/25 19:41:01 jeremy Exp $
 * @deprecated
 */
@Deprecated
public class RunControlDriver extends Driver implements ActionListener {

    JButton nextButton;
    JButton runButton;
    JFrame nextFrame;
    boolean run = false;
    final Object syncObject;

    public RunControlDriver() {
        syncObject = new Object();
    }

    @Override
    protected void startOfData() {
        nextFrame = new JFrame();
        nextFrame.setAlwaysOnTop(true);
        nextFrame.getContentPane().setLayout(new BoxLayout(nextFrame.getContentPane(), BoxLayout.X_AXIS));

        nextButton = new JButton("Next event");
        nextButton.addActionListener(this);
        nextFrame.add(nextButton);
        runButton = new JButton("Run");
        runButton.addActionListener(this);
        nextFrame.add(runButton);

        nextFrame.pack();
        nextFrame.setVisible(true);
    }

    @Override
    protected void process(EventHeader event) {
        if (!run) {
            synchronized (syncObject) {
                try {
                    syncObject.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    protected void endOfData() {
        nextFrame.setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == nextButton) {
            synchronized (syncObject) {
                syncObject.notify();
            }
        }
        if (ae.getSource() == runButton) {
            if (!run) {
                runButton.setText("Pause");
                run = true;
                synchronized (syncObject) {
                    syncObject.notify();
                }
            } else {
                run = false;
                runButton.setText("Run");
            }
        }
    }
}
