package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hps.online.recon.commands.CommandFactory;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client for interacting with the online reconstruction server.
 */
public final class Client {

    /**
     * Package logger.
     */
    private static Logger LOGGER = Logger.getLogger(Client.class.getPackageName());
    
    /**
     * Hostname of the server with default.
     */
    private String hostname = "localhost";
    
    /**
     * Port of the server with default from server.
     */
    private int port = Server.DEFAULT_PORT;

    /**
     * Output file for writing server responses.
     * By default it is null, which results in output being written to the console (System.out).
     */
    private File outputFile;
    
    /**
     * Parser for base options.
     */
    private CommandLineParser parser = new DefaultParser();
           
    /**
     * Append rather than overwrite if writing to output file.
     */
    private boolean append = false;
    
    
    /**
     * Run interactive console after command file.
     */
    private boolean interactive = false;
    
    /**
     * The factory for creating <code>Command</code> objects.
     */
    private CommandFactory cf = new CommandFactory();
    
    /**
     * Writer for file output.
     * If null output is written to System.out.
     */
    PrintWriter pw = null;
    
    /**
     * The base options (commands have their own Options objects).
     */
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("", "help", false, "print help"));
        OPTIONS.addOption(new Option("p", "port", true, "server port"));
        OPTIONS.addOption(new Option("h", "host", true, "server hostname"));
        OPTIONS.addOption(new Option("o", "output", true, "output file (default writes server responses to System.out)"));
        OPTIONS.addOption(new Option("a", "append", false, "append if writing to output file (default will overwrite)"));
        OPTIONS.addOption(new Option("i", "interactive", false, "start interactive console after executing command file"));
    }
   
    /**
     * Class constructor.
     */
    Client() { 
    }
    
    /**
     * Print the base command usage.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        final String commands = String.join(" ", cf.getCommandNames());
        help.printHelp(80, "Client [options] [[file] | [command] [command_options]]", "Send commands to the online reconstruction server",
                OPTIONS, "Commands: " + commands + '\n'
                    + "Use 'Client [command] --help' for information about a specific command." + '\n'
                    + "Run with no client arguments to start the interactive console." + '\n'
                    + "Provide a file with commands as a single argument to execute it.");
    }
                   
    /**
     * Run the client using command line arguments
     * @param args The command line arguments
     */
    void run(String args[]) {
        
        // Parse base options.
        CommandLine cl;
        try {
            cl = this.parser.parse(OPTIONS, args, true);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing arguments", e);
        }

        // Print usage and exit.
        if (cl.hasOption("help")) {
            this.printUsage();
            System.exit(0);
        }

        // Get extra arg list.
        List<String> argList = cl.getArgList();

        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
            LOGGER.config("Port: " + this.port);
        }

        if (cl.hasOption("h")) {
            this.hostname = cl.getOptionValue("h");
            LOGGER.config("Hostname: " + this.hostname);
        }

        if (cl.hasOption("o")) {
            this.outputFile = new File(cl.getOptionValue("o"));
            LOGGER.config("Output file: " + this.outputFile.getPath());
        }
        
        if (cl.hasOption("a")) {
            this.append = true;
            LOGGER.config("Appending to output file: " + this.append);
        }
        
        if (cl.hasOption("i")) {
            this.interactive = true;
            LOGGER.config("Interactive mode enable: " + this.interactive);
        }

        // If extra arguments are provided then try to run a command.
        if (argList.size() != 0) {
            
            // See if a command was provided.
            String commandName = argList.get(0);
            Command command = cf.create(commandName);

            // There was a valid command to execute.
            if (command != null) {

                // Remove command from arg list.
                argList.remove(0);

                // Convert command list to array.
                String[] argArr = argList.toArray(new String[0]);

                // Parse command options.
                DefaultParser commandParser = new DefaultParser();
                CommandLine cmdResult = null;
                try {
                    cmdResult = commandParser.parse(command.getOptions(), argArr);

                    // Print usage of the command and exit.
                    if (cmdResult.hasOption("help")) {
                        command.printUsage();
                        System.exit(0);
                    }
                } catch (ParseException e) {
                    command.printUsage();
                    throw new RuntimeException("Error parsing command options", e);
                }

                // Setup the command parameters from the parsed options.
                command.process(cmdResult);

                // Send the command to server.
                LOGGER.info("Sending command " + command.toString());
                send(command);
            } else {
                // If there is a single argument, see if it looks like a command file to execute.
                File execFile = new File(argList.get(0));
                if (argList.size() == 1 && execFile.exists()) {
                    Console cn = new Console(this);
                    try {
                        cn.setEcho(true);
                        cn.execFile(execFile);
                        if (this.interactive) {
                            cn.setEcho(false);
                            cn.run();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error executing command file: " + execFile.getPath(), e);
                    }
                } else {
                    // Could not parse command line options.
                    printUsage();
                    throw new IllegalArgumentException("Unknown command: " + commandName);
                }
            }
        } else {
            // No command was provided so run the interactive console.
            Console cn = new Console(this);
            cn.run();
        }
    }
    
    /**
     * Send a command to the online reconstruction server.
     * @param command The client command to send
     */
    // FIXME: Cleanup handling of PrintWriter etc.
    void send(Command command) {
        
        try {
            if (this.outputFile != null) {
                FileWriter fw = new FileWriter(this.outputFile, this.append);
                this.pw = new PrintWriter(fw);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error opening output file: " + this.outputFile.getPath(), e);
        }
        
        try (Socket socket = new Socket(hostname, port)) {
            // Send command to the server.           
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.write(command.toString() + '\n');            
            writer.flush();

            // Get server response.
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String resp = br.readLine();
            
            if (resp.startsWith("{")) {
                // Handle JSON object.
                printResponse(new JSONObject(resp));
            } else if (resp.startsWith("[")) {
                // Handle JSON array.
                printResponse(new JSONArray(resp));
            } else {
                // Try to read data stream from server. 
                
                LOGGER.info("Reading stream from server");

                // Print first line which was already read.
                printResponse(resp);
                
                // Read data stream from server.
                //Scanner sc = new Scanner(System.in);
                while (true) {                    
                    String line = br.readLine();
                    printResponse(line);
                    /*
                    if (sc.hasNext()) {
                        String userInput = sc.next();
                        if (userInput.equals("q")) {
                            LOGGER.info("Stopping log tail");
                            break;
                        }
                    }
                    */
                }
                //sc.close();
            }
            if (pw != null) {
                LOGGER.info("Wrote server response to: " + this.outputFile.getPath());
                pw.flush();
                pw.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Client error", e);
        }
    }
    
    /**
     * Run the client from the command line.
     * @param args The argument array
     */
    public static void main(String[] args) {
        Client client = new Client();
        client.run(args);
    }
    
    /**
     * Get the hostnae of the server.
     * @return The hostname of the server
     */
    String getHostname() {
        return this.hostname;
    }
    
    /**
     * Get the port number of the server.
     * @return The port number of the server
     */
    int getPort() {
        return this.port;
    }
    
    /**
     * Get the output file for writing server responses.
     * 
     * If this is null then server responses are written to <code>System.out</code>.
     * 
     * @return The output file for writing server responses.
     */
    File getOutputFile() {
        return this.outputFile;
    }
        
    /**
     * Set the port number of the server.
     * @param port The port number of the server
     */
    void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Set the hostname of the server.
     * @param hostname The hostname of the server.
     */
    void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    /**
     * Set the path to the output file for writing server responses.
     * @param outputPath The output file path or null to print to the terminal
     */
    void setOutputFile(String outputPath) {
        if (outputPath != null) {
            this.outputFile = new File(outputPath);
        } else {
            this.outputFile = null;
        }
    }        
    
    /**
     * Set whether to append to the output file.
     * 
     * By default existing output files are overwritten.
     * 
     * @param append True to append to the output file
     */
    void setAppend(boolean append) {
        this.append = append;
    }
    
    /**
     * Get whether to append to the output file.
     * @return Whether to append to the output file
     */
    boolean getAppend() {
        return this.append;
    }
    
    void printResponse(JSONObject jo) {
        if (pw != null) {
            pw.write(jo.toString(4) + '\n');
        } else {
            System.out.println(jo.toString(4));
        }
    }
    
    void printResponse(JSONArray ja) {
        if (pw != null) {
            pw.write(ja.toString(4) + '\n');
        } else {
            System.out.println(ja.toString(4));
        }
    }
    
    void printResponse(String line) {
        if (pw != null) {
            pw.write(line);
        } else {
            System.out.println(line);
        }
    }
}
