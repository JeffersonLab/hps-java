package org.lcsim.hps.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Lightweight driver for stepping through events in offline analysis, without
 * running the MonitoringApp.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: RunControlDialog.java,v 1.1 2013/10/25 19:41:01 jeremy Exp $
 * @deprecated
 */
@Deprecated
public class RunControlDialog implements ActionListener {

    JButton nextButton;
    JButton runButton;
    JButton saveButton;
    JFrame nextFrame;
    boolean run = false;
    final Object syncObject = new Object();

    public RunControlDialog() {
        nextFrame = new JFrame();
        nextFrame.setAlwaysOnTop(true);
        nextFrame.getContentPane().setLayout(new BoxLayout(nextFrame.getContentPane(), BoxLayout.X_AXIS));

        nextButton = new JButton("Next event");
        nextButton.addActionListener(this);
        nextFrame.add(nextButton);

        runButton = new JButton("Run");
        runButton.addActionListener(this);
        nextFrame.add(runButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        nextFrame.add(saveButton);

        nextFrame.pack();
        nextFrame.setVisible(true);
        nextFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    //return true to process the event; return false to discard; waits if in pause mode
    public boolean process(EventHeader event) {
        if (!run) {
            synchronized (syncObject) {
                try {
                    syncObject.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return true;
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
        if (ae.getSource() == saveButton) {
            savePlots();
        }
    }

    /**
     * Save plots to a selected output file.
     */
    private void savePlots() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showSaveDialog(nextFrame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File fileName = fc.getSelectedFile();
            try {
                AIDA.defaultInstance().saveAs(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
