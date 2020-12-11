package org.hps.online.example;

public class Scratch {

    /*
    System.out.println(">>>> bindName: " + rmiTreeServer.getBindName());
    try {
        Registry registry = LocateRegistry.getRegistry();
        String[] l = registry.list();
        System.out.println("RMI list: ");
        for (String s : l) {
            System.out.println("  " + s);
        }
    } catch (RemoteException e) {
        e.printStackTrace();
    }
    */

    /*
    // Show a plot for debugging purposes
    IPlotterFactory pf = af.createPlotterFactory();
    IPlotter plotter = null;
    private void show(String[] paths) {
        if (plotter == null) {
            plotter = pf.create();
            plotter.createRegions(paths.length);
            for (int i = 0; i < paths.length; i++) {
                IPlotterRegion region = plotter.region(i);
                IBaseHistogram hist = (IBaseHistogram) serverTree.find(paths[i]);
                if (hist instanceof IHistogram2D) {
                    IPlotterStyle style = region.style();
                    style.setParameter("hist2DStyle", "colorMap");
                    style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                    style.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
                }
                region.plot(hist);
            }
            plotter.show();
        }
    }
    */

}
