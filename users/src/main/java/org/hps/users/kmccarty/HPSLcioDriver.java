package org.hps.users.kmccarty;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * Class <code>HPSLcioDriver</code> outputs LCSim runs to a file. It is
 * able to reference HPS classes, and as such, allows data to be output
 * as the original data type.
 * @author Kyle McCarty
 */
public class HPSLcioDriver extends Driver {
	// The path to the output file.
	private String outputFilePath = "default.slcio";
	// Whether empty events should be written.
	private boolean writeEmptyEvents = false;
	// The output file writer.
	LCIOWriter writer = null;
	
	public void startOfData() {
		// Initialize the output writer.
		try {
			File out = new File(outputFilePath);
			writer = new LCIOWriter(out);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// If the output writer is still null, then something went
		// wrong. Terminate the process.
		if(writer == null) {
			System.err.println("Error :: LCIO writer improperly initialized.");
			System.exit(1);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void process(EventHeader event) {
		// If empty events should be written, then the event can be
		// output immediately without further investigation.
		if(writeEmptyEvents) {
			try { writer.write(event); }
			catch(IOException e) {
				System.err.println("Error :: Event can not be written.");
			}
		}
		
		// Otherwise, check to make sure that something exists in the
		// event to output.
		else {
			// Get the lists present in the event.
			Set<List> eventLists = event.getLists();
			
			// Track whether a list exists that is not empty.
			boolean hasData = false;
			
			// Iterate through the lists.
			dataLoop:
			for(List list : eventLists) {
				// If a list is not empty, than there is data and this
				// event is qualified to be written.
				if(list.size() != 0) {
					hasData = true;
					break dataLoop;
				}
			}
			
			// If the event has data, write it. Otherwise, do nothing.
			if(hasData) {
				try { writer.write(event); }
				catch(IOException e) {
					System.err.println("Error :: Event can not be written.");
				}
			}
		}
	}
	
	public void endOfData() {
		// Close the writer.
		try { writer.close(); }
		catch(IOException e) {
			System.err.println("Error closing writer.");
		}
	}
	
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}
	
	public void setWriteEmptyEvents(boolean writeEmptyEvents) {
		this.writeEmptyEvents = writeEmptyEvents;
	}
}
