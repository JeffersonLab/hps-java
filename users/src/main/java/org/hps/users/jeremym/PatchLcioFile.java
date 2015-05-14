package org.hps.users.jeremym;

import java.io.File;
import java.io.IOException;

import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;

/**
 * Read in an LCIO file and write it to a new location, which should fix 
 * the SIOCluster issue.  
 */
public final class PatchLcioFile {

    public static void main(String[] args) {
        
        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);

        LCIOReader reader = null;
        LCIOWriter writer = null;
        try {
            System.out.println("opening " + oldFile.getPath() + " for reading");
            reader = new LCIOReader(oldFile);
            
            System.out.println("opening " + newFile.getPath() + " for writing");
            writer = new LCIOWriter(newFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        EventHeader event = null;
        try {
            while ((event = reader.read()) != null) {
                writer.write(event);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        System.out.println("done patching " + oldFile.getPath());
    }
}
