package org.hps.conditions.beam;

import hep.aida.IAnalysisFactory;
import hep.aida.ITree;
import hep.aida.ITuple;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.beam.BeamConditions.BeamConditionsCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Load beam conditions for every run from the ECAL commissioning.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class BeamConditionsTest extends TestCase {

    /**
     * List of runs from the ECAL Eng Run.
     */
    static int runs[] = new int[] {2713, 2723, 2726, 2728, 2730, 2741, 2750, 2753, 2790, 2795, 2811, 2823, 2825, 2826,
            2837, 2847, 2888, 2889, 2891, 2915, 2916, 3128, 3129, 3151, 3374, 3464, 2814, 2815, 3183, 3206, 3207, 3215,
            3219, 3220, 3221, 3222, 3223, 3224, 3225, 3226, 3227, 3228, 3229, 3230, 3231, 3232, 3234, 3235, 3236, 3237,
            3238, 3240, 3241, 3242, 3244, 3245, 3246, 3247, 3248, 3249, 3250, 3251, 3254, 3255, 3256, 3257, 3258, 3259,
            3260, 3261, 3263, 3264, 3265, 3266, 3267, 3268, 3269, 3274, 3275, 3286, 3287, 3288, 3289, 3290, 3291, 3292,
            3293, 3294, 3295, 3312, 3313, 3314, 3315, 3316, 3317, 3318, 3319, 3320, 3321, 3322, 3323, 3324, 3325, 3326,
            3327, 3330, 3335, 3336, 3337, 3338, 3339, 3340, 3341, 3343, 3344, 3345, 3346, 3347, 3348, 3393, 3394, 3395,
            3396, 3398, 3399, 3401, 3402, 3417, 3418, 3419, 3420, 3421, 3422, 3423, 3424, 3426, 3427, 3428, 3429, 3430,
            3431, 3434, 3435, 3436, 3437, 3438, 3441, 3444, 3445, 3446, 3447, 3448, 3449, 3450, 3451, 3452, 3453, 3454,
            3455, 3456, 3457, 3458, 3459, 3461, 3462, 3463, 3216, 2926, 2935, 2934, 2937};

    /**
     * Write out an AIDA tuple with the beam conditions.
     *
     * @param beamConditions the beam conditions
     */
    private static void writeBeamTuple(final Map<Integer, BeamConditions> beamConditions) {
        final File dir = new TestOutputFile(BeamConditionsTest.class.getSimpleName());
        dir.mkdir();
        final IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        final ITree tree;
        try {
            tree = analysisFactory.createTreeFactory().create(dir.getPath() + File.separator + "BeamTuple.aida", "xml",
                    false, true);
        } catch (IllegalArgumentException | IOException e) {
            throw new RuntimeException(e);
        }
        final ITuple tuple = analysisFactory.createTupleFactory(tree).create("/Beam Tuple", "Beam Tuple",
                "int run, double current, position_x, position_y, energy");
        tuple.start();
        for (final Entry<Integer, BeamConditions> entry : beamConditions.entrySet()) {
            tuple.addRow();
            Double current = entry.getValue().getCurrent();
            if (current == null) {
                current = 0.;
            }
            Double positionX = entry.getValue().getPositionX();
            if (positionX == null) {
                positionX = 0.;
            }
            Double positionY = entry.getValue().getPositionY();
            if (positionY == null) {
                positionY = 0.;
            }
            Double energy = entry.getValue().getEnergy();
            if (energy == null) {
                energy = 0.;
            }
            tuple.fill(0, (int) entry.getKey());
            tuple.fill(1, (double) current);
            tuple.fill(2, (double) positionX);
            tuple.fill(3, (double) positionY);
            tuple.fill(4, (double) energy);
            tuple.next();
        }
        try {
            tree.commit();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test the beam conditions.
     */
    public void testBeamConditions() {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.SEVERE);
        System.out.println("run id current position_x position_y energy");
        final Map<Integer, BeamConditions> beamConditions = new LinkedHashMap<Integer, BeamConditions>();
        for (final int run : runs) {
            try {
                manager.setDetector("HPS-ECalCommissioning", run);
            } catch (final ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
            final BeamConditionsCollection beamCollection = manager.getCachedConditions(BeamConditionsCollection.class,
                    "beam").getCachedData();
            final BeamConditions beam = beamCollection.get(0);
            System.out.print(run + " ");
            System.out.print(beam.getRowId() + " ");
            System.out.print(beam.getCurrent() + " ");
            System.out.print(beam.getPositionX() + " ");
            System.out.print(beam.getPositionY() + " ");
            System.out.print(beam.getEnergy());
            System.out.println();
            beamConditions.put(run, beam);
        }
        writeBeamTuple(beamConditions);
    }
}
