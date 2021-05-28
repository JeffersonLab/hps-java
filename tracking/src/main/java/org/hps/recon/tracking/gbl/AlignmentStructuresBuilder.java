package org.hps.recon.tracking.gbl;

//import org.lcsim.detector.tracker.silicon.AlignableDetectorElement;
//import org.lcsim.detector.ITransform3D;
//import org.lcsim.detector.Transform3D;

import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.IRotation3D;
//import org.lcsim.detector.Vector3D;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;
import org.hps.recon.tracking.gbl.matrix.Matrix;
//import org.hps.recon.tracking.gbl.matrix.Vector;
//Rounding
import java.math.BigDecimal;
import java.math.RoundingMode;

// Constrain file writer
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class AlignmentStructuresBuilder { 

    //void buildShortModule(SiSensor siAxial, SiSensor siStereo){};
    //void buildDoubleSide(SiSensor siHole, SiSensor siSlot){};
    //void buildLongModule(){};
    
    //List<AlignableVolume> alignable_sensors = new ArrayList<AlignableVolume>();
    Map<String,AlignableVolume> alignable_volumes = new HashMap<String,AlignableVolume>();
    
    List<Integer> top_labels = new ArrayList<Integer>();
    List<Integer> bot_labels = new ArrayList<Integer>();

    FileWriter writer = null;
    

    //Pass the list of sensors of the HPS Tracker
    public AlignmentStructuresBuilder(List<SiSensor> sensors) {

        
        top_labels.add(11100);
        top_labels.add(11200);
        top_labels.add(11300);
        top_labels.add(12100);
        top_labels.add(12200);
        top_labels.add(12300);
        
        
        bot_labels.add(21100);
        bot_labels.add(21200);
        bot_labels.add(21300);
        bot_labels.add(22100);
        bot_labels.add(22200);
        bot_labels.add(22300);
    
    
        //Create first a list of alignmentVolumes from the sensors directly
        
        for (SiSensor sensor : sensors) {
            
            if (!(sensor instanceof HpsSiSensor) && sensor.getName().contains("Scoring")) 
                continue;
            
            //Null parent for the sensors for the moment
            //Null daughters
            
            AlignableVolume av_sensor = new AlignableVolume(sensor.getName()+ "_AV", ((HpsSiSensor)sensor).getGeometry().getLocalToGlobal(),null,null,((HpsSiSensor)sensor).getMillepedeId());
            alignable_volumes.put(av_sensor.getName(),av_sensor);
            if (((HpsSiSensor)sensor).isTopLayer()){
                av_sensor.setLabels(top_labels,av_sensor.getMillepedeId());
            }
            else {
                av_sensor.setLabels(bot_labels,av_sensor.getMillepedeId());
            }
        }
        
        //Now let's build the modules. 
        
        List<String> volumes = new ArrayList<String>();
        volumes.add("module_L1t_halfmodule_axial_sensor0_AV");
        volumes.add("module_L1t_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL1_Top_AV",61);
        volumes.clear();

        volumes.add("module_L2t_halfmodule_axial_sensor0_AV");
        volumes.add("module_L2t_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL2_Top_AV",62);
        volumes.clear();
        
        
        volumes.add("module_L3t_halfmodule_axial_sensor0_AV");
        volumes.add("module_L3t_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL3_Top_AV",63);
        volumes.clear();
        
        
        volumes.add("module_L4t_halfmodule_axial_sensor0_AV");
        volumes.add("module_L4t_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL4_Top_AV",64);
        volumes.clear();

        //Build front uChannel
        List<AlignableVolume> aliVolumeList = new ArrayList<AlignableVolume>();
        aliVolumeList.add(alignable_volumes.get("ModuleL1_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL2_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL3_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL4_Top_AV")); 
        IRotation3D rot = (alignable_volumes.get("ModuleL1_Top_AV")).getL2G().getRotation();
        AlignableVolume UChannelL14_Top = new AlignableVolume("UChannelL14_Top_AV", aliVolumeList, rot, null,80);
        alignable_volumes.put("UChannelL14_Top_AV",UChannelL14_Top);
        aliVolumeList.clear();

        
        //Build half sensors L5 - axial
        aliVolumeList.add(alignable_volumes.get("module_L5t_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L5t_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L5t_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L5_Top_AV = new AlignableVolume("doublesensor_axial_L5_Top_AV", aliVolumeList, rot, null,71);
        alignable_volumes.put("doublesensor_axial_L5_Top_AV",doublesensor_axial_L5_Top_AV);
        aliVolumeList.clear();


        //Build half sensors L5 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L5t_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L5t_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L5t_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L5_Top_AV = new AlignableVolume("doublesensor_stereo_L5_Top_AV", aliVolumeList, rot, null,72);
        alignable_volumes.put("doublesensor_stereo_L5_Top_AV",doublesensor_stereo_L5_Top_AV);
        aliVolumeList.clear();

        
        //Build module L5
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L5_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L5_Top_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L5_Top_AV")).getL2G().getRotation();
        AlignableVolume M5_Top = new AlignableVolume("ModuleL5_Top_AV", aliVolumeList, rot, null,65);
        alignable_volumes.put("ModuleL5_Top_AV",M5_Top);
        aliVolumeList.clear();

        
        //Build half sensors L6 - axial
        aliVolumeList.add(alignable_volumes.get("module_L6t_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L6t_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L6t_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L6_Top_AV = new AlignableVolume("doublesensor_axial_L6_Top_AV", aliVolumeList, rot, null,73);
        alignable_volumes.put("doublesensor_axial_L6_Top_AV",doublesensor_axial_L6_Top_AV);
        aliVolumeList.clear();


        //Build half sensors L6 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L6t_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L6t_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L6t_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L6_Top_AV = new AlignableVolume("doublesensor_stereo_L6_Top_AV", aliVolumeList, rot, null,74);
        alignable_volumes.put("doublesensor_stereo_L6_Top_AV",doublesensor_stereo_L6_Top_AV);
        aliVolumeList.clear();

        
        //Build module L6
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L6_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L6_Top_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L6_Top_AV")).getL2G().getRotation();
        AlignableVolume M6_Top = new AlignableVolume("ModuleL6_Top_AV", aliVolumeList, rot, null,66);
        alignable_volumes.put("ModuleL6_Top_AV",M6_Top);
        aliVolumeList.clear();


        //Build half sensors L7 - axial
        aliVolumeList.add(alignable_volumes.get("module_L7t_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L7t_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L7t_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L7_Top_AV = new AlignableVolume("doublesensor_axial_L7_Top_AV", aliVolumeList, rot, null,75);
        alignable_volumes.put("doublesensor_axial_L7_Top_AV",doublesensor_axial_L7_Top_AV);
        aliVolumeList.clear();


        //Build half sensors L7 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L7t_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L7t_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L7t_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L7_Top_AV = new AlignableVolume("doublesensor_stereo_L7_Top_AV", aliVolumeList, rot, null,76);
        alignable_volumes.put("doublesensor_stereo_L7_Top_AV",doublesensor_stereo_L7_Top_AV);
        aliVolumeList.clear();

        
        //Build module L7
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L7_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L7_Top_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L7_Top_AV")).getL2G().getRotation();
        AlignableVolume M7_Top = new AlignableVolume("ModuleL7_Top_AV", aliVolumeList, rot, null,67);
        alignable_volumes.put("ModuleL7_Top_AV",M7_Top);
        aliVolumeList.clear();

        
        //Build back UChannel
        aliVolumeList.add(alignable_volumes.get("ModuleL5_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL6_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL7_Top_AV"));
        rot = (alignable_volumes.get("ModuleL5_Top_AV")).getL2G().getRotation();
        AlignableVolume UChannelL57_Top = new AlignableVolume("UChannelL57_Top_AV", aliVolumeList, rot, null,90);
        alignable_volumes.put("UChannelL57_Top_AV",UChannelL57_Top);
        aliVolumeList.clear();


        //Build Top Volume
        aliVolumeList.add(alignable_volumes.get("UChannelL14_Top_AV"));
        aliVolumeList.add(alignable_volumes.get("UChannelL57_Top_AV"));
        rot = (alignable_volumes.get("UChannelL14_Top_AV")).getL2G().getRotation();
        AlignableVolume Volume_Top = new AlignableVolume("Volume_Top", aliVolumeList, rot, null,91);
        alignable_volumes.put("Volume_Top",Volume_Top);
        aliVolumeList.clear();

        // BOTTOM ////////
        
        //Now let's build the modules. 
        
        volumes.add("module_L1b_halfmodule_axial_sensor0_AV");
        volumes.add("module_L1b_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL1_Bot_AV",61);
        volumes.clear();

        volumes.add("module_L2b_halfmodule_axial_sensor0_AV");
        volumes.add("module_L2b_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL2_Bot_AV",62);
        volumes.clear();
        
        
        volumes.add("module_L3b_halfmodule_axial_sensor0_AV");
        volumes.add("module_L3b_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL3_Bot_AV",63);
        volumes.clear();
        
        
        volumes.add("module_L4b_halfmodule_axial_sensor0_AV");
        volumes.add("module_L4b_halfmodule_stereo_sensor0_AV");
        buildComposite(volumes,"ModuleL4_Bot_AV",64);
        volumes.clear();

        //Build front uChannel
        aliVolumeList.add(alignable_volumes.get("ModuleL1_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL2_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL3_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL4_Bot_AV")); 
        rot = (alignable_volumes.get("ModuleL1_Bot_AV")).getL2G().getRotation();
        AlignableVolume UChannelL14_Bot = new AlignableVolume("UChannelL14_Bot_AV", aliVolumeList, rot, null,80);
        alignable_volumes.put("UChannelL14_Bot_AV",UChannelL14_Bot);
        aliVolumeList.clear();

        
        //Build half sensors L5 - axial
        aliVolumeList.add(alignable_volumes.get("module_L5b_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L5b_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L5b_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L5_Bot_AV = new AlignableVolume("doublesensor_axial_L5_Bot_AV", aliVolumeList, rot, null,71);
        alignable_volumes.put("doublesensor_axial_L5_Bot_AV",doublesensor_axial_L5_Bot_AV);
        aliVolumeList.clear();


        //Build half sensors L5 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L5b_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L5b_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L5b_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L5_Bot_AV = new AlignableVolume("doublesensor_stereo_L5_Bot_AV", aliVolumeList, rot, null,72);
        alignable_volumes.put("doublesensor_stereo_L5_Bot_AV",doublesensor_stereo_L5_Bot_AV);
        aliVolumeList.clear();

        
        //Build module L5
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L5_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L5_Bot_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L5_Bot_AV")).getL2G().getRotation();
        AlignableVolume M5_Bot = new AlignableVolume("ModuleL5_Bot_AV", aliVolumeList, rot, null,65);
        alignable_volumes.put("ModuleL5_Bot_AV",M5_Bot);
        aliVolumeList.clear();

        
        //Build half sensors L6 - axial
        aliVolumeList.add(alignable_volumes.get("module_L6b_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L6b_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L6b_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L6_Bot_AV = new AlignableVolume("doublesensor_axial_L6_Bot_AV", aliVolumeList, rot, null,73);
        alignable_volumes.put("doublesensor_axial_L6_Bot_AV",doublesensor_axial_L6_Bot_AV);
        aliVolumeList.clear();


        //Build half sensors L6 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L6b_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L6b_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L6b_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L6_Bot_AV = new AlignableVolume("doublesensor_stereo_L6_Bot_AV", aliVolumeList, rot, null,74);
        alignable_volumes.put("doublesensor_stereo_L6_Bot_AV",doublesensor_stereo_L6_Bot_AV);
        aliVolumeList.clear();

        
        //Build module L6
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L6_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L6_Bot_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L6_Bot_AV")).getL2G().getRotation();
        AlignableVolume M6_Bot = new AlignableVolume("ModuleL6_Bot_AV", aliVolumeList, rot, null,66);
        alignable_volumes.put("ModuleL6_Bot_AV",M6_Bot);
        aliVolumeList.clear();


        //Build half sensors L7 - axial
        aliVolumeList.add(alignable_volumes.get("module_L7b_halfmodule_axial_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L7b_halfmodule_axial_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L7b_halfmodule_axial_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_axial_L7_Bot_AV = new AlignableVolume("doublesensor_axial_L7_Bot_AV", aliVolumeList, rot, null,75);
        alignable_volumes.put("doublesensor_axial_L7_Bot_AV",doublesensor_axial_L7_Bot_AV);
        aliVolumeList.clear();


        //Build half sensors L7 - stereo
        aliVolumeList.add(alignable_volumes.get("module_L7b_halfmodule_stereo_hole_sensor0_AV"));
        aliVolumeList.add(alignable_volumes.get("module_L7b_halfmodule_stereo_slot_sensor0_AV"));
        rot = (alignable_volumes.get("module_L7b_halfmodule_stereo_hole_sensor0_AV")).getL2G().getRotation();
        AlignableVolume doublesensor_stereo_L7_Bot_AV = new AlignableVolume("doublesensor_stereo_L7_Bot_AV", aliVolumeList, rot, null,6);
        alignable_volumes.put("doublesensor_stereo_L7_Bot_AV",doublesensor_stereo_L7_Bot_AV);
        aliVolumeList.clear();

        
        //Build module L7
        aliVolumeList.add(alignable_volumes.get("doublesensor_axial_L7_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("doublesensor_stereo_L7_Bot_AV"));
        rot = (alignable_volumes.get("doublesensor_axial_L7_Bot_AV")).getL2G().getRotation();
        AlignableVolume M7_Bot = new AlignableVolume("ModuleL7_Bot_AV", aliVolumeList, rot, null,67);
        alignable_volumes.put("ModuleL7_Bot_AV",M7_Bot);
        aliVolumeList.clear();

        //Build back UChannel
        aliVolumeList.add(alignable_volumes.get("ModuleL5_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL6_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("ModuleL7_Bot_AV"));
        rot = (alignable_volumes.get("ModuleL5_Bot_AV")).getL2G().getRotation();
        AlignableVolume UChannelL57_Bot = new AlignableVolume("UChannelL57_Bot_AV", aliVolumeList, rot, null,90);
        alignable_volumes.put("UChannelL57_Bot_AV",UChannelL57_Bot);
        aliVolumeList.clear();
        
        
        //Build Bot Volume
        aliVolumeList.add(alignable_volumes.get("UChannelL14_Bot_AV"));
        aliVolumeList.add(alignable_volumes.get("UChannelL57_Bot_AV"));
        rot = (alignable_volumes.get("UChannelL14_Bot_AV")).getL2G().getRotation();
        AlignableVolume Volume_Bot = new AlignableVolume("Volume_Bot", aliVolumeList, rot, null,91);
        alignable_volumes.put("Volume_Bot",Volume_Bot);
        aliVolumeList.clear();

        //Build Tracking Volume
        
        aliVolumeList.add(alignable_volumes.get("Volume_Top"));
        aliVolumeList.add(alignable_volumes.get("Volume_Bot"));
        rot = (alignable_volumes.get("Volume_Top")).getL2G().getRotation();
        AlignableVolume Tracker = new AlignableVolume("Tracker", aliVolumeList, rot, null,92);
        alignable_volumes.put("Tracker",Tracker);
        //Tracker.addLabel(11192);
        //Tracker.addLabel(11292);
        //Tracker.addLabel(11392);
        //Tracker.addLabel(12192);
        //Tracker.addLabel(12292);
        //Tracker.addLabel(12392);
        aliVolumeList.clear();

        for (Map.Entry<String,AlignableVolume> entry : alignable_volumes.entrySet()) {
            entry.getValue().Print();
        }
                
        try {
            writer = new FileWriter("AlignmentTree.json");
            BufferedWriter bf = new BufferedWriter(writer);
            bf.write("{\n");
            bf.write("\"AlignableStructures\" : {\n");
            

            int structures = alignable_volumes.size();
            int is = 0;
            for (Map.Entry<String,AlignableVolume> entry : alignable_volumes.entrySet()) {
                is++;
                entry.getValue().toJSON(bf);
                if (is!=structures)
                    bf.write(",\n");
            }
            bf.write("}\n");
            bf.write("}\n");
            bf.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Ref volume is always the first 
    public void buildComposite(List<String> volume_names, String name, int mpid)  {
        
        List<AlignableVolume> aliVolumeList = new ArrayList<AlignableVolume>();
        for (String volume_name : volume_names) {
            aliVolumeList.add(alignable_volumes.get(volume_name));
        }
        
        AlignableVolume ref_vol = aliVolumeList.get(0);
        IRotation3D rot = ref_vol.getL2G().getRotation(); 
        AlignableVolume avol = new AlignableVolume(name, aliVolumeList, rot, null,mpid); 
        alignable_volumes.put(name,avol);
        aliVolumeList.clear();
    }
    
    public AlignableVolume getAlignableVolume(String volume) { 
        return alignable_volumes.get(volume);
    }

    //Make the alignment Constraint file
    //Should make a list for only alignable sensors
    
    public void MakeAlignmentConstraintFile() {
        
        //Map for mapping structure to constraints
        Map<String,List<String> > constraintMap =  new HashMap< String,List<String> >(); 
        
        try {
            
            FileWriter writer = new FileWriter("mille_constraint_com.txt",false);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            //Get the base object
            for (Map.Entry<String,AlignableVolume> entry : alignable_volumes.entrySet()) {
                if (entry.getKey().contains("sensor0_AV"))
                    continue;
                AlignableVolume av = entry.getValue();
                if (!constraintMap.containsKey(av.getName())) {
                    //Make the constraints:
                    constraintMap.put(av.getName(),av.computeConstraint());
                }
            }
            
            //Print all the constraints
            List<String> constr_labels = new ArrayList<String>();
            constr_labels.add("!tu");
            constr_labels.add("!tv");
            constr_labels.add("!tw");
            constr_labels.add("!ru");
            constr_labels.add("!rv");
            constr_labels.add("!rw");
            
            bufferedWriter.write("!Constraint file for HPS MPII Alignment\n\n\n");
            for (Map.Entry< String,List<String>> me : constraintMap.entrySet()) {
                System.out.printf(me.getKey()+":\n"); 
                
                int iconstr = -1;
                for ( String constr : me.getValue()) {
                    iconstr +=1;
                    bufferedWriter.write("Constraint 0.0    !"+me.getKey()+" " +  constr_labels.get(iconstr)+"\n");
                    bufferedWriter.write(constr);
                    bufferedWriter.write("\n\n");
                }
            }
            bufferedWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}



interface IAlignableVolume {
    
    //IAlignableVolume(String name, ITranslation3D trans, IRotation3D rot, IAlignableVolume parent, List<IAlignableVolume> daughters);
    //public IAlignableVolume(String name, List<SurveyVolume> volumes, IRotation3D rot, IAlignableVolume parent);
}

class AlignableVolume implements IAlignableVolume {
    
    String   _name;
    int      _mpid;
    //Vector3D   _location;
    IRotation3D _rotation;
    ITransform3D _l2g;
    AlignableVolume _parent = null;
    List<AlignableVolume> _daughters = new ArrayList<AlignableVolume>();
    Map<String,Matrix> _cmatrix_map = new HashMap<String,Matrix>();
    List<Integer> derivativeLabels = new ArrayList<Integer>();
    
    //This has to become a static class
    private FrameToFrameDers f2fD = new FrameToFrameDers();
    
    public AlignableVolume(String name, Translation3D trans, Rotation3D rot,  AlignableVolume parent, List<AlignableVolume> daughters,int mpid) {}

    public AlignableVolume(String name, ITransform3D l2g_transform, AlignableVolume parent, List<AlignableVolume> daughters, int mpid) {
        _name = name;
        _parent = parent;
        _mpid   = mpid;
        if (daughters != null){
            for (AlignableVolume d : daughters) {
                _daughters.add(d);
            }
        }
        _l2g = new Transform3D(l2g_transform.getTranslation(),l2g_transform.getRotation()); 
        computeCMatrixMap();
    }
    
    //Decide the rotation
    public AlignableVolume(String name, List<AlignableVolume> AlignableVolumes, IRotation3D rot, AlignableVolume parent, int mpid) {
        _name = name;
        _parent = parent;
        _mpid   = mpid;
        for (AlignableVolume d : AlignableVolumes){
            _daughters.add(d);
            d.setParent(this);
            
        }
        //Compute the derivative label
        AlignableVolume d0  = _daughters.get(0);
        for (Integer l : d0.getLabels()) {
            Integer lab = l - d0.getMillepedeId() + mpid;
            derivativeLabels.add(lab);
        }
        
        _rotation =  new Rotation3D(rot);
        ITranslation3D trans = getCOM(AlignableVolumes);
        _l2g = new Transform3D(trans,rot);
        computeCMatrixMap();
    }
    
    
    public void setLabels(Integer tu, Integer tv, Integer tw, Integer ru, Integer rv, Integer rw) {
        derivativeLabels.add(tu);
        derivativeLabels.add(tv);
        derivativeLabels.add(tw);
        
        derivativeLabels.add(ru);
        derivativeLabels.add(rv);
        derivativeLabels.add(rw);
    }
    public void setLabels(List<Integer> ls, Integer mpid) {
        for (Integer l : ls){
            derivativeLabels.add(l+mpid);
        }
    }
    

    public void setLabels(List<Integer> ls) {
        for (Integer l : ls){
            derivativeLabels.add(l);
        }
    }

    public void addLabel(Integer l) {
        derivativeLabels.add(l);
    }

    public List<Integer> getLabels(){
        return derivativeLabels;
    }

    public int getMillepedeId() {
        return _mpid;
    }

    public String getName() {
        return _name;
    }
    
    public ITransform3D getL2G() {
        return _l2g;
    }

    public void setParent(AlignableVolume p) {
        _parent = p;

    }
    
    public AlignableVolume getParent() {
        return _parent;
    }

    public Matrix getCMatrix(String d_name) {
        return _cmatrix_map.get(d_name);
    }

    //Use a map string:matrix. Matching will be by name
    public void computeCMatrixMap() {
        
        //Get this transforms (component)
        Hep3Matrix Rctog = _l2g.getRotation().getRotationMatrix();
        Hep3Vector Tctog = _l2g.getTranslation().getTranslationVector();
        
        for (AlignableVolume d : _daughters) {
            
            //Sub-component to global frame
            Hep3Matrix Rsctog = d.getL2G().getRotation().getRotationMatrix();
            Hep3Vector Tsctog = d.getL2G().getTranslation().getTranslationVector();
            Matrix C_matrix = f2fD.getDerivative(Rsctog, Rctog, Tsctog, Tctog);
            _cmatrix_map.put(d.getName(), C_matrix);
        }

    }

    //Compute the constraint equations
    
    public List<String> computeConstraint() {
        
        List<String> constraints = new ArrayList<String>();
        constraints.add("");
        constraints.add("");
        constraints.add("");
        constraints.add("");
        constraints.add("");
        constraints.add("");
        
        for (AlignableVolume d : _daughters) {
            Matrix C_inverse = _cmatrix_map.get(d.getName()).inverse();
            List<Integer> labels = d.getLabels();
            //System.out.println(d.getName());
            //System.out.println(C_inverse.toString());
            //System.out.println(labels.toString());
            FormatConstraint(C_inverse, 6, constraints, labels, true);
            //System.out.println(constraints);
        }
        
        return constraints;
    }
    

    public ITranslation3D getCOM(List<AlignableVolume> avs) {
                
        Hep3Vector v3d = new BasicHep3Vector();
        
        for (AlignableVolume av : avs) {
            Hep3Vector v = av.getL2G().getTranslation().getTranslationVector();
            v3d = VecOp.add(v3d,v);
        }
        v3d = VecOp.mult(1./(float)avs.size(), v3d);
        
        ITranslation3D trans = new Translation3D(v3d);
        
        return trans;
    }

    public void toJSON(BufferedWriter bufferedWriter) {
        try {
            
            bufferedWriter.write("\""+_name+"\" : {\n");
            bufferedWriter.write("\"name\": \""+_name+"\",\n");
            if (_parent !=  null) 
                bufferedWriter.write("\"parent\":\""+_parent.getName()+"\",\n");
            else 
                bufferedWriter.write("\"parent\": \"\",\n");
            bufferedWriter.write("\"derivativeLabels\":"+derivativeLabels.toString()+",\n");
            bufferedWriter.write("\"daughters\":[");
            
            for (int id=0; id<_daughters.size();id++) {
                if (id != _daughters.size()-1)
                    bufferedWriter.write("\""+_daughters.get(id).getName()+"\",");
                else
                    bufferedWriter.write("\""+_daughters.get(id).getName()+"\"");
            }
            
            
            bufferedWriter.write("],\n");
            bufferedWriter.write("\"CMatrices\" : {");
            int ientry = 0;
            int matrices = _cmatrix_map.size();
            for (Map.Entry<String,Matrix> entry : _cmatrix_map.entrySet()) {
                ientry++;
                bufferedWriter.write("\""+entry.getKey()+"\" : ");
                bufferedWriter.write(Arrays.toString(entry.getValue().getRowPackedCopy()));
                if (ientry < matrices)
                    bufferedWriter.write(",");
            }
            bufferedWriter.write("}\n");
            bufferedWriter.write("}");
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Print() {
        System.out.println("#############");
        System.out.println("Alignable Volume: " + _name);
        System.out.println("Transform: " + _l2g.toString());
        if (_parent != null)
            System.out.println("Parent:" + _parent.getName());
        System.out.println("Daughters::" + _daughters.size());
        if (_daughters != null)
            for (AlignableVolume daughter : _daughters) {
                System.out.println("Daughter:" + daughter.getName()); 
            }
        for (Map.Entry<String,Matrix> entry : _cmatrix_map.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().print(3,6);
            
        }
        System.out.println(derivativeLabels.toString());
        System.out.println("#############");
        System.out.println("Constraints!");
        computeConstraint();
        System.out.println("..End of dump...");
    }


    //Format the constraint
    
    private void FormatConstraint(Matrix C_matrixInv, int nc, List<String> constraints, List<Integer> sc_labels, boolean MPIIFormat) {
        
        for (int ic = 0; ic < nc; ic++) {
            String appendix   = ""; //decide if to keep summing or last entry in the constraint.
            
            //The constraint is of the form 0 = sum_i=0 ^{n} C^{-1}a_i
            
            //Cmatrix is a 6x6
            for (int icol = 0 ; icol < 6; icol++) {
                
                //Get the current value
                String s_cnstr = constraints.get(ic);
                
                //Cmatrix coeff less than 5e-4 are ignored. Revisit? 
                
                if (Math.abs(C_matrixInv.get(ic,icol)) < 5e-4) 
                    continue;
                
                // get the rounded C matrix -1 entry rounded to 10e-4
                Double cnstr  = round(C_matrixInv.get(ic,icol),4);
                Integer sc_label = sc_labels.get(icol);
                
                int trans_type = (sc_label / 100) % 10;
                boolean isRot  = ((sc_label / 1000) % 10) == 1 ? false : true;
                
                if (s_cnstr != "")
                    if (!MPIIFormat)
                        s_cnstr += " + ";
                    else
                        s_cnstr += "\n";
                else
                    if (MPIIFormat)
                        s_cnstr += "\n\n";
                
                if (!MPIIFormat)
                    s_cnstr +=  String.valueOf(sc_label) + " * " + String.valueOf(cnstr);
                else
                    s_cnstr +=  String.valueOf(sc_label) + " " + String.valueOf(cnstr);
                
                //Set this in the list
                constraints.set(ic, s_cnstr);
            }//loop on C^-1 columns
        }//contraint loop
    }

    public static double round(double value, int places) {
        if (places < 0) 
            return value;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
