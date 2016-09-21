package org.hps.users.spaul;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtBiasConstant;
import org.hps.conditions.svt.SvtBiasConstant.SvtBiasConstantCollection;
import org.hps.conditions.svt.SvtMotorPosition;
import org.hps.conditions.svt.SvtMotorPosition.SvtMotorPositionCollection;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.rundb.RunManager;
import org.hps.users.meeg.SvtChargeIntegrator;

/**
 * Calculate the zeropoint value for each run from a myadump of scalerS2b
 * @author Sebouh Paul
 *
 */
public class ScalerAttenuationCalculator {
	//after the value of scalerS2b drops below this value, (at a beam trip)
	// take 10 samples, and use the 10th sample is the zero point value
	//static double zp_threshold = 500;

	/**
	 *
	 * @param args the command line arguments (requires a CSV run/file log file
	 * and a MYA dump file.)
	 */
	public static void main(String[] args) {

		Options options = new Options();
		options.addOption(new Option("z", true, "use zeropoint value from a file"));
		
		final CommandLineParser parser = new PosixParser();
		CommandLine cl = null;
		
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			throw new RuntimeException("Cannot parse.", e);
		}

		

		Map<Integer, Long> runErrorMap = new HashMap<Integer, Long>();
		

		if (cl.getArgs().length != 2) {
			printUsage(options);
			return;
		}

		List<CSVRecord> records = null;
		try {
			FileReader reader = new FileReader(cl.getArgs()[0]);
			final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

			records = csvParser.getRecords();

			csvParser.close();
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ScalerAttenuationCalculator.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(ScalerAttenuationCalculator.class.getName()).log(Level.SEVERE, null, ex);
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

		Map<Integer, Double> zeropointMap = new HashMap<Integer, Double>();
		if(cl.hasOption("z")){
			try {
				BufferedReader br = new BufferedReader(new FileReader(cl.getOptionValue("z")));
				String line;
				System.err.println("zero point file header: " + br.readLine()); //discard the first line
				double zeropoint = 0;
				while ((line = br.readLine()) != null) {
					String arr[] = line.split("[ \t]+");
					int run = Integer.parseInt(arr[0]);
					
					//if the zero point is unknown (ie, a very short run),
					// use the zeropoint of the previous run
					if(Double.parseDouble(arr[1]) != 0)
						zeropoint = Double.parseDouble(arr[1]);
					
					zeropointMap.put(run, zeropoint);
					//                    System.out.format("%d %d\n", run, errorTime);
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(cl.getArgs()[1]));
			String line;
			System.err.println("myaData header: " + br.readLine()); //discard the first line
			/*if (perRun) {
				if (cl.hasOption("e")) {
					System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\ttotalQ_noerror\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgatedQ_noerror\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tgoodQ_noerror");
				} else {
					System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
				}
			} else {
				if (cl.hasOption("e")) {
					System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\ttotalQ_noerror\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgatedQ_noerror\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tgoodQ_noerror");
				} else {
					System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
				}
			}*/
			System.out.println("run_num\tzeropoint\tattenuation");

			
			Date date = null;
			Date lastDate;

			for (CSVRecord record : records) {
				int runNum = Integer.parseInt(record.get(0));
				

				

				Date startDate, endDate;
				long firstTime, lastTime;//Unix time from head bank
				long firstTI, lastTI;//TI timestamp from TI bank

				
					firstTime = Long.parseLong(record.get(7));
					lastTime = Long.parseLong(record.get(8));
					firstTI = Long.parseLong(record.get(10));
					lastTI = Long.parseLong(record.get(11));

				

				
					if (firstTime == 0 || lastTime == 0) {
						continue;
					}
					startDate = new Date(firstTime * 1000);
					endDate = new Date(lastTime * 1000);
				

				

				
				br.mark(1000);
				
				
				
				
				double attenuation_count = 0, attenuation_sum = 0;
				
				//only change the zeropoint if there 
				double zeropoint = zeropointMap.get(runNum);
				
				double crossing_time = 0;
				
				double  bpm_threshold = 40;
				
				while ((line = br.readLine()) != null) {
					String arr[] = line.split(" +");

					if (arr.length != 5) {
						throw new RuntimeException("this line is not correct.");
					}
					lastDate = date;
					date = dateFormat.parse(arr[0] + " " + arr[1]);
					if (date.before(startDate)) { //not in the file's time range yet; keep reading the file 
						continue;
					}

					double scaler, livetime, bpm;
					if (arr[2].equals("<undefined>")) {
						scaler = 0;
					} else {
						scaler = Double.parseDouble(arr[2]);
					}
					
					if (arr[3].equals("<undefined>")) {
						bpm = 0;
					} else {
						bpm = Double.parseDouble(arr[3]);
					}

					

					if (lastDate != null) {
						long dtStart = Math.max(startDate.getTime(), lastDate.getTime());
						long dtEnd = Math.min(date.getTime(), endDate.getTime());
						double dt = (dtEnd - dtStart) / 1000.0;
						
						// average only the events when the bpm has been above a threshold for
						// at least 15 seconds
						if(bpm > bpm_threshold && crossing_time == 0)
							crossing_time =date.getTime();
						else if(bpm < bpm_threshold)
							crossing_time = 0;
						else if(crossing_time != 0 && date.getTime() > crossing_time+15000 && bpm > bpm_threshold){
							attenuation_sum+= (scaler-zeropoint)/bpm;
							attenuation_count ++;
						}
						
						
					}
					if (date.after(endDate)) {//this is the last interval overlapping the file's time range; backtrack so this line will be read again for the next file
						date = lastDate;
						try {
							br.reset();
						} catch (IOException e) {
						}
						break;
					}
					br.mark(1000);
					//current = nextCurrent;
				}
				System.out.printf("%d\t%f\t%f\n", runNum, zeropoint, attenuation_sum/attenuation_count);
				
			}
		} catch (Exception ex) {
			Logger.getLogger(ScalerAttenuationCalculator.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		System.err.println("SvtChargeIntegrator <CSV log file> <MYA dump file>");
		formatter.printHelp("Need to adhere to these options", options);
	}
}

