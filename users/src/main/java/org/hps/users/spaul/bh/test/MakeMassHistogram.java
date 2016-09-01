package org.hps.users.spaul.bh.test;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hps.conditions.ConditionsDriver;
import org.hps.users.spaul.feecc.CustomBinning;
import org.hps.users.spaul.feecc.RemoveDuplicateParticles;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.lcio.LCIOReader;

public class MakeMassHistogram {

    static String vertexCollectionName = "TargetConstrainedV0Vertices";
    static boolean display = false;
    static CustomBinning cb;
    public static void main(String arg[]) throws IllegalArgumentException, IOException{


        String input = arg[0];
        String output = arg[1];

        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(output,"xml",false,true);
        IHistogramFactory hf = af.createHistogramFactory(tree);
        setupHistograms(tree, hf);

        ConditionsDriver hack = new ConditionsDriver();
        //hack.setXmlConfigResource("/u/group/hps/hps_soft/detector-data/detectors/HPS-EngRun2015-Nominal-v3");
        hack.setDetectorName("HPS-PhysicsRun2016-2pt2-v0");
        hack.setFreeze(true);
        hack.setRunNumber(Integer.parseInt(arg[2]));
        hack.initialize();
        File file = new File(input);
        File inputs[] = new File[]{file};
        if(file.isDirectory()){
            inputs = file.listFiles();
        }
        for(int i = 0; i< inputs.length; i++){
            LCIOReader reader = new LCIOReader(inputs[i]);
            //reader.open(input);
            //reader.
            EventHeader event = reader.read();
            int nEvents = 0;
            try{
                outer : while(event != null){
                    processEvent(event);

                    //System.out.println(Q2);

                    event = reader.read();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        tree.commit();
        tree.close();

    }





    static IHistogram1D h1;


    static void setupHistograms(ITree tree, IHistogramFactory hf){
        //h1 = hf.createHistogram2D("px\\/pz vs py\\/pz", 160, -.16, .24, 160, -.2, .2);

        // 1 MeV. bins. 
        double maxMass = .4;
        double massBin = .001;
        h1 = hf.createHistogram1D("recon mass", (int) (maxMass/massBin), 0, maxMass);


    }

    private static void processEvent(EventHeader event) {
        if(event.getEventNumber() %10000 == 0)
            System.out.println("event number " + event.getEventNumber());




        List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "FinalStateParticles");
        particles = RemoveDuplicateParticles.removeDuplicateParticles(particles);
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);

        //System.out.println(mA);
        for(Vertex v : vertices){

            if(Cuts.passesCuts(v))
                h1.fill(InvariantMassCalculator.getMass(v));
        }
        //h9_t.fill(event.get(MCParticle.class, "MCParticles").get(0).getEnergy());

    }
}
