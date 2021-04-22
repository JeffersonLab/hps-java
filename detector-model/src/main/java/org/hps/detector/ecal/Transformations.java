package org.hps.detector.ecal;

import java.util.logging.Logger;

import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalCrystalPosition;

/**
 * Performs vector transformations to calculate center of crystal volume from front and back
 * positions as defined in the conditions database.
 */
public final class Transformations {

    public static Logger LOGGER = Logger.getLogger(Transformations.class.getPackage().getName());

    /* vector of translation */
    private GVector transVec; 
    /* array with rotation angles */
    private double[] rotation;

    /* geometrically defined center mass of top module */
    private final GVector vecTop = new GVector(109.88, 230.79, 1008.73); 
    /* geometrically defined center mass of bottom module */
    private final GVector vecBot = new GVector(109.88, -230.79, 1008.73); 
    
    /**
     * Create a new <code>Transformations</code> object. 
     * 
     * @param trans translation in space as an array containing [x,y,z] coordinates
     * @param rot rotation in space as an array with angles [alpha, beta, gamma] or [rx, ry, rz]
     */
    public Transformations(double[] trans, double[] rot) {
        this.transVec = new GVector(trans);
        this.rotation = rot;
    }
    
    /**
     * perform rotation and translation operation for a single crystal
     * 
     * @param position crystal position 
     * @param channel crystal channel
     * @return crystal position in G4 conventions
     */
    public Geant4Position transformToGeant4(EcalCrystalPosition position, EcalChannel channel) {
        
        // Define untransformed front and back positions.
        GVector frontVec = new GVector(position.getFrontX(), position.getFrontY(), position.getFrontZ());
        GVector backVec = new GVector(position.getBackX(), position.getBackY(), position.getBackZ());
        
        // Determine local coordinate system based on top or bottom half.
        GVector coordOrigin;
        if (channel.getY() > 0) {
            coordOrigin = vecTop;
        } else {
            coordOrigin = vecBot;
        }
        
        // Transform front and back global positions into the local coordinate system.
        frontVec = transformVector(frontVec, coordOrigin);
        backVec = transformVector(backVec, coordOrigin);
        
        // Return G4 position using front and back position vectors.
        return new Geant4Position(frontVec, backVec);
    }
       
    private GVector transformVector(GVector vec, GVector coordOrigin) {
        return vec.TranslateBy(coordOrigin.getOpposite())
                .RotateBy(this.rotation[0], this.rotation[1], this.rotation[2])
                .TranslateBy(transVec)
                .TranslateBy(coordOrigin);
    }
}
