/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.filtering;

import org.lcsim.util.Driver;

/**

 @author mgraham
 @version $Id:
 */
public class EventReconFilter extends Driver{

    private int nprocessed=0;
    private int npassed=0;

    public EventReconFilter(){
    }

    public void endOfData(){
        System.out.println(this.getClass().getSimpleName()+" Summary: ");
        System.out.println("events processed = "+nprocessed);
        System.out.println("events passed    = "+npassed);
        System.out.println("       rejection = "+((double) npassed)/nprocessed);

    }

    public void incrementEventProcessed(){
        nprocessed++;
    }
    
     public void incrementEventPassed(){
        npassed++;
    }
    
    public void skipEvent(){
        throw new Driver.NextEventException();
    }
}
