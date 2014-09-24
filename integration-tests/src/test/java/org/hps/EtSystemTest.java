package org.hps;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.record.evio.EvioFileProducer;
import org.jlab.coda.et.EtAttachment;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtStationConfig;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.apps.StartEt;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtWakeUpException;
import org.lcsim.util.cache.FileCache;

/**
 * <p>
 * This class runs an ET ring, EVIO file producer, and an ET station in separate system processes,
 * in order to test basic operation of the ET system.
 * </p>
 * <p>
 * Since the test uses a large EVIO test file located on SLAC NFS, it is turned off in the 
 * Maven build when that file system is not available, e.g. on non-SLAC computers.
 * </p> 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */ 
public class EtSystemTest extends TestCase {

    static final String loadPath = new File("../et/lib/Linux-x86_64/").getAbsoluteFile().getAbsolutePath();
    //static final String evioFile = "/nfs/slac/g/hps3/data/testrun/runs/evio/hps_000975.evio.0";
    //static final String evioFile = "/nfs/slac/g/hps3/data/testcase/hps_000975.evio.0";
    static final String fileLocation = "ftp://ftp-hps.slac.stanford.edu/hps/hps_data/hps_java_test_case_data/EtSystemTest.evio";
    static final String classPath = System.getProperty("java.class.path");
    static final String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";        
    static final String etBuffer = "ETBuffer";
    static final int port = 11111;
    static final int waitTime = 5000000; /* Wait time in microseconds. */
    static final int chunkSize = 1;
    static List<Process> processes = new ArrayList<Process>();
    static final int minimumEventsExpected = 5000; 
                       
