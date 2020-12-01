package org.hps.online.example;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

public class RemoteAidaOptions {

    String host = null;
    Integer port = 2001;
    String serverName = "RmiAidaServer";

    String name = "";

    Options options = new Options();

    RemoteAidaOptions(String name) {
        this.name = name;
        options.addOption(new Option("h", "help", false, "Print help and exit"));
        options.addOption(new Option("p", "port", true, "Network port of server"));
        options.addOption(new Option("s", "server", true, "Name of RMI server"));
        options.addOption(new Option("H", "host", true, "Host name or IP address of server"));
    }

    CommandLine parse(String[] args) {

        Parser parser = new BasicParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (cl.hasOption("h")) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("r", "", options, "", true);
            System.exit(0);
        }

        if (cl.hasOption("H")) {
            host = cl.getOptionValue("H");
        } else {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        if (cl.hasOption("p")) {
            port = Integer.parseInt(cl.getOptionValue("p"));
        }
        if (cl.hasOption("s")) {
            serverName = cl.getOptionValue("s");
        }
        System.out.println("host: " + host);
        System.out.println("port: " + port);
        System.out.println("server: " + serverName);

        return cl;
    }

    String getTreeBindName() {
        return "//"+host+":"+port+"/"+serverName;
    }
}
