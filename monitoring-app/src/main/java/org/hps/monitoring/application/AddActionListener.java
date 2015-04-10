package org.hps.monitoring.application;

import java.awt.event.ActionListener;

/**
 * Mixin interface for components which can be assigned an external <code>ActionListener</code>.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
interface AddActionListener {

    /**
     * Assign an <code>ActionListener</code> to this component which will assign to appropriate child components.
     *
     * @param listener the <code>ActionListener</code> to assign to this component
     */
    void addActionListener(ActionListener listener);
}
