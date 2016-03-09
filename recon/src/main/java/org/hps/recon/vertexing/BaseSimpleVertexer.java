package org.hps.recon.vertexing;

import org.lcsim.event.Vertex;

/**
 * 
 * Base class for simple vertexer objects.
 * 
 * @author phansson
 *
 */
public abstract class BaseSimpleVertexer implements SimpleVertexer {


    protected boolean _debug = false;
    protected Vertex _fitted_vertex = null;

    public BaseSimpleVertexer() {
    }

    @Override
    public abstract void fitVertex();

    @Override
    public Vertex getFittedVertex() {
        return _fitted_vertex;
    }
    
    public void clear() {
        _fitted_vertex = null;
    }
    
    public abstract boolean isValid();
    

}