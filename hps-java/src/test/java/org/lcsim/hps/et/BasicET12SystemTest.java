package org.lcsim.hps.et;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Try to...
 * 
 * 1) Start ET Ring.
 * 2) Start monitor.
 * 3) Start producer.
 * 
 * Verify working from printed output!
 * 
 * @author Jeremy McCormick
 * @version $Id: BasicET12SystemTest.java,v 1.1 2012/02/15 17:25:01 jeremy Exp $
 */

public class BasicET12SystemTest
{
    private static final String javaRuntime = "java";
    private static final String ET_FILE_NAME = "ETBuffer";
    private String HOSTNAME = "";
   
    // ps aux | grep "java" | grep "ETBuffer" | awk '{print $2}'

    public void testETSystem() throws Exception
    {        
        // Clobber old buffer file.
        /*
        File et = new File(ET_FILE_NAME);
        if (et.exists()) 
        {
            if (et.canWrite())
            {
                System.out.println("wiping out " + ET_FILE_NAME);
                et.delete();
            }
            else
            {
                throw new RuntimeException("unwritable ET system file found at " + ET_FILE_NAME);
            }
        }
        
        // Get the host name.
        //System.out.println("hostname="+InetAddress.getLocalHost().getHostName());
        HOSTNAME = InetAddress.getLocalHost().getHostName();
        */
        // Start some ET processes.
        Process server = startETServer();
        Process producer = startProducer();
        Process monitor = startMonitor();
        
        // Wait for something to happen.
        Thread.sleep(10000);

        // Tear down system.
        monitor.destroy();
        producer.destroy();
        server.destroy();

        return;
    }

    private String serverClass="org.jlab.coda.et.apps.StartEt";
    private Process startETServer() throws Exception
    {    
        List<String> argList = new ArrayList<String>();
        argList.add(javaRuntime);
        argList.add("-classpath");
        argList.add(System.getProperty("java.class.path"));
        argList.add(serverClass);
        argList.add("-f");
        argList.add(ET_FILE_NAME);
        argList.add("-n");
        argList.add("100");
        argList.add("-s");
        argList.add("1024");
        argList.add("-p");
        argList.add("11111");
        //argList.add("-nd");
        //argList.add("-f");
        //argList.add(ET_FILE_NAME);
        argList.add("-v");
        argList.add("-d");
        printArgList(argList, System.out);
        ProcessBuilder processBuilder = new ProcessBuilder(argList.toArray(new String[argList.size()]));
        processBuilder.directory(new File("."));
        Process p = processBuilder.start();
        //(new ProcessPrinter(p, System.out, serverClass+"::out")).run();
        //(new ProcessPrinter(p, System.out, serverClass+"::err")).run();
        return p;
    }

    //private String evioProducerClass="org.jlab.coda.et.apps.EvioProducer";
    private String evioProducerClass="org.jlab.coda.et.apps.Blaster";
    private Process startProducer() throws IOException
    {
        List<String> argList = new ArrayList<String>();
        argList.add(javaRuntime);
        argList.add("-classpath");
        argList.add(System.getProperty("java.class.path"));
        argList.add(evioProducerClass);
        argList.add("-f");
        argList.add(ET_FILE_NAME);
        //argList.add("-host");
        //argList.add(HOSTNAME);
        printArgList(argList, System.out);
        ProcessBuilder processBuilder = new ProcessBuilder(argList.toArray(new String[argList.size()]));
        processBuilder.directory(new File("."));
        Process producer = processBuilder.start();
        //(new ProcessPrinter(producer, System.out, evioProducerClass + "::out")).run();
        //(new ErrorPrinter(producer, System.out, evioProducerClass + "::err")).run();
        return producer;
    }

    private String evioMonitorClass = "org.jlab.coda.et.apps.EtMonitor";
    //  java EtMonitor -f <et name> [-p <period>] [-port <server port>] [-h <host>]
    private Process startMonitor() throws Exception
    {
        List<String> argList = new ArrayList<String>();
        argList.add(javaRuntime);
        argList.add("-classpath");
        argList.add(System.getProperty("java.class.path"));
        argList.add(evioMonitorClass);
        argList.add("-f");
        argList.add(ET_FILE_NAME);
        argList.add("-p");
        argList.add("1");
        argList.add("-port");
        argList.add("11111");
        argList.add("-h");
        argList.add(HOSTNAME);
        //argList.add("-h");
        //argList.add("255.255.255.255");
        printArgList(argList, System.out);
        ProcessBuilder processBuilder = new ProcessBuilder(argList.toArray(new String[argList.size()]));
        processBuilder.directory(new File("."));
        Process monitor = processBuilder.start();
        (new ProcessPrinter(monitor, System.out, evioMonitorClass + "::out")).run();
        (new ErrorPrinter(monitor, System.out, evioMonitorClass + "::err")).run();
        return monitor;
    }
    
    private static void printArgList(List<String> argList, PrintStream ps)
    {
        for (int i=0; i<argList.size(); i++)
        {
            ps.print(argList.get(i) + " ");
        }
        ps.println();
    }
    
    class ProcessPrinter implements Runnable
    {
        Process p;
        String prepend = "";
        PrintStream ps;
        InputStream is;

        /**
         * @param p Process to print.
         * @param is InputStream to print, either err or out of the Process.
         * @param ps The output print stream.
         * @param prepend A String to prepend to printed messages.
         */
        ProcessPrinter(Process p, PrintStream ps, String prepend)
        {
            this.p = p;
            this.is = p.getInputStream();
            this.ps = ps;
            this.prepend = prepend;
        }

        private void println(String msg)
        {
            ps.println(prepend + " - " + msg);
        }

        public void run()
        {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(this.is));

            try
            {
                while ((line = br.readLine()) != null)
                    println(line);
            }
            catch (IOException x)
            {
                throw new RuntimeException(x);
            }
            println("EOF");
            ps.flush();
            try 
            {
                p.waitFor();  // wait for process to complete
            } 
            catch (InterruptedException e)
            {
                System.err.println(e);  // "Can't Happen"
                return;
            }
            println("exit " + p.exitValue());
        }

    }    

    class ErrorPrinter extends ProcessPrinter
    {
        ErrorPrinter(Process p, PrintStream ps, String prepend)
        {
            super(p, ps, prepend);
            this.is = p.getErrorStream();
        }
    }    
}

