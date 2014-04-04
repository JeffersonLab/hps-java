package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.hps.conditions.deprecated.EcalConditions;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.TrackUtils;
import org.hps.util.AIDAFrame;
import org.hps.util.Redrawable;
import org.hps.util.Resettable;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson+
 */
public class ECalGainDriver extends Driver implements Resettable, ActionListener, Redrawable {

    private int nevents = 0;
    private boolean debug = true;
    private String trackCollectionName = "MatchedTracks";
    private String ecalClusterCollectionName = "EcalClusters";
    private String outputPlotFileName = "test.aida";
    private String ecalGainFileName = "clusterList.txt";
    private String triggerClusterCollection = "EcalTriggerClusters";
    private double triggerThreshold = 10.0;
    private boolean simTrigger = false;
    //Print out cluster and track to file
    private PrintWriter gainWriter = null;
    private boolean hideFrame = false;
    private int refreshRate = 100;
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private AIDAFrame pePlotterFrame;
    private IPlotter plotter;
    private JComboBox xCombo;
    private JLabel xLabel;
    private JComboBox yCombo;
    private JLabel yLabel;
    private Integer xList[];
    private Integer yList[];
    private JButton blankButton;
    private IHistogram1D pePlots[][][] = new IHistogram1D[47][11][5];
    private IHistogram2D mpePlot;
    private IHistogram2D spePlot;
    private IHistogram2D hitmap;
    private IHistogram1D[] h_PE_t = new IHistogram1D[5];
    private IHistogram1D[] h_PE_b = new IHistogram1D[5];
    private IHistogram2D weightPlot;
    private IHistogram2D sumPlot;
    int clTop = 0;
    int clBot = 0;
    int trTop = 0;
    int trBot = 0;
    int trigTop = 0;
    int trigBot = 0;
    int matchTop = 0;
    int matchBot = 0;

    @Override
    public void detectorChanged(Detector detector) {
        try {
            gainWriter = new PrintWriter(ecalGainFileName);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ECalGainDriver.class.getName()).log(Level.SEVERE, null, ex);
        }

        pePlotterFrame = new AIDAFrame();
        pePlotterFrame.setTitle("Gain Frame");

        IPlotterStyle style;

        IPlotter plotter_hitmap_gr = af.createPlotterFactory().create();
        plotter_hitmap_gr.createRegions(1, 3, 0);
        plotter_hitmap_gr.setTitle("Cluster hit map");
        plotter_hitmap_gr.style().statisticsBoxStyle().setVisible(false);
        pePlotterFrame.addPlotter(plotter_hitmap_gr);

        hitmap = aida.histogram2D("Cluster hit map", 46, -23, 23, 11, -5.5, 5.5);
        plotter_hitmap_gr.region(0).plot(hitmap);

        style = plotter_hitmap_gr.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        ((PlotterRegion) plotter_hitmap_gr.region(0)).getPlot().setAllowUserInteraction(true);
        ((PlotterRegion) plotter_hitmap_gr.region(0)).getPlot().setAllowPopupMenus(true);

        IPlotter[] plotter_PoverE = new IPlotter[5];


        for (int iE = 0; iE <= 4; ++iE) {

            String str = iE == 0 ? "" : (" iE=" + iE);

            h_PE_t[iE] = aida.histogram1D("E over p top" + str, 50, 0, 2);
            h_PE_b[iE] = aida.histogram1D("E over p bottom" + str, 50, 0, 2);

            plotter_PoverE[iE] = af.createPlotterFactory().create();
            plotter_PoverE[iE].createRegions(1, 2, 0);
            plotter_PoverE[iE].setTitle("E over P" + str);
            plotter_PoverE[iE].style().statisticsBoxStyle().setVisible(true);
            pePlotterFrame.addPlotter(plotter_PoverE[iE]);

            plotter_PoverE[iE].region(0).plot(h_PE_t[iE]);
            plotter_PoverE[iE].region(1).plot(h_PE_b[iE]);
        }

        plotter = af.createPlotterFactory().create();
        plotter.createRegions(1, 3, 0);
        plotter.setTitle("Gain Plots");

        pePlotterFrame.addPlotter(plotter);

        mpePlot = aida.histogram2D("<E over p>", 46, -23, 23, 11, -5.5, 5.5);
        plotter.region(0).plot(mpePlot);
        spePlot = aida.histogram2D("RMS(E over p)", 46, -23, 23, 11, -5.5, 5.5);
        plotter.region(1).plot(spePlot);
        plotter.region(0).style().statisticsBoxStyle().setVisible(false);
        plotter.region(0).style().setParameter("hist2DStyle", "colorMap");
        plotter.region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter.region(1).style().statisticsBoxStyle().setVisible(false);
        plotter.region(1).style().setParameter("hist2DStyle", "colorMap");
        plotter.region(1).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");

