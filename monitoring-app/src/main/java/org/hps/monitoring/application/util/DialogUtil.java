package org.hps.monitoring.application.util;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class DialogUtil {

    /**
     * @param parent
     * @param message
     * @param title
     * @return
     */
    public static int showConfirmationDialog(final Component parent, final String title, final String message) {
        final Object[] options = { "Yes", "No", "Cancel" };
        final int result = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
        return result;
    }

    /**
     * @param component
     * @param error
     * @param title
     */
    public static void showErrorDialog(final Component component, final String title, final String message) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * @param component
     * @param error
     * @param title
     */
    public static void showErrorDialog(final Component component, final Throwable error, final String title) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(component, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * @param component
     * @param title
     * @param message
     */
    public static void showInfoDialog(final Component component, final String title, final String message) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(component, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * @param parentComponent
     * @param title
     * @param message
     * @return
     */
    public static JDialog showStatusDialog(final Component parentComponent, final String title, final String message) {
        final JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[] {}, null);
        final JDialog dialog = new JDialog();
        dialog.setContentPane(optionPane);
        dialog.setTitle(title);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.pack();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
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
}
