package org.lcsim.geometry.compact.converter.lcdd;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.lcsim.util.test.TestUtil.TestOutputFile;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
*
* @author Per Hansson Adrian <phansson@slac.stanford.edu>
*/
public class HPSTracker2014v1SurveyLCDDTest extends TestCase
{    
   public HPSTracker2014v1SurveyLCDDTest(String name)
   {
    super(name);
   }
   
   public static TestSuite suite()
   {
       return new TestSuite(HPSTracker2014v1SurveyLCDDTest.class);
   }
   
   public void test_converter() throws Exception
   {
       InputStream in = HPSTracker2014v1.class.getResourceAsStream("/org/lcsim/geometry/subdetector/HPSTracker2014v1Survey.xml");
       OutputStream out = new BufferedOutputStream(new FileOutputStream(new TestOutputFile("HPSTracker2014v1Survey.lcdd")));
       new Main().convert("HPSTracker2014v1",in,out);
   }
}