        weightPlot = aida.histogram2D("Weights for correction factor", 46, -23, 23, 11, -5.5, 5.5);
        sumPlot = aida.histogram2D("Sum for correction factor", 46, -23, 23, 11, -5.5, 5.5);

        for (int iE = 0; iE <= 4; ++iE) {
            for (int irow = -5; irow <= 5; ++irow) {
                for (int icol = -23; icol <= 23; ++icol) {
                    if (iE == 0) {
                        pePlots[icol + 23][irow + 5][iE] = aida.histogram1D("E over p x=" + icol + " y=" + irow, 50, 0, 2);
                    } else {
                        pePlots[icol + 23][irow + 5][iE] = aida.histogram1D("E over p x=" + icol + " y=" + irow + " iE=" + iE, 50, 0, 2);
                    }
                }
            }
        }

        xList = new Integer[46];
        yList = new Integer[10];
        int in = 0;
        for (int i = -5; i <= 5; ++i) {
            if (i != 0) {
                yList[in] = i;
                ++in;
            }
        }
        in = 0;
        for (int i = -23; i <= 23; ++i) {
            if (i != 0) {
                xList[in] = i;
                ++in;
            }
        }

        xCombo = new JComboBox(xList);
        xCombo.addActionListener(this);
        xLabel = new JLabel("x");
        xLabel.setLabelFor(xCombo);
        pePlotterFrame.getControlsPanel().add(xLabel);
        pePlotterFrame.getControlsPanel().add(xCombo);
        yCombo = new JComboBox(yList);
        yCombo.addActionListener(this);
        yLabel = new JLabel("y");
        yLabel.setLabelFor(xCombo);
        pePlotterFrame.getControlsPanel().add(yLabel);
        pePlotterFrame.getControlsPanel().add(yCombo);

        plotter.region(2).plot(pePlots[-5 + 23][2 + 5 - 1][0]);
        xCombo.setSelectedIndex(-5 + 23);
        yCombo.setSelectedIndex(2 + 5 - 1);

        blankButton = new JButton("Hide histogram");
        pePlotterFrame.getControlsPanel().add(blankButton);
        blankButton.addActionListener(this);

