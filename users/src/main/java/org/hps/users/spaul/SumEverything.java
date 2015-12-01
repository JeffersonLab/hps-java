package org.hps.users.spaul;

import java.io.File;
import java.io.IOException;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;

//sums up everything in a folder full of files containing histograms.  Pretty convenient.
// or if one arg is given, assume that that one arg is a folder full of folders
// full of aida output files.  Then add up all of the histograms in each sub folder,
// and put the sums in separate files in a folder called "sums"
public class SumEverything {
	public static void main(String arg[]) throws IllegalArgumentException, IOException{
		if(arg.length > 1){
			twoArg(arg[0], arg[1]);
		}
		else{
			oneArg(arg[0]);
		}
	}
	static void oneArg(String indir) throws IllegalArgumentException, IOException{
		File outdir = new File(indir + "/sums");
		outdir.mkdir();
		for(File subdirf : new File(indir).listFiles()){
			String subdir = subdirf.getAbsolutePath();
			if(subdir.matches(".*/sums/?"))
				continue;
			String split[] = subdir.split("/");
			String outfile = indir + "/sums/" + split[split.length-1] + ".aida";
			twoArg(subdir, outfile);
		}
		new File(indir + "/sums/total.aida").delete();
		twoArg(outdir.getAbsolutePath(), indir + "/sums/total.aida");
		
	}

	static void twoArg(String indir, String out) throws IllegalArgumentException, IOException{
		IAnalysisFactory af = IAnalysisFactory.create();
		ITreeFactory tf = af.createTreeFactory();
		new File(out).delete();
		ITree tree0 = tf.create(out, "xml", false, true);
		IHistogramFactory hf = af.createHistogramFactory(tree0);
		

		int j = 0;
		for(File s : new File(indir).listFiles()){
			ITree tree = af.createTreeFactory().create(s.getAbsolutePath(),"xml");

			
			if(j == 0){
				String [] names = tree.listObjectNames();
				tree0.mount("/tmp", tree, "/");
				for(String name : names){
					Object o = tree.find(name);
					if(o instanceof IHistogram1D || o instanceof IHistogram2D)
						tree0.cp("/tmp/" + name, name);
				}
				tree0.unmount("/tmp");
				tree.close();
				
			}
			else{
				//tree.
				String [] names = tree.listObjectNames();
				tree0.mount("/tmp", tree, "/");
				for(String name : names){
					Object o = tree.find(name);
					if(o instanceof IHistogram1D)
						((IHistogram1D)tree0.find(name)).add((IHistogram1D)o);
					if(o instanceof IHistogram2D)
						((IHistogram2D)tree0.find(name)).add((IHistogram2D)o);
				}
				tree0.unmount("/tmp");
				tree.close();
			}

			tree.close();
			j++;
			System.out.println(j + " files have been read");
		}
		tree0.commit();
	}
}

