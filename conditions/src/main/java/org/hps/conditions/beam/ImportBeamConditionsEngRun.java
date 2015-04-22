package org.hps.conditions.beam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.beam.BeamConditions.BeamConditionsCollection;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * <p>
 * Import beam measurements into the database from a text file.
 * <p>
 * This has the format:<br/>
 * run current x y
 * <p>
 * The beam energy is hard-coded to 1.92 GeV for now, pending updates with better information.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ImportBeamConditionsEngRun {

    /**
     * Nominal beam energy for Eng Run.
     */
    private static final double BEAM_ENERGY = 1.92;

    /**
     * Null value from the input text file.
     */
    private static final double NULL_VALUE = -999.0;

    /**
     * Import the Eng Run beam conditions from a text file.
     *
     * @param args the argument list
     * @throws Exception if there is an error importing the text file
     */
    public static void main(final String[] args) throws Exception {

        if (args.length == 0) {
            throw new RuntimeException("missing file list argument");
        }

        final String fileName = args[0];
        if (!new File(fileName).exists()) {
            throw new IOException("The file " + fileName + " does not exist.");
        }

        final Map<Integer, BeamConditions> beamMap = new LinkedHashMap<Integer, BeamConditions>();

        final BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = reader.readLine()) != null) {

            final String[] values = line.split(" ");

            final BeamConditions beam = new BeamConditions();

            setValue(beam, "current", values[1]);
            setValue(beam, "position_x", values[2]);
            setValue(beam, "position_y", values[3]);

            if (beam.getFieldValue("current") == null) {
                // Use null value to indicate beam was not measured.
                beam.setFieldValue("energy", null);
            } else if ((Double) beam.getFieldValue("current") == 0) {
                // Use zero for no beam.
                beam.setFieldValue("energy", 0);
            } else {
                // Use nominal beam energy from ECAL commissioning.
                beam.setFieldValue("energy", BEAM_ENERGY);
            }

            beamMap.put(Integer.parseInt(values[0]), beam);
        }
        reader.close();

        System.out.println("printing beam conditions parsed from " + fileName + " ...");
        System.out.println("run id current x y energy");
        for (final Entry<Integer, BeamConditions> entry : beamMap.entrySet()) {
            System.out.print(entry.getKey() + " ");
            System.out.println(entry.getValue() + " ");
        }

        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.ALL);

        for (final Entry<Integer, BeamConditions> entry : beamMap.entrySet()) {
            final int run = entry.getKey();
            final BeamConditions beam = entry.getValue();
            final int collectionId = manager.addCollection("beam", "ImportBeamConditionsEngRun created collection by "
                    + System.getProperty("user.name"), null);
            final ConditionsRecord record = new ConditionsRecord(collectionId, run, run, "beam", "beam",
                    "imported from HPS_Runs.pdf", "eng_run");
            System.out.println(record);
            System.out.println(beam);
            final BeamConditionsCollection collection = new BeamConditionsCollection();
            collection.add(beam);
            manager.insertCollection(collection);
            record.insert();
        }
        manager.closeConnection();
    }

    /**
     * Set the value of the beam current.
     *
     * @param beam the beam conditions object
     * @param fieldName the name of the field
     * @param rawValue the raw value from the text file
     */
    static void setValue(final BeamConditions beam, final String fieldName, final String rawValue) {
        final double value = Double.parseDouble(rawValue);
        if (value != NULL_VALUE) {
            beam.setFieldValue(fieldName, value);
        } else {
            beam.setFieldValue(fieldName, null);
        }
    }

    /**
     * Class should not be instantiated.
     */
    private ImportBeamConditionsEngRun() {
    }
}