        if (!hideFrame) {
            pePlotterFrame.pack();
            pePlotterFrame.setVisible(true);
        }
    }

    public ECalGainDriver() {
    }

    public void setDebug(boolean flag) {
        this.debug = flag;
    }

    public void setSimTrigger(boolean simTrigger) {
        this.simTrigger = simTrigger;
    }

    public void setOutputPlotFileName(String name) {
        this.outputPlotFileName = name;
    }

    public void setEcalGainFileName(String name) {
        this.ecalGainFileName = name;
    }

    public void setHideFrame(boolean val) {
        this.hideFrame = val;
    }

    @Override
    public void process(EventHeader event) {
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalClusterCollectionName);

        List<Track> tracks;
        if (event.hasCollection(Track.class, trackCollectionName)) {
            tracks = event.get(Track.class, trackCollectionName);
        } else {
            return;
        }

        if (simTrigger) {
            boolean trigger = false;
            if (event.hasCollection(HPSEcalCluster.class, triggerClusterCollection)) {
                List<HPSEcalCluster> triggerClusters = event.get(HPSEcalCluster.class, triggerClusterCollection);
                if (debug) {
                    System.out.println("Found " + triggerClusters.size() + " trigger clusters and " + clusters.size() + " readout clusters");
                }
                for (HPSEcalCluster cluster : triggerClusters) {
                    if (cluster.getEnergy() > triggerThreshold) {
                        trigger = true;
                        int[] pos = getCrystalPair(cluster);
                        int side = pos[1] > 0 ? 0 : 1; //top or bottom
                        if (side == 0) {
                            trigTop++;
                        } else {
                            trigBot++;
                        }
                    }
                }
            }
            if (!trigger) {
                return;
            }
            if (debug) {
                System.out.println("Triggered");
            }
        }

        ++nevents;
        if (debug) {
            System.out.println("Processing event " + nevents);
        }

        if (refreshRate > 0 && nevents % refreshRate == 0) {
            redraw();
        }

        for (Track track : tracks) {
            BaseTrack trk = (BaseTrack) track;
            trk.setTrackParameters(trk.getTrackStates().get(0).getParameters(), -0.491);
            if (debug) {
                if (TrackUtils.getTrackPositionAtEcal(trk).y() > 0) {
                    trTop++;
                } else {
                    trBot++;
                }
            }
        }

        if (debug) {
            System.out.println(clusters.size() + " clusters in this event");
            for (HPSEcalCluster cl : clusters) {
                int[] pos = getCrystalPair(cl);
                int side = pos[1] > 0 ? 0 : 1; //top or bottom
                if (side == 0) {
                    clTop++;
                } else {
                    clBot++;
                }
                System.out.format("[%f\t%f\t%f], ix = %d, iy = %d\n", cl.getPosition()[2], cl.getPosition()[0], cl.getPosition()[1], pos[0], pos[1]);
            }

            System.out.println(tracks.size() + " tracks in this event");
            for (Track track : tracks) {
                System.out.println(TrackUtils.getTrackPositionAtEcal(track));
            }
        }


        while (!tracks.isEmpty() && !clusters.isEmpty()) {
            HPSEcalCluster bestCl = null;
            Track bestTrk = null;
            double minDist = Double.POSITIVE_INFINITY;

            for (HPSEcalCluster cl : clusters) {
                EcalTrackMatch trkMatchTool = new EcalTrackMatch(debug);
                int[] pos = getCrystalPair(cl);
                if (debug) {
                    System.out.println("Looking at cluster at ix=" + pos[0] + " iy=" + pos[1]);
                }

                trkMatchTool.setCluster(cl);
                trkMatchTool.match(tracks);
//            double dist = trkMatchTool.getDistanceToTrackInY();
                double dist = trkMatchTool.getDistanceToTrack();
                if (dist < minDist) {
                    minDist = dist;
                    bestCl = cl;
                    bestTrk = trkMatchTool.getMatchedTrack();
                }
            }
            tracks.remove(bestTrk);
            clusters.remove(bestCl);

            if (bestCl != null & bestTrk != null && minDist < 80.0) {
                if (debug) {
                    int[] pos = getCrystalPair(bestCl);
                    System.out.format("Matched cluster: [%f\t%f\t%f], ix = %d, iy = %d\n", bestCl.getPosition()[2], bestCl.getPosition()[0], bestCl.getPosition()[1], pos[0], pos[1]);
                    System.out.println("Matched track: " + TrackUtils.getTrackPositionAtEcal(bestTrk));
                    System.out.println("Distance: " + minDist);
                }
                processMatchedPair(event, bestCl, bestTrk);
            } else {
                if (debug) {
                    System.out.println("Couldn't find a good match");
                }
            }
        }
    }

    private void processMatchedPair(EventHeader event, HPSEcalCluster bestCl, Track bestTrk) {

        int[] pos = getCrystalPair(bestCl);
        int side = pos[1] > 0 ? 0 : 1; //top or bottom
//            if (!trkMatchTool.isMatchedY(50)) {
//                if (debug) {
//                    System.out.println("Cluster not matched to a track");
//                }
//                if (!tracks.isEmpty()) {
//                    System.out.format("Unmatched cluster: ");
//                } else {
//                    System.out.format("No tracks to match to this cluster: ");
//                }
//                System.out.format("[%f\t%f\t%f], ix = %d, iy = %d\n", bestCl.getPosition()[2], bestCl.getPosition()[0], bestCl.getPosition()[1], pos[0], pos[1]);
//                    ExtendTrack extender = new ExtendTrack();
//                    extender.setTrack(bestTrk);
//                    System.out.println(extender.positionAtEcal());
//                return;
//            }

//            if (debug) {
//                System.out.println("Cluster matched to track at distance Y " + trkMatchTool.getDistanceToTrackInY() + " and X " + trkMatchTool.getDistanceToTrackInX());
//            }
        if (debug && side == 0) {
            matchTop++;
        } else {
            matchBot++;
        }

        double P = bestTrk.getTrackStates().get(0).getMomentum()[0] * 1000.0;
        double E = bestCl.getEnergy() * 1000.0;
        double Eoverp = E / P;
        double PoverE = P / E;

        for (CalorimeterHit hit : bestCl.getCalorimeterHits()) {
            double weight = hit.getRawEnergy() / bestCl.getEnergy();
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            weightPlot.fill(ix - 0.5 * Math.signum(ix), iy, weight);
            sumPlot.fill(ix - 0.5 * Math.signum(ix), iy, weight * PoverE);
        }

        if (debug) {
            System.out.println("P " + P + " E " + E);
        }

//            double Eseed = cl.getSeedHit().getRawEnergy();
//            double ErawSum = 0;
//            for(CalorimeterHit hit : cl.getCalorimeterHits()) {
//                ErawSum += hit.getRawEnergy();
//            }
//            
//            if(Eseed/ErawSum<0.6) continue;
//            
        int ebin;
        if (P > 500 && P <= 700) {
            ebin = 1;
        } else if (P > 700 && P <= 900) {
            ebin = 2;
        } else if (P > 900 && P <= 1100) {
            ebin = 3;
        } else {
            ebin = 4;
        }

        if (side == 0) {
            h_PE_t[0].fill(Eoverp);
            h_PE_t[ebin].fill(Eoverp);

        } else {
            h_PE_b[0].fill(Eoverp);
            h_PE_b[ebin].fill(Eoverp);
        }

        hitmap.fill(pos[0] - 0.5 * Math.signum(pos[0]), pos[1]);

        pePlots[pos[0] + 23][pos[1] + 5][0].fill(Eoverp);

        pePlots[pos[0] + 23][pos[1] + 5][ebin].fill(Eoverp);

        gainWriter.print(event.getEventNumber() + " " + P + " " + E + " " + pos[0] + " " + pos[1]);
        for (CalorimeterHit hit : bestCl.getCalorimeterHits()) {
            gainWriter.print(" " + hit.getIdentifierFieldValue("ix") + " " + hit.getIdentifierFieldValue("iy") + " " + hit.getRawEnergy() * 1000.0 + " " + EcalConditions.physicalToGain(hit.getCellID()));
        }
        gainWriter.println("");
    }

    private int[] getCrystalPair(HPSEcalCluster cluster) {
        int[] pos = new int[2];
        pos[0] = cluster.getSeedHit().getIdentifierFieldValue("ix");
        pos[1] = cluster.getSeedHit().getIdentifierFieldValue("iy");

        //System.out.println("cluster ix,iy " + pos[0] + "," + pos[1] + "    from pos  " + cluster.getSeedHit().getPositionVec().toString());
        return pos;
        //getCrystalPair(cluster.getPosition());
    }

    @Override
    public void endOfData() {
        redraw();

        if (!"".equals(outputPlotFileName)) {
            try {
                aida.saveAs(outputPlotFileName);
            } catch (IOException ex) {
                Logger.getLogger(ECalGainDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
            }
        }
        //displayFastTrackingPlots();
        for (int x = -23; x <= 23; x++) { // slot
            for (int y = -5; y <= 5; y++) { // crate  
                if (EcalConditions.physicalToGain(EcalConditions.makePhysicalID(x, y)) != null) {
                    System.out.printf("%d\t%d\t%d\t%f\t%f\n", x, y, pePlots[x + 23][y + 5][0].allEntries(), pePlots[x + 23][y + 5][0].mean(), pePlots[x + 23][y + 5][0].rms());
                }
            }
        }

        gainWriter.close();
        if (debug) {
            System.out.format("trigTop %d trigBot %d trTop %d trBot %d clTop %d clBot %d matchTop %d matchBot %d\n", trigTop, trigBot, trTop, trBot, clTop, clBot, matchTop, matchBot);
        }
    }

    @Override
    public void reset() {
        if (plotter != null) {
            plotter.hide();
            plotter.destroyRegions();
            for (int x = -23; x <= 23; x++) { // slot
                for (int y = -5; y <= 5; y++) { // crate  
                    for (int iE = 0; iE <= 4; ++iE) {
                        pePlots[x + 23][y + 5][iE].reset();
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == blankButton) {
            plotter.region(2).clear();
        } else {
            //get the col and row to display
            Integer x = (Integer) xCombo.getSelectedItem();
            Integer y = (Integer) yCombo.getSelectedItem();
            plotter.region(2).clear();
            plotter.region(2).plot(pePlots[x + 23][y + 5][0]);
        }
    }

    @Override
    public void redraw() {

        //do something if needed
        mpePlot.reset();
        spePlot.reset();

        for (int irow = -5; irow <= 5; ++irow) {
            for (int icol = -23; icol <= 23; ++icol) {
                //System.out.println(" redraw irow " + irow + " icol " + icol + " entries " + pePlots[icol+23][irow+5].entries());
                if (pePlots[icol + 23][irow + 5][0].entries() > 10) {
                    mpePlot.fill(icol - 0.5 * Math.signum(icol), irow, pePlots[icol + 23][irow + 5][0].mean());
                    spePlot.fill(icol - 0.5 * Math.signum(icol), irow, pePlots[icol + 23][irow + 5][0].rms());
                }
            }
        }
    }

    @Override
    public void setEventRefreshRate(int eventRefreshRate) {
        refreshRate = eventRefreshRate;
    }
}
