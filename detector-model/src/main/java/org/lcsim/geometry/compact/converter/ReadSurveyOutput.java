/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hps.util.BasicLogFormatter;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBox;
import org.lcsim.util.log.LogUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class ReadSurveyOutput {

    private static Logger logger = LogUtil.create(ReadSurveyOutput.class, new BasicLogFormatter(),Level.INFO);
    
    List<CSVRecord> records;
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        FileReader reader;
        try {
            reader = new FileReader(args[0]);
        } catch (FileNotFoundException e) {
              throw new RuntimeException("cannot upen file " + args[0], e);
        }
        
        ReadSurveyOutput survey = new ReadSurveyOutput();
        
        
        try {
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
            survey.records = parser.getRecords();
        } catch (IOException e) {
            throw new RuntimeException("cannot parse file " + args[0], e);
        }
        
        logger.info("Got " + Integer.toString(survey.records.size()));
        
        
        
        survey.printOrigin("M_BOT13");
        
        
        survey.printOrigin("M_TOP13");
  
        
       
        
    }
    
    
    
    
    private void printBallPositions(String name) {
        for (CSVRecord record: records) {
            logger.fine(record.toString());
            if(Pattern.matches("^M_[A-Z]*13([1-4]$)", record.get(0))) {
                Hep3Vector v = getBallPosition(record);
                logger.info(v.toString());
            }
        }
    }
    
    
    private void printOrigin(String name ) {
        Map<Integer, Hep3Vector> mtop = getBallPositions(name);
        
        Hep3Vector dT = VecOp.sub(mtop.get(1),mtop.get(3));
        Hep3Vector orgT = VecOp.add(mtop.get(3), VecOp.mult(0.5, dT));
        logger.info("Type: " + name);
        logger.info("mtop " + mtop.get(1).toString() + " " + mtop.get(3).toString());
        logger.info("dT " + dT.toString());
        logger.info("orgT " + orgT.toString());
        //translate to center of SVT box
        Hep3Vector orgTbox = VecOp.sub(orgT, new BasicHep3Vector(0, 0, 0.375*HPSTrackerGeometryDefinition.inch));
        logger.info("orgTbox " + orgTbox.toString());

        orgTbox = VecOp.sub(orgTbox, new BasicHep3Vector(0, 0, HPSTracker2014GeometryDefinition.SvtBox.length/2.0));
        logger.info("orgTbox " + orgTbox.toString());
    }
   
    
    private Map<Integer, Hep3Vector> getBallPositions(String name) {
        Map<Integer, Hep3Vector> map = new HashMap<Integer, Hep3Vector>();
        Pattern pattern = Pattern.compile("^"+name+"([1-4]$)");
        Matcher matcher;
        for (CSVRecord record : records) {
            logger.fine( record.toString());
            matcher = pattern.matcher(record.get(0));
            if(matcher.matches()) {
                Hep3Vector v = getBallPosition(record);
                map.put(Integer.parseInt(matcher.group(1)), v);
            }
        }
        return map;
    }
    
    
    private Hep3Vector getBallPosition(CSVRecord record) {
       
        if(record.size()<8) {
            logger.warning("record has weird format? " + record.toString());
        }
        Hep3Vector v = new BasicHep3Vector(Double.parseDouble(record.get(5)), Double.parseDouble(record.get(6)), Double.parseDouble(record.get(7)));
        return v;
        
    }
    

}
