package org.hps.online.recon.remoteaida;

import hep.aida.IAnalysisFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

public class RmiList {

    public static void main(String[] args) {

        /*
        String lkp = "//thinksgiving:3001/RmiAidaAgg";

        try {
            // from command line: java.rmi.activation.port (?)
            int port = RmiRemoteUtils.port;
            System.out.println("Locating registry on port: " + port);
            Registry registry = LocateRegistry.getRegistry(port);
            String[] l = registry.list();
            System.out.println("RMI list: ");
            for (String s : l) {
                System.out.println("  " + s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            RmiServer server = (RmiServer) Naming.lookup(lkp);
            if (server != null) {
                System.out.println("Server: " + server.getClass().getCanonicalName());
                System.out.println("Server bind name: " + server.getBindName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Server: hep.aida.ref.remote.rmi.server.RmiServerImpl_Stub
        // Server bind name: //thinksgiving:3001/RmiAidaAgg
        */

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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
