package org.hps.analysis.examples;

import static java.lang.Math.sqrt;
import java.util.List;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

public class VertexAnalysis extends Driver {

    String vertexCollectionName = "TargetConstrainedV0Vertices";
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[4];
    double[] p2 = new double[4];
    double emass = 0.000511;
    double mass2 = emass*emass;
    
    boolean debug = false;
    
    protected void process(EventHeader event) {
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        for (Vertex v : vertices) {
            Map<String, Double> vals = v.getParameters();
            //System.out.println(vals);
            p1[0] = vals.get("p1X");
            p1[1] = vals.get("p1Y");
            p1[2] = vals.get("p1Z");
            p2[0] = vals.get("p2X");
            p2[1] = vals.get("p2Y");
            p2[2] = vals.get("p2Z");
            double e1 = 0;
            double e2 = 0.;
            for(int i=0; i<3; ++i)
            {
               e1 += p1[i]*p1[i];
               e2 += p2[i]*p2[i];
            }
            e1 = sqrt(e1+mass2);
            e2 = sqrt(e2+mass2);
            Momentum4Vector vec1 = new Momentum4Vector(p1[0], p1[1], p1[2], e1);
            Momentum4Vector vec2 = new Momentum4Vector(p2[0], p2[1], p2[2], e2);
            Lorentz4Vector sum = vec1.plus(vec2);
            double mass = sum.mass();
            double invMass = vals.get("invMass");
            if(debug) System.out.println("mass: "+mass+" invMass: "+invMass +" delta: "+(invMass-mass));
            
        }
    }
}
