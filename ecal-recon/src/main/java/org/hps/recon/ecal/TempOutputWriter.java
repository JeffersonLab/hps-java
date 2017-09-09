package org.hps.recon.ecal;

import java.io.FileWriter;
import java.io.IOException;

public class TempOutputWriter {
	private final FileWriter writer;
	private static final String FILE_DIR = "C:\\cygwin64\\home\\Kyle\\";
	
	public TempOutputWriter(String filename) {
		try {
			writer = new FileWriter(FILE_DIR + filename);
			writer.write("");
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public void write(String text) {
		try { writer.append(text + "\n"); }
		catch (IOException e) { throw new RuntimeException(); }
	}
	
	public void close() {
		try { writer.close(); }
		catch (IOException e) { throw new RuntimeException(); }
	}
}
