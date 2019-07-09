package org.hps.conditions.beam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.conditions.beam.BeamPosition.BeamPositionCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

import junit.framework.TestCase;

/**
 * 
 * Test that beam positions from conditions database match 
 * hard-coded map copied from reconstruction driver.
 * 
 * @author jeremym
 */
public class BeamPositionTest extends TestCase {

    private Map<Integer, double[]> beamPositionMap = null;
    
    public void setUp() {
        beamPositionMap = new LinkedHashMap<Integer, double[]>();

        // Values copied from HpsReconParticleDriver.java
        // 20190111 Values from Matt Solt's analysis of tuple output from Pass2 (note copied here)
        beamPositionMap.put(7629, new double[]{-4.17277481802, -0.12993997991, -0.0853344591497});
        beamPositionMap.put(7630, new double[]{-4.14431582882, -0.131667930281, -0.0818403429116});
        beamPositionMap.put(7636, new double[]{-4.21047915591, -0.133674849016, -0.089578068184});
        beamPositionMap.put(7637, new double[]{-4.24645776407, -0.101418909471, -0.0910518478041});
        beamPositionMap.put(7644, new double[]{-4.17901911124, -0.130285615341, -0.0822733438671});
        beamPositionMap.put(7653, new double[]{-4.17260490817, -0.131034318083, -0.0791072417695});
        beamPositionMap.put(7779, new double[]{-4.1787064368, -0.14063872959, -0.0964567926519});
        beamPositionMap.put(7780, new double[]{-4.1728601751, -0.138420200972, -0.0946284667682});
        beamPositionMap.put(7781, new double[]{-4.16985657657, -0.156226289295, -0.0968035162023});
        beamPositionMap.put(7782, new double[]{-4.18257346152, -0.140219484074, -0.109736506045});
        beamPositionMap.put(7783, new double[]{-4.18257346152, -0.140219484074, -0.109736506045});
        beamPositionMap.put(7786, new double[]{-4.12972261841, -0.166377573933, -0.0970139372611});
        beamPositionMap.put(7795, new double[]{-4.21859751579, -0.144244767944, -0.0853166371595});
        beamPositionMap.put(7796, new double[]{-4.20194805564, -0.143712797497, -0.0814142818837});
        beamPositionMap.put(7798, new double[]{-4.23579296792, -0.145096101419, -0.0763395379254});
        beamPositionMap.put(7799, new double[]{-4.21915161348, -0.150063795069, -0.0747834605672});
        beamPositionMap.put(7800, new double[]{-4.21341596102, -0.148070389758, -0.0759533031441});
        beamPositionMap.put(7801, new double[]{-4.22235421469, -0.152015276101, -0.0771084658072});
        beamPositionMap.put(7803, new double[]{-4.34166909052, -0.142164101651, -0.0737517199669});
        beamPositionMap.put(7804, new double[]{-4.32755215514, -0.142501982627, -0.0736984742692});
        beamPositionMap.put(7805, new double[]{-4.34001918433, -0.14128234629, -0.0719415420433});
        beamPositionMap.put(7807, new double[]{-4.2913881367, -0.146538069491, -0.0713110421539});
        beamPositionMap.put(7947, new double[]{-4.11570919432, -0.0910859069129, -0.115216742215});
        beamPositionMap.put(7948, new double[]{-4.15441978567, -0.0686135478054, -0.129060986622});
        beamPositionMap.put(7949, new double[]{-4.15625618167, -0.0732414149365, -0.133909636515});
        beamPositionMap.put(7953, new double[]{-4.1247535341, -0.0498468870412, -0.136667869602});
        beamPositionMap.put(7962, new double[]{-4.18892745552, -0.0888237919098, -0.133084782275});
        beamPositionMap.put(7963, new double[]{-4.21544617772, -0.0746121484095, -0.138791648195});
        beamPositionMap.put(7964, new double[]{-4.22151568434, -0.0767100152439, -0.138029976144});
        beamPositionMap.put(7965, new double[]{-4.21982078088, -0.0633124399662, -0.13548854});
        beamPositionMap.put(7966, new double[]{-4.22967441763, -0.074293601613, -0.136050581329});
        beamPositionMap.put(7968, new double[]{-4.24234161374, -0.0898360310379, -0.1348398567});
        beamPositionMap.put(7969, new double[]{-4.27252423462, -0.0870002501041, -0.133719459818});
        beamPositionMap.put(7970, new double[]{-4.26842346064, -0.0857240792101, -0.116971022199});
        beamPositionMap.put(7972, new double[]{-4.33363167982, 0.0345877733342, -0.100637477098});
        beamPositionMap.put(7976, new double[]{-4.28593685326, 0.0248070264018, -0.102808747635});
        beamPositionMap.put(7982, new double[]{-4.29646985597, 0.0127599017288, -0.101991281778});
        beamPositionMap.put(7983, new double[]{-4.26170058486, 0.0189046639217, -0.107073001527});
        beamPositionMap.put(7984, new double[]{-4.27436464212, 0.0245269396206, -0.108859729825});
        beamPositionMap.put(7985, new double[]{-4.27834263863, 0.0236149343493, -0.114436070246});
        beamPositionMap.put(7986, new double[]{-4.27263205142, 0.0290293298289, -0.129868804995});
        beamPositionMap.put(7987, new double[]{-4.26816324002, 0.021546891022, -0.134582348971});
        beamPositionMap.put(7988, new double[]{-4.25916023097, 0.0288738200402, -0.134675209766});
        beamPositionMap.put(8025, new double[]{-4.26772080184, -0.0374515885847, -0.111794925212});
        beamPositionMap.put(8026, new double[]{-4.26342349557, -0.0130037595274, -0.108269292234});
        beamPositionMap.put(8027, new double[]{-4.26228873413, -0.00643925463781, -0.10675932599});
        beamPositionMap.put(8028, new double[]{-4.24130730501, -0.00747575221052, -0.102814820173});
        beamPositionMap.put(8029, new double[]{-4.22872103491, -0.00447916152702, -0.105249943381});
        beamPositionMap.put(8030, new double[]{-4.23900754195, -0.000357473510209, -0.106146355534});
        beamPositionMap.put(8031, new double[]{-4.20496275068, 0.000100290515539, -0.104054377677});
        beamPositionMap.put(8039, new double[]{-4.22124716174, -0.0011390722464, -0.101544332935});
        beamPositionMap.put(8040, new double[]{-4.2205668431, -0.00181385356273, -0.102991182594});
        beamPositionMap.put(8041, new double[]{-4.28721393166, 0.00197335438837, -0.101551648105});
        beamPositionMap.put(8043, new double[]{-4.22920389672, -0.000848565041975, -0.0995558470643});
        beamPositionMap.put(8044, new double[]{-4.22870291956, -0.00175258909226, -0.0988413432517});
        beamPositionMap.put(8045, new double[]{-4.20258011807, -0.00866029673502, -0.100045336346});
        beamPositionMap.put(8046, new double[]{-4.22290557247, -0.00729779373861, -0.100652118073});
        beamPositionMap.put(8047, new double[]{-4.14601676145, -0.0202991409332, -0.105998899874});
        beamPositionMap.put(8048, new double[]{-4.20679634717, -0.00924325674287, -0.106248419021});
        beamPositionMap.put(8049, new double[]{-4.21496917922, -0.00500571957867, -0.10725973208});
        beamPositionMap.put(8051, new double[]{-4.2126457536, -0.00153038672528, -0.111092588541});
        beamPositionMap.put(8055, new double[]{-4.28341733723, 0.0206565632476, -0.115598253441});
        beamPositionMap.put(8057, new double[]{-4.2882639213, 0.00480487421616, -0.104316741434});
        beamPositionMap.put(8058, new double[]{-4.29698307957, 0.00818999458705, -0.109941003868});
        beamPositionMap.put(8059, new double[]{-4.28762465865, -0.00153129299044, -0.111814005204});
        beamPositionMap.put(8072, new double[]{-4.13924541982, 0.0180721454354, -0.113772583512});
        beamPositionMap.put(8073, new double[]{-4.15278781506, -0.00108521877967, -0.112893566712});
        beamPositionMap.put(8074, new double[]{-4.15571729252, 0.00618781078807, -0.113017354596});
        beamPositionMap.put(8075, new double[]{-4.1733104989, -0.00486744222345, -0.112424119993});
        beamPositionMap.put(8077, new double[]{-4.20683436964, 0.0110201050856, -0.109299859828});
        beamPositionMap.put(8085, new double[]{-4.13876392508, 0.0439497207201, -0.0903205833013});
        beamPositionMap.put(8086, new double[]{-4.16507539815, 0.0597982603734, -0.0910001508689});
        beamPositionMap.put(8087, new double[]{-4.20213132671, 0.0396348079161, -0.0784607661075});
        beamPositionMap.put(8088, new double[]{-4.23374437206, 0.0741295264942, -0.0838311072439});
        beamPositionMap.put(8090, new double[]{-4.18462908099, 0.0224605407948, -0.078660407208});
        beamPositionMap.put(8092, new double[]{-4.23292219117, 0.00789727246464, -0.0745098357754});
        beamPositionMap.put(8094, new double[]{-4.21308308691, 0.00356660582853, -0.072071620408});
        beamPositionMap.put(8095, new double[]{-4.20185037174, 0.00805359635246, -0.0747092315702});
        beamPositionMap.put(8096, new double[]{-4.23251278514, 0.00613811160073, -0.0741564828197});
        beamPositionMap.put(8097, new double[]{-4.19022011872, 0.00740408472403, -0.0735952313026});
        beamPositionMap.put(8098, new double[]{-4.20923479595, 0.00408775878779, -0.0755429310062});
        beamPositionMap.put(8099, new double[]{-4.20773101369, 0.0051498614277, -0.0797183115611});
    }
    
    /**
     * Perform a run-by-run comparison between the conditions values and the data map.
     * @throws ConditionsNotFoundException If there is a problem initializing the conditions system
     */
    public void testBeamPositions() throws ConditionsNotFoundException {
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        for (Entry<Integer, double[]> entry : this.beamPositionMap.entrySet()) {
            final Integer runNumber = entry.getKey();
            final double[] beamPositionArr = entry.getValue();
            mgr.setDetector("HPS-dummy-detector", runNumber);
            final BeamPositionCollection beamPositionCond = 
                    mgr.getCachedConditions(BeamPositionCollection.class, "beam_positions").getCachedData();
            final BeamPosition beamPosition = beamPositionCond.get(0);
            System.out.println("Testing run " + runNumber);
            assertEquals("Beam Z position does not match.", beamPositionArr[0], beamPosition.getPositionZ());
            assertEquals("Beam X position does not match.", beamPositionArr[1], beamPosition.getPositionX());
            assertEquals("Beam Y position does not match.", beamPositionArr[2], beamPosition.getPositionY());
        }
    }
}

