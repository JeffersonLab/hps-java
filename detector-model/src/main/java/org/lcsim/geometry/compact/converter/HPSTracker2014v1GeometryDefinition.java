package org.lcsim.geometry.compact.converter;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;

/**
 * 
 * Updated geometry information for the HPS tracker 2014

 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTracker2014v1GeometryDefinition extends HPSTracker2014GeometryDefinition {

    public HPSTracker2014v1GeometryDefinition(boolean debug, Element node) {
        super(debug, node);
    }

    public static class LongAxialSlotHalfModule extends HPSTracker2014GeometryDefinition.LongAxialSlotHalfModuleBase  {

        public LongAxialSlotHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer,
                String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }
       
        @Override
        protected void applyGenericCoordinateSystemCorrections() {
            
            super.applyGenericCoordinateSystemCorrections();
            
            // apply 180 degree rotation around w to get hybrid on the correct side
            
            if(debug) {
                System.out.printf("%s: Coord before corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
                System.out.printf("%s: box center before corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());
            }
            getCoord().rotateApache(getSlotRotation());

            if(debug) {
                System.out.printf("%s: Coord after corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
                System.out.printf("%s: box center after corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());
            }
        }
    }
    
    public static class LongStereoSlotHalfModule extends HPSTracker2014GeometryDefinition.LongStereoSlotHalfModuleBase {

        public LongStereoSlotHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }
        
        @Override
        protected void applyGenericCoordinateSystemCorrections() {
            
            super.applyGenericCoordinateSystemCorrections();

            if(debug) {
                System.out.printf("%s: v1 LongStereoSlotHalfModule Generic Corrections\n", getClass().getSimpleName());
                System.out.printf("%s: Coord before corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
                System.out.printf("%s: box center before corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());
            }
            
            getCoord().rotateApache(getSlotRotation());
            
            if(debug) {
                System.out.printf("%s: Coord after corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
                System.out.printf("%s: box center after corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());
            }
        }

        
    }

    @Override
    protected LongHalfModule createLongAxialSlotHalfModule(String name, SurveyVolume mother, 
                                                            AlignmentCorrection alignmentCorrection, 
                                                            int layer, String half) {
        return new LongAxialSlotHalfModule(name, mother, alignmentCorrection, layer, half);
    }
    
    @Override
    protected LongHalfModule createLongStereoSlotHalfModule(String name,
            SurveyVolume mother, AlignmentCorrection alignmentCorrection,
            int layer, String half) {
        
        return new LongStereoSlotHalfModule(name, mother, alignmentCorrection, layer, half);
    }

    
    /**
     * PI rotation around generic z-axis
     * @return
     */
    private static Rotation getSlotRotation() {
        return new Rotation(new Vector3D(0,0,1),Math.PI);
    }
    

    
}