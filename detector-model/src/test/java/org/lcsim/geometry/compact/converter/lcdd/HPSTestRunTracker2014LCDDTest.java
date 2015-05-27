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
public class HPSTestRunTracker2014LCDDTest extends TestCase
{    
   public HPSTestRunTracker2014LCDDTest(String name)
   {
   	super(name);
   }
   
   public static TestSuite suite()
   {
       return new TestSuite(HPSTestRunTracker2014LCDDTest.class);
   }
   
   public void test_converter() throws Exception
   {
       InputStream in = HPSTestRunTracker2014.class.getResourceAsStream("/org/lcsim/geometry/subdetector/HPSTestRunTracker2014.xml");
       OutputStream out = new BufferedOutputStream(new FileOutputStream(new TestOutputFile("HPSTestRunTracker2014.lcdd")));
       new Main().convert("HPSTestRunTracker2014",in,out);
   }
}