package org.hps.recon.skims;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOWriter;

/* ****************************************
   
 */
abstract class Skimmer{
    private LCIOWriter writer;
    private Set<String> listIgnore = new HashSet<String>();
    private Set<String> listKeep = new HashSet<String>();
    private File outputFile;
    private int nprocessed = 0;
    private int npassed = 0;
    
    abstract boolean passSelection(EventHeader event);
    abstract void setParameters(String parsFileName);    

    //methods below are taken from lcsim LCIODriver and shouldn't need to be touched
    public Skimmer(String file) {
        this(addFileExtension(file), null);
    }

    public Skimmer(String file, Collection<String> listIgnore) {
        this(new File(addFileExtension(file)), listIgnore);
    }

    public Skimmer(File file, Collection<String> listIgnore) {
        this.outputFile = file;
        if (listIgnore != null) {
            this.listIgnore.addAll(listIgnore);
        }
	setupWriter();
    }

    public Skimmer(){
    }

    void writeEvent(EventHeader event){
	try {
            writer.write(event);
        } catch (IOException x) {
            throw new RuntimeException("Error writing LCIO file", x);
        }
    }
        
    public void setOutputFilePath(String filePath) {
        outputFile = new File(addFileExtension(filePath));
    }

    public void setIgnoreCollections(String[] ignoreCollections) {
        listIgnore.addAll(Arrays.asList(ignoreCollections));
    }

    public void setWriteOnlyCollections(String[] keepCollections) {
        listKeep.addAll(Arrays.asList(keepCollections));
    }

    public void setIgnoreCollection(String ignoreCollection) {
        listIgnore.add(ignoreCollection);
    }

    public void setWriteOnlyCollection(String writeOnlyCollection) {
        listKeep.add(writeOnlyCollection);
    }

    private void setupWriter() {
        // Cleanup existing writer.
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
            }
        }

        // Setup new writer.
        try {
            writer = new LCIOWriter(outputFile);
        } catch (IOException x) {
            throw new RuntimeException("Error creating writer", x);
        }
        writer.addAllIgnore(listIgnore);
        writer.addAllWriteOnly(listKeep);

        try {
            writer.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    private static String addFileExtension(String filePath) {
        if (!filePath.endsWith(".slcio")) {
            return filePath + ".slcio";
        } else
            return filePath;
    }
    ///////////////   done stealing from LCIODriver   /////////////    
    public void incrementEventProcessed() {
        nprocessed++;
    }

    public void incrementEventPassed() {
        npassed++;
    }
    
    public int getNProcessed(){	
	return nprocessed; 
    }
    
    public int getNPassed(){	
	return npassed; 
    }

    public double getPassFraction(){
	return ((double)npassed)/nprocessed;
    }
}
