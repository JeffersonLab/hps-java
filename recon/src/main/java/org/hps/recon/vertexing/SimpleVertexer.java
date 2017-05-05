package org.hps.recon.vertexing;

import org.lcsim.event.Vertex;

public interface SimpleVertexer {

    public void fitVertex();
    public Vertex getFittedVertex();
    
}
