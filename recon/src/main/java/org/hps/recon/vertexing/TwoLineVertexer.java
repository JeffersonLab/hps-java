/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.vertexing;

import org.lcsim.event.base.BaseVertex;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import hep.physics.matrix.SymmetricMatrix;


/**
 * Class to calculate the POCA between two straight lines
 *
 * @author phansson
 */
public class TwoLineVertexer extends BaseSimpleVertexer {
    protected Hep3Vector A1,A2,B1,B2;
	
	public TwoLineVertexer() {
		
	}
	
    public void setLines(Hep3Vector PA1, Hep3Vector PA2, Hep3Vector PB1, Hep3Vector PB2) {
    	this.A1 = PA1;
    	this.A2 = PA2;
    	this.B1 = PB1;
    	this.B2 = PB2;
    }
  
    public void clear() {
    	super.clear();
    	setLines(null,null,null,null);
    }
    
    public boolean isValid() {
        if(A1==null || A2==null || B1==null || B2==null) return false;
        else return true;
    }
    
    @Override
    public void fitVertex() {
    	assert isValid();
    	Hep3Vector vtxPosition = getPOCALineToLine();
		if (vtxPosition!=null) {
			_fitted_vertex = new BaseVertex(true, "Two Line Vertexer", 0, 0, new SymmetricMatrix(0), vtxPosition, null); 
    	}
    }
    
    /**
     * Function to calculate DCA in 3D of two lines
     * @param A1 Starting 3D space point for one of the straight lines 
     * @param A2 End 3D space point for one of the straight lines
     * @param B1 Starting 3D space point for one of the straight lines
     * @param B2 End 3D space point for one of the straight lines
     * @return position of closest approach of the two lines defined by the input parameters
     */
    private Hep3Vector getPOCALineToLineAlt() {

        /*
         * Line1 defined by A1 and A2
         * Line2 defined by B1 and B2
         *
         * nA = dot(cross(B2-B1,A1-B1),cross(A2-A1,B2-B1));
         * nB = dot(cross(A2-A1,A1-B1),cross(A2-A1,B2-B1));
         * d = dot(cross(A2-A1,B2-B1),cross(A2-A1,B2-B1));
         * A0 = A1 + (nA/d)*(A2-A1);
         * B0 = B1 + (nB/d)*(B2-B1);
         */
        if(_debug) System.out.printf("%s: A1=%s A2=%s B1=%s B2=%s\n", this.getClass().getSimpleName(), A1.toString(), A2.toString(), B1.toString(), B2.toString());
        double nA = VecOp.dot(VecOp.cross(VecOp.sub(B2, B1), VecOp.sub(A1, B1)), VecOp.cross(VecOp.sub(A2, A1), VecOp.sub(B2, B1)));
        double nB = VecOp.dot(VecOp.cross(VecOp.sub(A2, A1), VecOp.sub(A1, B1)), VecOp.cross(VecOp.sub(A2, A1), VecOp.sub(B2, B1)));
        double d = VecOp.dot(VecOp.cross(VecOp.sub(A2, A1), VecOp.sub(B2, B1)), VecOp.cross(VecOp.sub(A2, A1), VecOp.sub(B2, B1)));
        Hep3Vector A0 = VecOp.add(A1, VecOp.mult(nA / d, VecOp.sub(A2, A1)));
        Hep3Vector B0 = VecOp.add(B1, VecOp.mult(nB / d, VecOp.sub(B2, B1)));
        Hep3Vector diff = VecOp.sub(B0, A0);
        Hep3Vector tmp = VecOp.mult(0.5, diff);
        Hep3Vector vertexPos = VecOp.add(A0, tmp);
        if(_debug) System.out.printf("%s: A0=%s B0=%s ==> vtxPos=%s (tmp=%s)\n", this.getClass().getSimpleName(), A0.toString(), B0.toString(), vertexPos.toString(), tmp.toString());
        return vertexPos;
    }
    
    /**
     * Function to calculate DCA in 3D of two lines
     * @param A1 Starting 3D space point for one of the straight lines 
     * @param A2 End 3D space point for one of the straight lines
     * @param B1 Starting 3D space point for one of the straight lines
     * @param B2 End 3D space point for one of the straight lines
     * @return position of closest approach of the two lines defined by the input parameters
     */
    private Hep3Vector getPOCALineToLine() {

		if(_debug) System.out.printf("%s: A1=%s A2=%s B1=%s B2=%s\n", this.getClass().getSimpleName(), A1.toString(), A2.toString(), B1.toString(), B2.toString());

    	double ya[][] = {VecOp.mult(-1,VecOp.sub(B1, A1)).v()};
    	BasicMatrix y = (BasicMatrix)MatrixOp.transposed(new BasicMatrix(ya));
    	Hep3Vector dB = VecOp.sub(B2, B1);
    	Hep3Vector dA = VecOp.sub(A2, A1);
    	BasicMatrix X = new BasicMatrix(3,2);
    	for(int col=0;col<2;++col) {
    		if(col==0) {
    			X.setElement(0, col, dB.x());
    			X.setElement(1, col, dB.y());
    			X.setElement(2, col, dB.z());
    		} else {
    			X.setElement(0, col, -1*dA.x());
    			X.setElement(1, col, -1*dA.y());
    			X.setElement(2, col, -1*dA.z());
    		}
    	}

    	BasicMatrix X_T = (BasicMatrix)MatrixOp.transposed(X);
    	BasicMatrix XX_T = (BasicMatrix)MatrixOp.mult(X_T, X);
		BasicMatrix IXX_T = null;
		try {
			IXX_T = (BasicMatrix)MatrixOp.inverse(XX_T);
		} 
		catch(MatrixOp.IndeterminateMatrixException e) {
			System.out.printf("%s: caught indeterminate exception %s\n",this.getClass().getSimpleName(),e.getMessage());
			return null;
		}
    	BasicMatrix X_Ty = (BasicMatrix)MatrixOp.mult(X_T,y);
    	BasicMatrix b = (BasicMatrix)MatrixOp.mult(IXX_T, X_Ty);
    	double t = b.e(0, 0);
    	double s = b.e(1, 0);
    	Hep3Vector Bpca = VecOp.add(B1, VecOp.mult(t, dB));
    	Hep3Vector Apca = VecOp.add(A1, VecOp.mult(s, dA));
        Hep3Vector vertex = VecOp.add(Apca, VecOp.mult(0.5, VecOp.sub(Bpca, Apca)));
    	if(_debug) {
    		System.out.printf("y:\n%s\n",y.toString());
    		System.out.printf("X:\n%s\n",X.toString());
    		System.out.printf("b:\n%s\n",b.toString());
        	Hep3Vector ymin = VecOp.add(VecOp.mult(t, dB) , VecOp.mult(s, dA) );
        	Hep3Vector yminprime = VecOp.add(VecOp.sub(B1, A1), ymin);
        	System.out.printf("ymin:\n%s\n",ymin.toString());
        	System.out.printf("yminprime:\n%s\n",yminprime.toString());
        	System.out.printf("Apca:\n%s\n",Apca.toString());
        	System.out.printf("Bpca:\n%s\n",Bpca.toString());
        	System.out.printf("vertex:\n%s\n",vertex.toString());
    	}
    	return vertex;
    	
    	
    }

    
}
