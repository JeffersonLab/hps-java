package org.hps.online.recon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

/**
 * Interactive console for online reconstruction client.
 * 
 * @author jeremym
 */
public class Console {

    private final Client client;
    private final Set<String> commands;
    private final Map<String, ClientCommand> commandMap;
    
    Console(Client client) {
        this.client = client;
        this.commands = client.getCommands();
        this.commandMap = client.getCommandMap();
    }
         
    public void run() {
        String userInput;    
        
        Scanner sn = new Scanner(System.in);

        System.out.println("HPS Online Reconstruction");
        System.out.println("Type 'help' or 'help [command]' for more information or 'exit' to quit.");
        
        while (true) {
            System.out.print("online> ");
            userInput = sn.nextLine();
            userInput = userInput.trim();
            //System.out.println("userInput: " + userInput);
            if (userInput.length() > 0) {
                String rawInputArr[] = userInput.split(" ");
                String cmdStr = rawInputArr[0];
                List<String> args = new ArrayList<String>(Arrays.asList(rawInputArr));
                //System.out.println("args: " + args);
                if (args.size() > 0) {
                    args.remove(0);
                }
                //System.out.println("cmd args: " + args);
                if (cmdStr.equals("exit")) {
                    break;
                } else if (cmdStr.equals("help")) {
                    if (args.size() == 0) {
                        printHelp();
                    } else {                        
                        printCommandHelp(args.get(0));
                    }
                } else if (cmdStr.equals("port")) {
                    if (args.size() == 0) {
                        System.out.println(client.getPort());
                    } else {
                        try {
                            Integer port = Integer.parseInt(args.get(0));
                            client.setPort(port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (cmdStr.equals("host")) {
                    if (args.size() == 0) {
                        System.out.println(client.getHostname());
                    } else {
                        try {
                            String hostname = args.get(0);
                            client.setHostname(hostname);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (cmdStr.equals("file")) {
                    if (args.size() == 0) {
                        if (client.getOutputFile() != null) {
                            System.out.println(client.getOutputFile().toPath());
                        } else {
                            System.out.println("Output is being written to the terminal.");
                        }
                    } else {
                        String filename = args.get(0);                        
                        client.setOutputFile(filename);                        
                    }
                } else if (cmdStr.equals("terminal")) {
                    if (client.getOutputFile() != null) {
                        client.setOutputFile(null);
                        System.out.println("Redirected output back to the terminal.");
                    }
                } else {
                    if (this.commands.contains(cmdStr)) {
                        ClientCommand cmd = this.commandMap.get(cmdStr);
                        DefaultParser parser = new DefaultParser();
                        try {
                            String cmdArr[] = args.toArray(new String[0]);
                            CommandLine cl = parser.parse(cmd.getOptions(), cmdArr);
                            try {
                                cmd.process(cl);
                                client.send(cmd);
                                cmd.reset();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Unknown client commnd: " + cmdStr);
                    }
                }
            }
        }
        
        sn.close();
    }
    
    void printHelp() {
        System.out.println('\n' + "GENERAL" + '\n');
        System.out.println("    help - print general information");
        System.out.println("    help [cmd] - print information for specific command");
        System.out.println("    exit - quit the console");
        System.out.println('\n' + "SETTINGS" + '\n');        
        System.out.println("    port [port] - set the server port");
        System.out.println("    host [host]- set the server hostname");
        System.out.println("    file [filename] - write server output to a file");
        System.out.println("    terminal - redirect server output back to the terminal");
        System.out.println('\n' + "COMMANDS" + '\n');
        for (Entry<String, ClientCommand> entry : this.commandMap.entrySet()) {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue().getDescription());
        }
    }
    
    void printCommandHelp(String command) {
        if (this.commands.contains(command)) {
            this.commandMap.get(command).printUsage();        
        } else {
            System.err.println("Unknown command: " + command);
        }
    }
}
