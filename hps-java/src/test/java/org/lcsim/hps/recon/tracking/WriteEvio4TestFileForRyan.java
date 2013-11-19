package org.lcsim.hps.recon.tracking;

import junit.framework.TestCase;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.BaseStructureHeader;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

public class WriteEvio4TestFileForRyan extends TestCase {
	
	static final String fname = "Evio4TestFileRyan.evio";
	
	public void testIt() throws Exception {
		
		// Make an example event with dummy data.
		EventBuilder builder = new EventBuilder(123, DataType.BANK, 1); // tag=123, dataType=bank, number=1
		int tag = 100;
		int number = 1;
		for (int i=0; i<8; i++) {
			EvioBank bank = new EvioBank(tag, DataType.INT32, number);
			bank.appendIntData(new int[] {1,2,3,4});
			bank.setAllHeaderLengths();
			builder.addChild(builder.getEvent(), bank);
			tag -= 1;
			number += 1;
		}
		
		// Write this EVIO event.
        builder.setAllHeaderLengths();
        EventWriter writer = new EventWriter(fname);
        writer.writeEvent(builder.getEvent());
        writer.close();
        
        // Read back the file and make test assertions.
        EvioReader reader = new EvioReader(fname);
        EvioEvent event = reader.parseNextEvent();
        BaseStructureHeader eventHeader = event.getHeader();        
        assertEquals(eventHeader.getTag(), 123);
        assertEquals(eventHeader.getDataType(), DataType.BANK);
        assertEquals(eventHeader.getNumber(), 1);
        tag = 100;
        number = 1;
        for (int i=0; i<8; i++) {
        	BaseStructure bank = event.getChildren().get(i);
        	BaseStructureHeader bankHeader = bank.getHeader();
        	assertEquals(bankHeader.getTag(), tag);
        	assertEquals(bankHeader.getNumber(), number);
        	assertEquals(bankHeader.getDataType(), DataType.INT32);
        	int[] intData = bank.getIntData();
        	assertEquals(intData.length, 4);
        	assertEquals(intData[0], 1);
        	assertEquals(intData[1], 2);
        	assertEquals(intData[2], 3);
        	assertEquals(intData[3], 4);
        	tag -= 1;
        	number +=1;
        }
	}
}