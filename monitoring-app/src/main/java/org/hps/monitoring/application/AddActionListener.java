package org.hps.monitoring.application;

import java.awt.event.ActionListener;

/**
 * Mix-in interface for components which can be assigned an <code>ActionListener</code>.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
interface AddActionListener {

    /**
     * Set an <code>ActionListener</code> for this component, which should assign it to appropriate child components
     * that emit <code>ActionEvent</code> objects.
     *
     * @param listener the <code>ActionListener</code> to assign to this component
     */
    void addActionListener(ActionListener listener);
}
