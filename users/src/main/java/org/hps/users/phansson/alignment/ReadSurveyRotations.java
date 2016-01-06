/**
 * 
 */
package org.hps.users.phansson.alignment;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class ReadSurveyRotations {

    final static Logger LOGGER = Logger.getLogger(ReadSurveyRotations.class.getSimpleName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
    String name;
    String parent;
    Hep3Vector u;
    Hep3Vector v;
    Hep3Vector w;
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        
        List<ReadSurveyRotations> rotSurvey = getRotations(args[0]);
        LOGGER.info("Found " + rotSurvey.size() + " survey rotations");
        
        List<ReadSurveyRotations> rotIdeal = getRotations(args[1]);
        LOGGER.info("Found " + rotIdeal.size() + " ideal rotations");
        
        
        
        for (ReadSurveyRotations r : rotIdeal) {
            for (ReadSurveyRotations rs : rotSurvey) {
                if(rs.equals(r)) {
                    Rotation rotation = new Rotation(get3DVector(r.u), get3DVector(r.v), get3DVector(rs.u), get3DVector(rs.v));
                    //logger.fine(r.name + " in " + r.parent + " u " + r.u.toString() + " v " + r.v.toString() + " v " + r.w.toString());
                    if(args.length>3) {
                        if(!Pattern.matches(args[2], r.name) || !Pattern.matches(args[3], r.parent)) {
                            continue;
                        }
                    }
                    StringBuffer sb = new StringBuffer();
                    sb.append( String.format("%s in %s\n", r.name,r.parent) );
                    sb.append( String.format("u: %s -> %s\n", r.u.toString(),rs.u.toString()) );
                    sb.append( String.format("v: %s -> %s\n", r.v.toString(),rs.v.toString()) );
                    sb.append( String.format("w: %s -> %s\n", r.w.toString(),rs.w.toString()) );
                    sb.append( String.format("w: %s -> %s\n", r.w.toString(),rs.w.toString()) );
                    double cardanAngles[] = rotation.getAngles(RotationOrder.XYZ);
                    sb.append( String.format("%s in %s Cardan XYZ: %f,%f,%f\n", r.name, r.parent, cardanAngles[0],cardanAngles[1],cardanAngles[2]) );
                    System.out.printf("%s\n", sb.toString());
                } 
            }   
        }
        
        
        
        
        
    }
    
    
    private static Vector3D get3DVector(Hep3Vector x) {
        return new Vector3D(x.x(), x.y(), x.z());
    }
    
    public boolean equals(ReadSurveyRotations other) {
        return other==null ? false : this.name.equals(other.name) && this.parent.equals(other.parent);
    }
    
    
    private static double convertDouble(String str) {
        return new BigDecimal(str).doubleValue();
    }
    
    private static Hep3Vector getVector(String str) {
        Pattern p = Pattern.compile("\\s*(\\S+),\\s*(\\S+),\\s*(\\S+).*");
        Matcher matcher = p.matcher(str);
        if(!matcher.matches()) {
            throw new RuntimeException("cannot match vector to string \"" + str + "\"");
        }
        return new BasicHep3Vector(convertDouble(matcher.group(1)), convertDouble(matcher.group(2)), convertDouble(matcher.group(3)));
        
    }
    
    
    
    public ReadSurveyRotations(String name, String parent, Hep3Vector u, Hep3Vector v, Hep3Vector w) {
        this.name = name;
        this.parent = parent;
        this.u = u;
        this.v = v;
        this.w = w;
    }
    
    
    private static List<ReadSurveyRotations> getRotations(String fileName) {
        
        List<ReadSurveyRotations> list = new ArrayList<ReadSurveyRotations>();
        
        
        Pattern pattern = Pattern.compile("(.*)\\sorigin\\sin\\s(\\S+)\\s.*\\(mm\\)\\su.*\\[(.*)\\]\\sv.*\\[(.*)\\]\\sw.*\\[(.*)\\].*");
        //Pattern.compile(".*mm.*u\s[(.*)].*v\s[(.*)].*w\s[(.*)]");
        Matcher matcher;
        File file = new File(fileName);
        //File outputFile = new File(fileName+".rot");
        try {
            
            //FileWriter writer = new FileWriter(outputFile);
            //BufferedWriter bWriter = new BufferedWriter(writer);
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
  
            String line;
            try {
                while((line = bReader.readLine()) != null) {
                    //logger.info(line);
                    matcher = pattern.matcher(line);
                    if(matcher.matches()) {
                        String name = matcher.group(1);
                        String parent = matcher.group(2);
                        //logger.info("matched " + name + " g1 " + matcher.group(3));
                        Hep3Vector u = getVector(matcher.group(3));
                        Hep3Vector v = getVector(matcher.group(4));
                        Hep3Vector w = getVector(matcher.group(5));
                        if(Math.abs(u.magnitude()-1.0)>0.0001 || Math.abs(v.magnitude()-1.0)>0.0001 ||Math.abs(w.magnitude()-1.0)>0.0001 ) {
                            LOGGER.warning(line);
                            LOGGER.warning("name: " + name + " unit vectors: " + u.toString() + " " + v.toString() + " " + w.toString());
                            throw new RuntimeException("error reading vectors");
                        }
                        ReadSurveyRotations rot = new ReadSurveyRotations(name, parent, u, v, w);
                        list.add(rot);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                bReader.close();
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            
            
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        return list;
        
        
    }
    


}