    /**
     * This test will start the ET ring, attach a station to it, and then stream an EVIO
     * file onto the ring.  Each of these tasks is done on a separate operating system
     * process.
     */
    public void testEtSystem() throws Exception {
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
                
        // Add shutdown hook to cleanup processes in case test case is interrupted.
        Runtime.getRuntime().addShutdownHook(new ProcessCleanupThread());
                        
        // Delete preexisting ET buffer if necessary.
        File etBufferFile = new File(etBuffer);
        if (etBufferFile.exists())
            etBufferFile.delete();
        
        // Start the ET ring.
        Process etProcess = execEt();
        processes.add(etProcess);
        
        // Wait 1 second for ET ring to start up.
        try {
            Thread currentThread = Thread.currentThread();
            synchronized(currentThread) {
                currentThread.wait(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }        
        
        // Attach a station to the ET ring which will receive events.
        Process etStationProcess = execStation();
        processes.add(etStationProcess);
        
        // Start the file producer.
        Process fileProducerProcess = execFileProducer(inputFile);
        processes.add(fileProducerProcess);
                
        // Wait for the file producer to finish.
        int producerReturnCode = 0;
        try {
            producerReturnCode = fileProducerProcess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals("The file producer process returned a non-zero exit status.", 0, producerReturnCode);
        
        // Wait 10 seconds for the event queue to drain.
        try {
            Thread currentThread = Thread.currentThread();
            synchronized(currentThread) {
                currentThread.wait(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }               
        
        // Kill the ET ring process.
        etProcess.destroy();
        
        // Now wait for the station process to die from the ET ring going down which will cause an EOFException.
        //int stationProcessReturnCode = 0;
        try {
            //stationProcessReturnCode = 
            etStationProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        //System.out.println("state")
        
        //assertEquals("The station process returned a non-zero exit status.", 0, stationProcessReturnCode);
        
        // Clear the list of active processes.
        processes.clear();
    }
    
    /**
     * Execute the ET ring process.
     * @return The <tt>Process</tt> that was created by <tt>ProcessBuilder</tt>.
     */
    Process execEt() {               
        ProcessBuilder processBuilder = new ProcessBuilder(                
                javaPath,
                "-Xmx1024m",
                "-cp",
                classPath,
                StartEt.class.getName(),
                "-f",
                etBuffer,
                "-s",
                "10000",
                "-v");        
        return startProcess(processBuilder);
    }     
    
    /**
     * Execute the EVIO file producer process.
     * @return The <tt>Process</tt> that was created by <tt>ProcessBuilder</tt>.
     */
    Process execFileProducer(File file) {        
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                javaPath,
                "-Xmx1024m",
                "-cp",
                classPath,                
                EvioFileProducer.class.getName(),                
                "-e",
                file.getPath(),
                "-f",
                etBuffer,
                "-host",
                "localhost",
                "-s",
                "10000");
        return startProcess(processBuilder);
    }
    
    /**
     * Execute the ET station process.
     * @return The <tt>Process</tt> that was created by <tt>ProcessBuilder</tt>.
     */
    Process execStation() {                
        ProcessBuilder processBuilder = new ProcessBuilder(
                javaPath,
                "-Xmx1024m",
                "-cp",
                classPath,
                getClass().getName());
        return startProcess(processBuilder);
    }
    
    /**
     * Start a Process from the ProcessBuilder.
     * @param processBuilder The ProcessBuilder.
     * @return The Process that was started.
     */
    Process startProcess(ProcessBuilder processBuilder) {
        System.out.println("Starting process ...");
        for (String commandPart : processBuilder.command()) {
            System.out.print(commandPart + " ");
        }
        System.out.println();
        processBuilder.environment().put("LD_LIBRARY_PATH", loadPath);                
        processBuilder.inheritIO();        
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException();
        }                              
        return process;
    }
                    
    /**
     * This will create an ET station and attachment to receive events from the ET ring.
     * @param args The command line arguments (which are not used at all).
     */
    public static void main(String[] args) {

        EtAttachment att;
        EtSystem sys;
        
        try {
        
            // make a direct connection to ET system's tcp server
            EtSystemOpenConfig config = new EtSystemOpenConfig(etBuffer, InetAddress.getLocalHost().getHostName(), 11111);

            // create ET system object with verbose debugging output
            sys = new EtSystem(config, EtConstants.debugInfo);
            sys.open();

            // configuration of a new station
            EtStationConfig statConfig = new EtStationConfig();
            statConfig.setFlowMode(EtConstants.stationSerial);
            statConfig.setBlockMode(EtConstants.stationNonBlocking);
            
            // Create the station.
            EtStation stat = sys.createStation(statConfig, "MY_STATION", 1, 0);

            // attach to new station
            att = sys.attach(stat);
                                   
        } catch (Exception e) {
            throw new RuntimeException(e);
        }            
        
        int eventsReceived = 0;
                
        while (true) {
            try {
                EtEvent[] events = sys.getEvents(
                        att, 
                        Mode.TIMED,
                        Modify.NOTHING,
                        waitTime,
                        chunkSize);
                if (events == null || events.length == 0) {
                    System.out.println("null or 0 length event array!");
                    throw new RuntimeException("Bad event array.");
                } else {
                    eventsReceived += events.length;
                }
            } catch (EtTimeoutException e) {
                //System.out.println("Caught timeout but will try again.");
                //e.printStackTrace();
                //continue;
                throw new RuntimeException("Timed out.", e);
            } catch (EOFException e) {
                System.out.println("Caught end of file exception.  Probably ET ring went down!");
                e.printStackTrace();
                break;
            } catch (IOException | EtException | EtDeadException | EtEmptyException | EtBusyException | EtWakeUpException e) {
                System.out.println("Caught specific Exception of type " + e.getClass().getCanonicalName() + ".");
                e.printStackTrace();                
                throw new RuntimeException(e);
            } catch (Exception e) {
                System.out.println("Caught generic Exception of type " + e.getClass().getCanonicalName() + ".");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }    
        
        System.out.println("received " + eventsReceived + " events total");
        assertTrue("Did not receive enough ET events in session.", eventsReceived > minimumEventsExpected);
    }  
    
    /**
     * Thread to cleanup spawned processes, e.g. if test was interrupted with Ctrl + C.
     * It is registered with the runtime as a shutdown hook.
     */
    static class ProcessCleanupThread extends Thread {
        public void run() {
            for (Process process : processes) {
                System.out.println("cleaning up process " + process.toString() + " ...");
                process.destroy();
            }
        }
    }
}
