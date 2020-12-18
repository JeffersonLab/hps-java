package org.hps.online.recon.example;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import hep.aida.IAnalysisFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.remote.rmi.RmiRemoteUtils;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;

public class RmiList {

    public static void main(String[] args) {

        System.out.println("Locating RMI registry...");
        try {
            Registry registry = LocateRegistry.getRegistry("thinksgiving", RmiRemoteUtils.port);
            String[] l = registry.list();
            System.out.println("RMI list: ");
            for (String s : l) {
                System.out.println("  " + s);
            }
            System.out.println("Located registry!");
        } catch (RemoteException e) {
            e.printStackTrace();
            System.err.println("Failed to locate registry!");
        }
        System.out.println();

        System.out.println("Finding RmiServer with Naming lookup...");
        try {
            String lkp = "//thinksgiving:3001/RmiAidaAgg";
            RmiServer server = (RmiServer) Naming.lookup(lkp);
            if (server != null) {
                System.out.println("Server: " + server.getClass().getCanonicalName());
                System.out.println("Server bind name: " + server.getBindName());
            }
            System.out.println("Found RmiServer!");
            server = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Server: hep.aida.ref.remote.rmi.server.RmiServerImpl_Stub
        // Server bind name: //thinksgiving:3001/RmiAidaAgg
        System.out.println();

        System.out.println("Connecting to remote tree...");
        try {
            boolean clientDuplex = false;
            boolean hurry = false;
            String treeBindName = "//thinksgiving:3001/RmiAidaAgg";
            System.out.println("Connecting to RMI server: " + treeBindName);
            String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
            IAnalysisFactory af = IAnalysisFactory.create();
            ITreeFactory tf = af.createTreeFactory();
            ITree tree = tf.create(treeBindName, RmiStoreFactory.storeType, true, false, options);
            tree.ls("/", true);
            tree.close();
            tree = null;
            System.out.println("Connected to remote tree!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to connect to remote tree!");
        }
        System.out.println("Bye!");
        System.exit(0);

/*
        if (DEBUG) {

            try {
                LOG.info("Testing thinksgiving host lookup...");
                InetAddress addr = InetAddress.getByName("thinksgiving");
                System.out.println("Got host: " + addr.getCanonicalHostName());
                LOG.info("Host lookup of thinksgiving succeeded!");
            } catch (Exception e) {
                LOG.severe("Host lookup of thinksgiving failed!");
            }

            // DEBUG
            try {
                String testTreeBind = "//thinksgiving:3001/RmiAidaAgg";
                System.out.println("Connecting to RMI server: " + treeBindName);
                IAnalysisFactory af = IAnalysisFactory.create();
                ITreeFactory tf = af.createTreeFactory();
                LOG.info("Testing connection to tree: " + testTreeBind);
                ITree tree = tf.create(treeBindName, RmiStoreFactory.storeType, true, false, options);
                LOG.info("Test connection succeeded!");
                System.out.println("Remote tree contents...");
                tree.ls("/", true);
                tree.close();
            } catch (Exception e) {
                LOG.severe("Test connection failed!");
                e.printStackTrace();
            }

            // DEBUG
            System.out.println("Finding RmiServer with Naming lookup...");
            try {
                String lkp = "//thinksgiving:3001/RmiAidaAgg";
                RmiServer server = (RmiServer) Naming.lookup(lkp);
                if (server != null) {
                    System.out.println("Server: " + server.getClass().getCanonicalName());
                    System.out.println("Server bind name: " + server.getBindName());
                }
                System.out.println("Found RmiServer!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
*/
    }
}
