package org.hps.monitoring.application.util;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class DialogUtil {

    public static JDialog showStatusDialog(final Component parentComponent, String title, String message) {
        final JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[] {}, null);
        final JDialog dialog = new JDialog();
        dialog.setContentPane(optionPane);
        dialog.setTitle(title);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.pack();
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
                parentComponent.setEnabled(true);
            }
        });
        parentComponent.setEnabled(false);
        optionPane.setVisible(true);
        dialog.setVisible(true);
        return dialog;
    }

    public static void showErrorDialog(final Component component, final Throwable error, final String title) {
        final Runnable runnable = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(component, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    public static void showInfoDialog(final Component component, final String title, final String message) {
        final Runnable runnable = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(component, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
