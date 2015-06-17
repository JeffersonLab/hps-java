/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;

/**
 * Class to keep survey results.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SurveyResult {

    String name;
    String desc;
    private Hep3Vector origin;
    private Hep3Vector x;
    private Hep3Vector y;
    private Hep3Vector z;
    
    public SurveyResult(String name,Element node) {
        this.name = name;
        try {
            setResults(node);
        } catch (DataConversionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    private final void initFromDetectorXmlNode(Element detectorNode) {
        Element element = findResultFromDetector(detectorNode);
        if(element == null) {
            System.out.printf("%s: no element from for %s \n", this.getClass().getSimpleName(),this.name);
        } else {

            try {
                setResults(element);
            } catch (DataConversionException e) {
                new RuntimeException("cannot get the results from xml", e);
            }
        }
    }   
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Name: " + this.getName());
        sb.append(" Desc.: " + this.getDesc() + "\n");
        sb.append("Origin: " + this.getOrigin().toString() + "\n");
        sb.append("unit x: " + this.getX().toString() + "\n");
        sb.append("unit y: " + this.getY().toString() + "\n");
        sb.append("unit z: " + this.getZ().toString() + "\n");
        return sb.toString();
    }
    
    
    public String getDesc() {
        return this.desc;
    }

    private void setResults(Element element) throws DataConversionException {
        
        setDesc(element.getAttributeValue("desc"));
        
        Element eO = element.getChild("origin");
        if (eO == null) throw new RuntimeException("No origin node foudn for this element");


        setOrigin(getUnit(eO));


        List<Element> eUnitVectors = element.getChildren("unitvec");
        if (eUnitVectors == null) throw new RuntimeException("No unitvec node foudn for this element");
        if(eUnitVectors.size()!=3) throw new RuntimeException("Need 3 unitvecs");
        for (Element ev : eUnitVectors) {
            switch (ev.getAttributeValue("name")) {
            case "X": setX(getUnit(ev));
            break;
            case "Y": setY(getUnit(ev));
            break;
            case "Z": setZ(getUnit(ev));
            break;
            default: throw new RuntimeException("This attribute name is wrong " + ev.getAttributeValue("name"));
            }
        }



    }

    public void setDesc(String desc) {
       this.desc = desc;
        
    }

    public static SurveyResult findResultFromDetector(Element detectorNode,  String name) {
        Element elementSurveyVolumes = detectorNode.getChild("SurveyVolumes");
        if(elementSurveyVolumes==null) {
           System.out.printf("WARNING: no XML file for survey information available.\n");
           //throw new RuntimeException("no SurveyVolumes in this xml file.");
           return null;
        }
        List<Element> list = elementSurveyVolumes.getChildren("SurveyVolume");
        for(Element element : list) {
            if(element.getAttribute("name")!=null) {
                if(element.getAttributeValue("name").compareTo(name) == 0) {
                    return new SurveyResult(name, element);
                } 
            } else {
                throw new RuntimeException("this element is not formatted correctly");
            }
        }
        return null;
    
    }
    
    private Element findResultFromDetector(Element detectorNode) {
        Element elementSurveyVolumes = detectorNode.getChild("SurveyVolumes");
             if(elementSurveyVolumes==null) {
                 throw new RuntimeException("no SurveyVolumes in this xml file.");
             }
             List<Element> list = elementSurveyVolumes.getChildren("SurveyVolume");
             for(Element element : list) {
                 if(element.getAttribute("name")!=null) {
                     if(element.getAttributeValue("name").compareTo(name) == 0) {
                         return element;
                     } 
                 } else {
                     throw new RuntimeException("this element is not formatted correctly");
                 }
             }
             return null;
         }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Hep3Vector getX() {
        return x;
    }

    public Hep3Vector getY() {
        return y;
    }

    public Hep3Vector getZ() {
        return z;
    }
    
    public Hep3Vector getOrigin() {
        return origin;
    }

        
    protected void setX(Hep3Vector unit) {
        this.x = unit;
    }
    protected void setY(Hep3Vector unit) {
        this.y = unit;
    }
    protected void setZ(Hep3Vector unit) {
        this.z = unit;
    }
    protected void setOrigin(Hep3Vector unit) {
        this.origin = unit;
    }

    private Hep3Vector getUnit(Element e) throws DataConversionException  {
        Hep3Vector v = new BasicHep3Vector(e.getAttribute("x").getDoubleValue(), e.getAttribute("y").getDoubleValue(), e.getAttribute("z").getDoubleValue());
        return v;
    }


    public void rotate(Rotation rot) {
       setOrigin( new BasicHep3Vector(rot.applyTo(new Vector3D(getOrigin().v())).toArray()) );
       setX( new BasicHep3Vector(rot.applyTo(new Vector3D(getX().v())).toArray()) );
       setY( new BasicHep3Vector(rot.applyTo(new Vector3D(getY().v())).toArray()) );
       setZ( new BasicHep3Vector(rot.applyTo(new Vector3D(getZ().v())).toArray()) );
        
    }
    
    public void rotateOrigin(Rotation rot) {
        setOrigin( new BasicHep3Vector(rot.applyTo(new Vector3D(getOrigin().v())).toArray()) );
    }
    
    public void rotateUnitVectors(Rotation rot) {
        setX( new BasicHep3Vector(rot.applyTo(new Vector3D(getX().v())).toArray()) );
        setY( new BasicHep3Vector(rot.applyTo(new Vector3D(getY().v())).toArray()) );
        setZ( new BasicHep3Vector(rot.applyTo(new Vector3D(getZ().v())).toArray()) );
         
     }


    public Rotation getRotationFrom(SurveyCoordinateSystem coord) {
        return new Rotation(new Vector3D(coord.u().v()), new Vector3D(coord.v().v()), new Vector3D(getX().v()), new Vector3D(getY().v()));
    }


    public Translation3D getTranslationFrom(SurveyCoordinateSystem coord) {
        return new Translation3D(VecOp.sub(getOrigin(), coord.origin()));
    }
    
    
    public Transform3D getTransformation() {
        // Find the transform between the two frames - use transform classes here (not really needed)
        Translation3D translation = new Translation3D(origin.x(), origin.y(), origin.z());
        Rotation3D rotation = new Rotation3D(
                new BasicHep3Matrix(
                        x.x(),y.x(),z.x(),
                        x.y(),y.y(),z.y(),
                        x.z(),y.z(),z.z()
                        ));
        Transform3D envelopeToSupportTransform = new Transform3D(translation, rotation);
        return envelopeToSupportTransform;
    }
    
    
    public void transform(Transform3D t) {
        Transform3D t_this = getTransformation();
        Hep3Vector v = t_this.getTranslation().getTranslationVector();
        Hep3Vector vrot = t.rotated(v);
        Hep3Vector vrottrans = t.translated(vrot);
        origin = vrottrans;
        rotate(t.getRotation());
    }
    
    public void rotate(IRotation3D r) {
        r.rotate(x);
        r.rotate(y);
        r.rotate(z);
    }
    
}
