package org.hps.users.phansson.alignment;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.Hep3Vector;

public class GlobalParameter {
    
    private String _name;
    private int _side;
    private int _layer;
    private int _type;
    private int _direction;
    
    private boolean _active;
    private BasicMatrix _dfdp = null;
    
   
    
    public GlobalParameter(String name,int side,int layer,int type,int direction,boolean active) {
        _name = name;
        _side = side;
        _layer = layer;
        _type = type;
        _direction = direction;
        _active = active;
    
    }

    public void setParameters(String name,int side,int layer,int type,int direction,boolean active) {
        clear();
        _name = name;
        _side = side;
        _layer = layer;
        _type = type;
        _direction = direction;
        _active = active;
    }

    public void clear() {
        _name = "";
        _side = -1;
        _layer = -1;
        _type = -1;
        _direction = -1;
        _active = false;
        _dfdp=null;
    }
    
    public void setDfDp(BasicMatrix mat) {
        _dfdp = mat;
    }

    public void setDfDp(Hep3Vector v) {
        _dfdp = new BasicMatrix(3,1);
        _dfdp.setElement(0, 0, v.x());
        _dfdp.setElement(1, 0, v.y());
        _dfdp.setElement(2, 0, v.z());
    }

    public boolean active() {
        return _active;
    }
    
    public BasicMatrix getDfDp(BasicMatrix mat) {
        return _dfdp;
    }
    
    public double dfdp(int dim) {
        return _dfdp.e(dim,0);
    }
    
    public int getLabel() {
        int label = _side + _type + _direction + _layer;  
        return label;
    }
    
    public int getSide() {
        return _side;
    }
    
    public int getLayer() {
        return _layer;
    }
    
    public int getType() {
        return _type;
    }
    
    public int getDirection() {
        return _direction;
    }

    public String getName() {
        return _name;
    }
    
    public void print() {
        System.out.println("GP: " + _name + " " + " side " + _side + " layer " + _layer + " type " + _type + " dir " + _direction + " label " + getLabel() + " Active: " + _active);
        if(_dfdp==null) {
            System.out.println("dfdp not evaluated yet");
        } else {
            System.out.println("dfdp: " + _dfdp.e(0,0) + " , " + _dfdp.e(1,0) + " , " + _dfdp.e(2, 0));
        }
    }


}
