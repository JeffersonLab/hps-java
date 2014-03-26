/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.vertexing;

import org.lcsim.event.Vertex;

/**
 *
 * @author phansson
 */
public interface SimpleVertexer {

    public void fitVertex();
    public Vertex getFittedVertex();
    
}
