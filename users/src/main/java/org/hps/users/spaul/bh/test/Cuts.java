package org.hps.users.spaul.bh.test;

import org.lcsim.event.Vertex;

public class Cuts {

    static double xtarget = 0; //NABO
    
    static double xmax =2, ymax = 1, 
                vchi2max = 10,
                v0pzmin = 1.2, v0pzmax = 2.75,
                minP = .1, maxP = 2.0,
                tchi2max = 50;

    public static boolean passesCuts(Vertex v) {
        double x = v.getPosition().x();
        double y = v.getPosition().y();
        double z = v.getPosition().z();
        if(Math.abs(x-xtarget)> 2 || Math.abs(y) > 1)
            return false;
        System.out.println("passed position test");
        if(v.getChi2()> vchi2max)
            return false;
        System.out.println("passed vertex chi2 test");
        
        double px = v.getAssociatedParticle().getMomentum().x();
        double py = v.getAssociatedParticle().getMomentum().y();
        double pz = v.getAssociatedParticle().getMomentum().z();
        
        if(pz < v0pzmin || pz > v0pzmax)
            return false;

        System.out.println("passed pztot test");
        
        double px1 = v.getAssociatedParticle().getParticles().get(0).getMomentum().x();
        double py1 = v.getAssociatedParticle().getParticles().get(0).getMomentum().y();
        double pz1 = v.getAssociatedParticle().getParticles().get(0).getMomentum().z();
        
        if(pz1 < minP || pz1> maxP)
            return false;
        System.out.println("passed pz1 test");
        
        double px2 = v.getAssociatedParticle().getParticles().get(1).getMomentum().x();
        double py2 = v.getAssociatedParticle().getParticles().get(1).getMomentum().y();
        double pz2 = v.getAssociatedParticle().getParticles().get(1).getMomentum().z();
        
        if(pz2 < minP || pz2> maxP)
            return false;
        System.out.println("passed pz2 test");
        
        if(v.getAssociatedParticle().getParticles().get(0).getTracks().size() == 0
                || v.getAssociatedParticle().getParticles().get(1).getTracks().size() == 0)
            return false;
        
        double chi2_1 = v.getAssociatedParticle().getParticles().get(0).getTracks().get(0).getChi2();

        double chi2_2 = v.getAssociatedParticle().getParticles().get(1).getTracks().get(0).getChi2();
        if(chi2_1 > tchi2max || chi2_2 > tchi2max)
            return false;
        
        return true;
    }

}
