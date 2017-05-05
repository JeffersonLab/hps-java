package org.hps.users.spaul;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

//sums up everything in a folder full of files containing histograms.  Pretty convenient.
// or if one arg is given, assume that that one arg is a folder full of folders
// full of aida output files.  Then add up all of the histograms in each sub folder,
// and put the sums in separate files in a folder called "sums"
public class SumEverything {
    public static void main(String arg[]) throws IllegalArgumentException, IOException{
        if(arg.length == 2){
            twoArg(arg[0], arg[1]);
        }
        else if(arg.length == 1){
            oneArg(arg[0]);
        }
        else 
            polyArg(arg);
    }
    static void oneArg(final String indir) throws IllegalArgumentException, IOException{
        File outdir = new File(indir + "/sums");
        outdir.mkdir();
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for(final File subdirf : new File(indir).listFiles()){
            Thread t = new Thread(){
                public void run(){

                    String subdir = subdirf.getAbsolutePath();
                    if(subdir.matches(".*/sums/?"))
                        return;
                    String split[] = subdir.split("/");
                    String outfile = indir + "/sums/" + split[split.length-1] + ".aida";
                    try {
                        twoArg(subdir, outfile);
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };
            threads.add(t);
            t.start();
        }
        for(Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        new File(indir + "/sums/total.aida").delete();
        twoArg(outdir.getAbsolutePath(), indir + "/sums/total.aida");

    }



    static void twoArg(String indir, String out) throws IllegalArgumentException, IOException{

        run(new File(indir).listFiles(), out);
    }
    static void run(File[] files, String out) throws IllegalArgumentException, IOException{
        
        long timeStart = System.currentTimeMillis();
        IAnalysisFactory af = IAnalysisFactory.create();
        //AIDA.defaultInstance().
        ITreeFactory tf = af.createTreeFactory();
        new File(out).delete();
        ITree outtree = tf.createTree(out, "xml", ITreeFactory.RECREATE);
        IHistogramFactory hf = af.createHistogramFactory(outtree);
        int j = 0;
        String names[] = null;
        for(File s : files){
            System.gc();
            if(!s.getAbsolutePath().endsWith("aida"))
                continue;
            try{
                
                ITree tree = tf.createTree(s.getAbsolutePath(), "xml", ITreeFactory.READONLY);//.create(s.getAbsolutePath(),"xml");


                if(j == 0){
                    names = tree.listObjectNames("/", true);
                    System.out.println(Arrays.toString(names));
                    //outtree.mount("/tmp", tree, "/");
                    for(String name : names){
                        if(name.endsWith("/")){
                            outtree.mkdirs(name);
                            continue;
                        }
                        Object o = tree.find(name);
                        if(o instanceof IHistogram1D)
                            hf.createCopy(name,(IHistogram1D)o);
                        if(o instanceof IHistogram2D)
                            hf.createCopy(name,(IHistogram2D)o);
                        
                    }
                    //outtree.unmount("/tmp");
                    //tree.close();

                }
                else{
                    //tree.
                    //String [] names = tree.listObjectNames("/", true);
                    //outtree.mount("/tmp", tree, "/");
                    //System.out.println(Arrays.toString(names));
                    for(String name : names){
                        if(name.endsWith("/"))
                            continue;
                        Object o = null;
                        try{
                            o = tree.find(name);
                        } catch(IllegalArgumentException e){
                            System.err.println("couldn't find object called " + name +  " in file " + s);
                            throw e;
                        }
                        if(o instanceof IHistogram1D){
                            if(((IHistogram1D)o).allEntries() != 0)
                            ((IHistogram1D)outtree.find(name)).add((IHistogram1D)o);
                        }
                        if(o instanceof IHistogram2D)
                            if(((IHistogram2D)o).allEntries() != 0)
                            ((IHistogram2D)outtree.find(name)).add((IHistogram2D)o);
                    }
                    //outtree.unmount("/tmp");
                    //tree.close();
                }

                tree.close();
                j++;
                System.out.println(j + " files have been read (" +(System.currentTimeMillis()-timeStart)/j + " ms per file)");

            } catch(IllegalArgumentException e){
                //print the filename
                System.out.println("Exception happened at file " + s.getAbsolutePath());

                e.printStackTrace();
            }

        }
        outtree.commit();
        System.out.println("summed file " + out +" commited.  Total time = " + (System.currentTimeMillis()-timeStart)/1000 + " seconds");
    
    }

    static void polyArg(String[] arg) throws IllegalArgumentException, IOException{
        ArrayList<File> files = new ArrayList<File>();
        boolean nextIsOutput = false;
        for(String a : arg){
            if(a.equals("-o")){
                nextIsOutput = true; 
                continue;
            }
            if(nextIsOutput){
                run(files.toArray(new File[0]), a);
                nextIsOutput = false;
                files.clear();
                continue;
            }
            files.add(new File(a));

        }
    }
}

