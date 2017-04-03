package org.hps.users.spaul;

import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram2D;

public class TCMatchParameters extends Driver{

    private IHistogram2D fidTopElec5x;
    private IHistogram2D fidBotElec5x;
    private IHistogram2D fidBotPosi5x;
    private IHistogram2D fidTopPosi5x;
    private IHistogram2D fidTopElec6x;
    private IHistogram2D fidBotElec6x;
    private IHistogram2D fidBotPosi6x;
    private IHistogram2D fidTopPosi6x;

    private IHistogram2D fidTopElec5y;
    private IHistogram2D fidBotElec5y;
    private IHistogram2D fidBotPosi5y;
    private IHistogram2D fidTopPosi5y;
    private IHistogram2D fidTopElec6y;
    private IHistogram2D fidBotElec6y;
    private IHistogram2D fidBotPosi6y;
    private IHistogram2D fidTopPosi6y;
    @Override
    protected void detectorChanged(Detector d){
        AIDA aida = AIDA.defaultInstance();
        fidBotPosi6y = aida.histogram2D("0000", 100, 0, 3, 100, -20, 20);
        fidBotElec6y = aida.histogram2D("0001", 100, 0, 3, 100, -20, 20);
        fidBotPosi5y = aida.histogram2D("0010", 100, 0, 3, 100, -20, 20);
        fidBotElec5y = aida.histogram2D("0011", 100, 0, 3, 100, -20, 20);

        fidTopPosi6y = aida.histogram2D("0100", 100, 0, 3, 100, -20, 20);
        fidTopElec6y = aida.histogram2D("0101", 100, 0, 3, 100, -20, 20);
        fidTopPosi5y = aida.histogram2D("0110", 100, 0, 3, 100, -20, 20);
        fidTopElec5y = aida.histogram2D("0111", 100, 0, 3, 100, -20, 20);

        fidBotPosi6x = aida.histogram2D("1000", 100, 0, 3, 100, -20, 20);
        fidBotElec6x = aida.histogram2D("1001", 100, 0, 3, 100, -20, 20);
        fidBotPosi5x = aida.histogram2D("1010", 100, 0, 3, 100, -20, 20);
        fidBotElec5x = aida.histogram2D("1011", 100, 0, 3, 100, -20, 20);

        fidTopPosi6x = aida.histogram2D("1100", 100, 0, 3, 100, -20, 20);
        fidTopElec6x = aida.histogram2D("1101", 100, 0, 3, 100, -20, 20);
        fidTopPosi5x = aida.histogram2D("1110", 100, 0, 3, 100, -20, 20);
        fidTopElec5x = aida.histogram2D("1111", 100, 0, 3, 100, -20, 20);

        //matcher = new TrackClusterMatcher();
        //matcher.setRunNumber(this.getConditionsManager().getRun());
    }
    @Override
    protected void process(EventHeader event){
        List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
        List<Track> tracks = event.get(Track.class, "GBLTracks");
        
        for(Cluster c: clusters)
            if(!(c.getPosition()[0] > -262.74 && c.getPosition()[0] < 347.7 && Math.abs(c.getPosition()[1])>33.54 
                    && Math.abs(c.getPosition()[1])<75.18 
                    && !(c.getPosition()[0]>-106.66 && c.getPosition()[0] < 42.17 && Math.abs(c.getPosition()[1])<47.17)))
                return;
        
        if(tracks.size() != 2 || clusters.size() != 2)
            return;
        if(tracks.get(0).getChi2() >50 || tracks.get(1).getChi2() > 50)
            return;
        double xextrap1 = TrackUtils.getTrackPositionAtEcal(tracks.get(0)).x();
        double yextrap1 = TrackUtils.getTrackPositionAtEcal(tracks.get(0)).y();
        double charge1 = TrackUtils.getCharge(tracks.get(0));
        int nhits1 = tracks.get(0).getTrackerHits().size();
        double pz1 = 1.57e-4*Math.abs(TrackUtils.getR(tracks.get(0)));
        
        double xextrap2 = TrackUtils.getTrackPositionAtEcal(tracks.get(1)).x();
        double yextrap2 = TrackUtils.getTrackPositionAtEcal(tracks.get(1)).y();
        double charge2 = TrackUtils.getCharge(tracks.get(1));
        int nhits2 = tracks.get(1).getTrackerHits().size();
        double pz2 = 1.57e-4*Math.abs(TrackUtils.getR(tracks.get(1)));



        double x1 = clusters.get(0).getPosition()[0];
        double y1 = clusters.get(0).getPosition()[1];
        double x2 = clusters.get(1).getPosition()[0];
        double y2 = clusters.get(1).getPosition()[1];
        
        double dt = clusters.get(0).getCalorimeterHits().get(0).getTime()-clusters.get(1).getCalorimeterHits().get(0).getTime();
        if(Math.abs(dt) > 2.5)
            return;
        
        //switch the clusters.
        if(y1*yextrap1 < 0 && y2*yextrap2 <0){
            double temp = x1;
            x1 = x2;
            x2 = temp;
            temp = y1;
            y1 = y2;
            y2 = temp;
            //if there isn't exactly one cluster and one track on each side return.  
        } else if(y1*yextrap1 <0 || y2*yextrap2 <0)
            return;
        if(charge1 == charge2)
            return;

        double dx1 = x1-xextrap1;
        double dy1 = y1-yextrap1;
        double dx2 = x2-xextrap2;
        double dy2 = y2-yextrap2;

        fill(y1 >0, nhits1 == 5, charge1 == -1,  dx1, dy1, pz1);
        fill(y2 >0, nhits2 == 5, charge2 == -1,  dx2, dy2, pz2);



    }

    void fill(boolean isTop, boolean is5, boolean isElec, double dx, double dy, double pz){
        if(isTop){
            if(isElec){
                if(is5){
                    fidTopElec5x.fill(pz, dx);
                    fidTopElec5y.fill(pz, dy);
                }else{
                    fidTopElec6x.fill(pz, dx);
                    fidTopElec6y.fill(pz, dy);
                }
            } else {
                if(is5){
                    fidTopPosi5x.fill(pz, dx);
                    fidTopPosi5y.fill(pz, dy);
                }else{
                    fidTopPosi6x.fill(pz, dx);
                    fidTopPosi6y.fill(pz, dy);
                }
            }
        }else{
            if(isElec){
                if(is5){
                    fidBotElec5x.fill(pz, dx);
                    fidBotElec5y.fill(pz, dy);
                }else{
                    fidBotElec6x.fill(pz, dx);
                    fidBotElec6y.fill(pz, dy);
                }
            } else {
                if(is5){
                    fidBotPosi5x.fill(pz, dx);
                    fidBotPosi5y.fill(pz, dy);
                }else{
                    fidBotPosi6x.fill(pz, dx);
                    fidBotPosi6y.fill(pz, dy);
                }
            }
        }
    }
}
