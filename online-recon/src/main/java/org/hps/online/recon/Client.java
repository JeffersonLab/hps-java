package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
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
    private static Logger LOG = Logger.getLogger(Client.class.getPackage().getName());

    /**
     * Host name of the server
     */
    private String hostName = null;

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
    private PrintWriter pw = null;

    /**
     * Command line arguments for the client
     */
    String[] clientArgs = null;

    /**
     * Arguments for the command to run on the server
     */
    String[] cmdArgs = null;

    /**
     * The client command (which may be null when opening a console or executing a file)
     */
    Command command = null;

    /**
     * The base options (commands have their own Options objects).
     */
    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("h", "help", false, "print help"));
        OPTIONS.addOption(new Option("p", "port", true, "server port"));
        OPTIONS.addOption(new Option("H", "host", true, "server hostname"));
        OPTIONS.addOption(new Option("o", "output", true, "output file (default writes server responses to the console)"));
        OPTIONS.addOption(new Option("a", "append", false, "append if writing to output file (default will overwrite)"));
        OPTIONS.addOption(new Option("i", "interactive", false, "start interactive console after executing a command file"));
    }

    /**
     * Class constructor.
     */
    private Client() {
    }

    /**
     * Print the base command usage.
     */
    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        final String commands = String.join(" ", cf.getCommandNamesSorted());
        help.printHelp(80, "Client [options] [[file] | [command] [command_options]]",
                "Send commands to the online reconstruction server",
                OPTIONS, "Commands: " + commands + '\n'
                    + "Run with no client arguments to start the interactive console." + '\n'
                    + "Provide a file with commands as a single argument to execute it.");
    }

    private void execFile(File file) {
        // If there is a single argument, see if it looks like a command file to execute.
        Console cn = new Console(this);
        try {
            cn.setEcho(true);
            cn.execFile(file);
            if (this.interactive) {
                cn.setEcho(false);
                cn.run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing command file: " + file.getPath(), e);
        }
    }

    /**
     * Setup state from the raw command line arguments
     *
     * @param args The raw command line arguments
     */
    private void setup(String args[]) {
        int cmdIdx = findCommand(args);
        clientArgs = args;
        if (cmdIdx > 0) {
            String commandName = args[cmdIdx];
            command = cf.create(commandName);
            clientArgs = new String[0];
            if (cmdIdx > 0) {
                clientArgs = new String[cmdIdx];
                System.arraycopy(args, 0, clientArgs, 0, cmdIdx);
            } else {
                clientArgs = new String[0];
            }

            cmdArgs = new String[0];
            if (args.length - cmdIdx - 1 > 0) {
                cmdArgs = new String[args.length - cmdIdx];
                System.arraycopy(args, cmdIdx + 1, cmdArgs, 0, args.length - cmdIdx);
            }
        }

        /*
        if (clientArgs.length > 0) {
            List<String> clientArgList = Arrays.asList(clientArgs);
            LOG.info("Client arg list: " + String.join(" ", clientArgList));
        }

        if (cmdArgs != null) {
            List<String> cmdArgList = Arrays.asList(cmdArgs);
            LOG.info("Command arg list: " + String.join(" ", cmdArgList));
        }
        */
    }

    /**
     * Run the client using command line arguments.
     *
     * @param args The command line arguments
     */
    void run(String args[]) {

        // Find the client command to run if provided and set various other state
        setup(args);

        // Parse the client options
        List<String> extraArgs = parseOptions(clientArgs);

        if (command != null) {
            // Run a client command if one was provided

            LOG.info("Executing client command " + command.getName());

            try {
                command.parse(cmdArgs);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            // Setup the command parameters from the parsed options.
            command.process();

            // Send the command to server.
            LOG.info("Sending command " + command.toString());
            send(command);

        } else if (extraArgs.size() > 0) {
            // Execute a file with client commands using the Console

            LOG.info("Extra args: " + String.join(" ", extraArgs));

            execFile(new File(extraArgs.get(0)));
        } else {
            // No command was provided so run the interactive console.
            Console cn = new Console(this);
            cn.run();
        }
    }

    /**
     * Find a client command in the raw arguments
     * @param args
     * @return
     */
    private int findCommand(String[] args) {
        int idx = -1;
        for (int i = 0; i < args.length; i++) {
            if (cf.commandExists(args[i])) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    private List<String> parseOptions(String[] args) {

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

        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
            LOG.config("Port: " + this.port);
        }

        if (cl.hasOption("H")) {
            this.hostName = cl.getOptionValue("H");
            LOG.config("User specified host name: " + this.hostName);
        } else {
            try {
                this.hostName = InetAddress.getLocalHost().getHostName();
                LOG.config("Default host name: " + this.hostName);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        if (cl.hasOption("o")) {
            this.outputFile = new File(cl.getOptionValue("o"));
            LOG.config("Output file: " + this.outputFile.getPath());
        }

        if (cl.hasOption("a")) {
            this.append = true;
            LOG.config("Appending to output file: " + this.append);
        }

        if (cl.hasOption("i")) {
            this.interactive = true;
            LOG.config("Interactive mode enable: " + this.interactive);
        }

        return cl.getArgList();
    }

    /**
     * Send a command to the online reconstruction server.
     * @param command The client command to send
     */
    void send(Command command) {

        // Setup writing to output file.
        try {
            if (this.outputFile != null) {
                FileWriter fw = new FileWriter(this.outputFile, this.append);
                this.pw = new PrintWriter(fw);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error opening output file: " + this.outputFile.getPath(), e);
        }

        LOG.info("Opening connection to server: " + this.hostName + ":" + port);

        // Open socket to server.
        try (final Socket socket = new Socket(this.hostName, port)) {

            // Send command to the server.
            final PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.write(command.toString() + '\n');
            writer.flush();

            // Get server response.
            final InputStream is = socket.getInputStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String resp = br.readLine();
            //LOGGER.info("resp: " + resp);
            if (resp.startsWith("{")) {
                // Print JSON object response.
                printResponse(new JSONObject(resp));
            } else if (resp.startsWith("[")) {
                // Handle JSON array response.
                printResponse(new JSONArray(resp));
            } else {
                // Try to read continuous data stream from server.

                LOG.info("Reading stream from server - press any key to exit");

                // Print first line which was already read.
                printResponse(resp);

                // We block the client thread but that's fine (server streaming log data to us)
                readLoop(br, writer);
            }

            // Close the PrintWriter.
            if (pw != null) {
                System.out.println("Wrote server response to: " + this.outputFile.getPath());
                pw.flush();
                pw.close();
            }

            // Close the socket's BufferedReader.
            try {
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Close the socket's InputStream.
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException("Client error", e);
        }
    }

    /**
     * Read server output until user presses a key to continue
     * @param br The reader for getting server data
     * @param writer The writer for writing response to server
     */
    private void readLoop(final BufferedReader br, final PrintWriter writer) {

        // Read server responses in separate thread
        Thread readThread = new Thread("Client Read Thread") {
            public void run() {
                while (true) {
                    try {
                        if (br.ready()) {
                            String line = br.readLine();
                            printResponse(line);
                        }
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        readThread.start();

        // Wait until user presses any key before continuing
        while(readThread.isAlive() && !readThread.isInterrupted()) {
            try {
                // Any client input breaks the read loop
                if (System.in.available() > 0) {
                    System.err.println("<<<< You pressed the keyboard >>>>");
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        // Kill the read thread
        try {

            readThread.interrupt();
            readThread.join(5000L);
            readThread = null;

            // This tells the server to stop sending log info
            writer.write("STOP" + '\n');
            writer.flush();

        } catch (InterruptedException e) {
            e.printStackTrace();
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
     * Get the hostname of the server.
     * @return The hostname of the server
     */
    String getHostname() {
        return this.hostName;
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
     * If this is <code>null</code> then server responses are written to <code>System.out</code>.
     *
     * @return The output file for writing server responses
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
     * @param hostname The hostname of the server
     */
    void setHostname(String hostname) {
        this.hostName = hostname;
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

    /**
     * Print JSON object to file or <code>System.out</code>.
     * @param jo The JSON object to print out
     */
    private void printResponse(JSONObject jo) {
        if (pw != null) {
            pw.write(jo.toString(4) + '\n');
        } else {
            System.out.println(jo.toString(4));
        }
    }

    /**
     * Print JSON array to file or <code>System.out</code>.
     * @param jo The JSON array to print out
     */
    private void printResponse(JSONArray ja) {
        if (pw != null) {
            pw.write(ja.toString(4) + '\n');
        } else {
            System.out.println(ja.toString(4));
        }
    }

    /**
     * Print server response line to file or <code>System.out</code>.
     * @param jo The line to print out
     */
    private void printResponse(String line) {
        if (pw != null) {
            pw.write(line);
        } else {
            System.out.println(line);
        }
    }
}
