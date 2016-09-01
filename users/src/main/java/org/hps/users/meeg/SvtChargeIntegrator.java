package org.hps.users.meeg;

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

/**
 * @author Sho Uemura <meeg@slac.stanford.edu>
 *
 */
public class SvtChargeIntegrator {

	private static final double angleTolerance = 1e-4;
	private static final double burstModeNoiseEfficiency = 0.965;
	//below this number, the current is counted as zero.  
	private static double zeroPointThreshold = .1;

	/**
	 *
	 * @param args the command line arguments (requires a CSV run/file log file
	 * and a MYA dump file.)
	 */
	public static void main(String[] args) {

		Options options = new Options();
		options.addOption(new Option("r", false, "use per-run CSV log file (default is per-file)"));
		options.addOption(new Option("t", false, "use TI timestamp instead of Unix time (higher precision, but requires TI time offset in run DB)"));
		options.addOption(new Option("c", false, "get TI time offset from CSV log file instead of run DB"));
		options.addOption(new Option("e", true, "header error file"));
		options.addOption(new Option("d", false, "use 0.5 as the nominal svt position (rather than look in run DB for it)"));
		options.addOption(new Option("z", true, "use zeropoint value from a file"));
		
		
		final CommandLineParser parser = new PosixParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			throw new RuntimeException("Cannot parse.", e);
		}

		boolean perRun = cl.hasOption("r");
		boolean useTI = cl.hasOption("t");
		boolean useCrawlerTI = cl.hasOption("c");
		boolean useDefaultNominalSvtPos = cl.hasOption("d");
		
		Map<Integer, Long> runErrorMap = new HashMap<Integer, Long>();
		if (cl.hasOption("e")) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(cl.getOptionValue("e")));
				String line;
				System.err.println("header error file header: " + br.readLine()); //discard the first line
				while ((line = br.readLine()) != null) {
					String arr[] = line.split(" +");
					int run = Integer.parseInt(arr[1]);
					long errorTime = Long.parseLong(arr[4]);
					runErrorMap.put(run, errorTime);
					//                    System.out.format("%d %d\n", run, errorTime);
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		Map<Integer, Double> zeropointMap = new HashMap<Integer, Double>();
		Map<Integer, Double> attenuationMap = new HashMap<Integer, Double>();
		if(cl.hasOption("z")){
			try {
				BufferedReader br = new BufferedReader(new FileReader(cl.getOptionValue("z")));
				String line;
				System.err.println("zero point file header: " + br.readLine()); //discard the first line
				double zeropoint = 0;
				double attenuation = 0;
				while ((line = br.readLine()) != null) {
					String arr[] = line.split("[ \t]+");
					int run = Integer.parseInt(arr[0]);
					
					//if the zero point is unknown (ie, a very short run),
					// use the zeropoint of the previous run
					if(Double.parseDouble(arr[1]) != 0)
						zeropoint = Double.parseDouble(arr[1]);
					if(arr[2].equals("NaN"))
						attenuation = Double.NaN;
					else attenuation = Double.parseDouble(arr[2]);
					
					zeropointMap.put(run, zeropoint);
					attenuationMap.put(run, attenuation);
					//                    System.out.format("%d %d\n", run, errorTime);
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

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
			Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));

		try {
			BufferedReader br = new BufferedReader(new FileReader(cl.getArgs()[1]));
			String line;
			System.err.println("myaData header: " + br.readLine()); //discard the first line
			if (perRun) {
				if (cl.hasOption("e")) {
					System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\ttotalQ_noerror\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgatedQ_noerror\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tgoodQ_noerror");
				} else {
					if (cl.hasOption("z"))
						System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tzeropoint\tscale");
					else 
						System.out.println("run_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
				}
			} else {
				if (cl.hasOption("e")) {
					System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\ttotalQ_noerror\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgatedQ_noerror\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tgoodQ_noerror");
				} else {
					if (cl.hasOption("z"))
						System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom\tzeropoint\tscale");
					else
						System.out.println("run_num\tfile_num\tnominal_position\tnEvents\ttotalQ\ttotalQ_withbias\ttotalQ_atnom\tgatedQ\tgatedQ_withbias\tgatedQ_atnom\tgoodQ\tgoodQ_withbias\tgoodQ_atnom");
				}
			}

			int currentRun = 0;
			double nominalAngleTop = -999;
			double nominalAngleBottom = -999;
			String nominalPosition = null;
			long tiTimeOffset = 0;
			double efficiency = 0;
			SvtBiasConstantCollection svtBiasConstants = null;
			SvtMotorPositionCollection svtPositionConstants = null;
			SvtAlignmentConstant.SvtAlignmentConstantCollection alignmentConstants = null;
			Date date = null;
			Date lastDate;

			for (CSVRecord record : records) {
				int runNum = Integer.parseInt(record.get(0));
				if (useCrawlerTI) {
					if (perRun) {
						tiTimeOffset = Long.parseLong(record.get(12));
					} else {
						tiTimeOffset = Long.parseLong(record.get(8));
					}
					if (tiTimeOffset == 0) {
						continue;
					}
				}
				double zeropoint = zeropointMap.containsKey(runNum) ? zeropointMap.get(runNum) : 0;
				double attenuation = attenuationMap.containsKey(runNum) ? attenuationMap.get(runNum) : 1;
				
				if (runNum != currentRun) {
					if (useTI && !useCrawlerTI) {
						RunManager.getRunManager().setRun(runNum);
						if (!RunManager.getRunManager().runExists() || RunManager.getRunManager().getRunSummary().getTiTimeOffset() == null) {
							continue;
						}
						tiTimeOffset = RunManager.getRunManager().getRunSummary().getTiTimeOffset();
						if (tiTimeOffset == 0) {
							continue;
						}
					}

					try {
						DatabaseConditionsManager.getInstance().setDetector("HPS-EngRun2015-Nominal-v3", runNum);
					} catch (Exception ex) {
						continue;
					}

					try {
						svtBiasConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtBiasConstant.SvtBiasConstantCollection.class, "svt_bias_constants").getCachedData();
					} catch (Exception ex) {
						svtBiasConstants = null;
					}
					try {
						svtPositionConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtMotorPosition.SvtMotorPositionCollection.class, "svt_motor_positions").getCachedData();
					} catch (Exception ex) {
						svtPositionConstants = null;
					}
					if(!useDefaultNominalSvtPos){
						try {
							alignmentConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtAlignmentConstant.SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();
							for (final SvtAlignmentConstant constant : alignmentConstants) {
								switch (constant.getParameter()) {
								case 13100:
									nominalAngleTop = constant.getValue();
									break;
								case 23100:
									nominalAngleBottom = -constant.getValue();
									break;
								}
							}
							if (Math.abs(nominalAngleBottom) < angleTolerance && Math.abs(nominalAngleTop) < angleTolerance) {
								nominalPosition = "0pt5";
							} else if (Math.abs(nominalAngleBottom - 0.0033) < angleTolerance && Math.abs(nominalAngleTop - 0.0031) < angleTolerance) {
								nominalPosition = "1pt5";
							} else {
								nominalPosition = "unknown";
							}
						} catch (Exception ex) {
							alignmentConstants = null;
							nominalPosition = "unknown";
						}
					} else {
						nominalPosition = "0pt5";
						nominalAngleTop = 0;
						nominalAngleBottom = 0;
					}
					efficiency = burstModeNoiseEfficiency;
					SvtTimingConstants svtTimingConstants;
					try {
						svtTimingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
					} catch (Exception ex) {
						svtTimingConstants = null;
					}
					if (svtTimingConstants != null) {
						if (svtTimingConstants.getOffsetTime() > 27) {
							efficiency *= 2.0 / 3.0; // bad latency: drop 2 out of 6 trigger phases
						}// otherwise, we have good latency
					} else {
						efficiency = 0;
					}//no latency info in conditions: give up
					currentRun = runNum;
				}

				Date startDate, endDate;
				long firstTime, lastTime;//Unix time from head bank
				long firstTI, lastTI;//TI timestamp from TI bank

				if (perRun) {
					firstTime = Long.parseLong(record.get(7));
					lastTime = Long.parseLong(record.get(8));
					firstTI = Long.parseLong(record.get(10));
					lastTI = Long.parseLong(record.get(11));

				} else {
					firstTime = Long.parseLong(record.get(4));
					lastTime = Long.parseLong(record.get(5));
					firstTI = Long.parseLong(record.get(6));
					lastTI = Long.parseLong(record.get(7));
				}

				if (useTI) {
					if (firstTI == 0 || lastTI == 0) {
						continue;
					}
					startDate = new Date((firstTI + tiTimeOffset) / 1000000);
					endDate = new Date((lastTI + tiTimeOffset) / 1000000);
				} else {
					if (firstTime == 0 || lastTime == 0) {
						continue;
					}
					startDate = new Date(firstTime * 1000);
					endDate = new Date(lastTime * 1000);
				}

				Long errorTime = runErrorMap.get(runNum);
				Date errorDate = null;
				if (errorTime != null) {
					errorDate = new Date(errorTime / 1000000);
					boolean isGood = Math.abs(errorDate.getTime() - startDate.getTime()) < 10 * 60 * 60 * 1000; //10 hours
					if (!isGood && useTI) {
						errorDate = new Date((errorTime + tiTimeOffset) / 1000000);
						//                        boolean isPlusOffsetGood = Math.abs(errorDatePlusOffset.getTime() - startDate.getTime()) < 10 * 60 * 60 * 1000; //10 hours
						//                        System.out.format("%d, %d, %d: %s (good: %b), %s (good: %b)\n", runNum, errorTime, tiTimeOffset, errorDate, isGood, errorDatePlusOffset, isPlusOffsetGood);
					}
				}

				double totalCharge = 0;
				double totalChargeWithBias = 0;
				double totalChargeWithBiasAtNominal = 0;
				double totalChargeWithBiasAtNominalNoError = 0;
				double totalGatedCharge = 0;
				double totalGatedChargeWithBias = 0;
				double totalGatedChargeWithBiasAtNominal = 0;
				double totalGatedChargeWithBiasAtNominalNoError = 0;
				double totalGoodCharge = 0;
				double totalGoodChargeWithBias = 0;
				double totalGoodChargeWithBiasAtNominal = 0;
				double totalGoodChargeWithBiasAtNominalNoError = 0;
				br.mark(1000);
				
				//take into account the previous current sample.
				double prevCurrent = 0;
				while ((line = br.readLine()) != null) {
					String arr[] = line.split(" +");

					if (arr.length != 4) {
						throw new RuntimeException("this line is not correct.");
					}
					lastDate = date;
					date = dateFormat.parse(arr[0] + " " + arr[1]);
					if (date.before(startDate)) { //not in the file's time range yet; keep reading the file 
						continue;
					}

					double thisCurrent, livetime;
					if (arr[2].equals("<undefined>")) {
						thisCurrent = 0;
					} else {
						thisCurrent = Double.parseDouble(arr[2]);
					}
					if (arr[3].equals("<undefined>")) {
						livetime = 0;
					} else {
						livetime = Math.min(100.0, Math.max(0.0, Double.parseDouble(arr[3]))) / 100.0;
					}

					boolean biasGood = false;
					boolean positionGood = false;
					SvtBiasConstant biasConstant = null;
					if (svtBiasConstants != null) {
						biasConstant = svtBiasConstants.find(date);
						if (biasConstant == null && lastDate != null) {
							biasConstant = svtBiasConstants.find(lastDate);
						}
						if (biasConstant != null) {
							biasGood = true;
						}
					}
					SvtMotorPosition positionConstant = null;
					if (svtPositionConstants != null) {
						positionConstant = svtPositionConstants.find(date);
						if (positionConstant == null && lastDate != null) {
							positionConstant = svtPositionConstants.find(lastDate);
						}
						if (positionConstant != null && alignmentConstants != null) {
							//                    System.out.format("%f %f %f %f\n", positionConstant.getBottom(), nominalAngleBottom, positionConstant.getTop(), nominalAngleTop);
							if (Math.abs(positionConstant.getBottom() - nominalAngleBottom) < angleTolerance && Math.abs(positionConstant.getTop() - nominalAngleTop) < angleTolerance) {
								positionGood = true;
							}
						}
					}

					if (lastDate != null) {
						double biasDt = 0;
						double positionDt = 0;
						long dtStart = Math.max(startDate.getTime(), lastDate.getTime());
						long dtEnd = Math.min(date.getTime(), endDate.getTime());
						double dt = (dtEnd - dtStart) / 1000.0;
						double errorDt = 0;
						if (biasConstant != null) {
							long biasStart = Math.max(dtStart, biasConstant.getStart());
							long biasEnd = Math.min(dtEnd, biasConstant.getEnd());
							biasDt = Math.max(0, biasEnd - biasStart) / 1000.0;
							if (positionConstant != null) {
								long positionStart = Math.max(biasStart, positionConstant.getStart());
								long positionEnd = Math.min(biasEnd, positionConstant.getEnd());
								positionDt = Math.max(0, positionEnd - positionStart) / 1000.0;

								long errorEnd = positionStart;
								if (errorDate == null) {
									errorEnd = positionEnd;
								} else if (errorDate.getTime() > dtStart) {
									errorEnd = Math.min(positionEnd, errorDate.getTime());
								}
								errorDt = Math.max(0, errorEnd - positionStart) / 1000.0;
							}
						}
						//                        System.out.format("start %d end %d date %d lastDate %d current %f dt %f\n", startDate.getTime(), endDate.getTime(), date.getTime(), lastDate.getTime(), current, dt);
						
						// you should only use the attenuation and zeropoint file when using scalerS2b.
						// if no file is specified, then attenuation = 1 and zeropoint = 0.
						double current = (thisCurrent -zeropoint)/attenuation;
						
						
						totalCharge += dt * current; // nC
						totalGatedCharge += dt * current * livetime;
						totalGoodCharge += dt * current * livetime * efficiency;
						if (biasGood) {
							totalChargeWithBias += biasDt * current;
							totalGatedChargeWithBias += biasDt * current * livetime;
							totalGoodChargeWithBias += biasDt * current * livetime * efficiency;
							if (positionGood) {
								totalChargeWithBiasAtNominal += positionDt * current;
								totalGatedChargeWithBiasAtNominal += positionDt * current * livetime;
								totalGoodChargeWithBiasAtNominal += positionDt * current * livetime * efficiency;

								totalChargeWithBiasAtNominalNoError += errorDt * current;
								totalGatedChargeWithBiasAtNominalNoError += errorDt * current * livetime;
								totalGoodChargeWithBiasAtNominalNoError += errorDt * current * livetime * efficiency;
							}
						}
						prevCurrent = thisCurrent;
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
				if (perRun) {
					int nEvents = Integer.parseInt(record.get(9));
					if (cl.hasOption("e")) {
						System.out.format("%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalChargeWithBiasAtNominalNoError, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGatedChargeWithBiasAtNominalNoError, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal, totalGoodChargeWithBiasAtNominalNoError);
					} else {
						if(cl.hasOption("z"))
							System.out.format("%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal, zeropoint, attenuation);
						else 
							System.out.format("%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal);
					}
				} else {
					int fileNum = Integer.parseInt(record.get(1));
					int nEvents = Integer.parseInt(record.get(2));
					if (cl.hasOption("e")) {
						System.out.format("%d\t%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, fileNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalChargeWithBiasAtNominalNoError, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGatedChargeWithBiasAtNominalNoError, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal, totalGoodChargeWithBiasAtNominalNoError);
					} else {
						if(cl.hasOption("z"))
							System.out.format("%d\t%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, fileNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal, zeropoint, attenuation);
						else 
							System.out.format("%d\t%d\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", runNum, fileNum, nominalPosition, nEvents, totalCharge, totalChargeWithBias, totalChargeWithBiasAtNominal, totalGatedCharge, totalGatedChargeWithBias, totalGatedChargeWithBiasAtNominal, totalGoodCharge, totalGoodChargeWithBias, totalGoodChargeWithBiasAtNominal);
					}
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(SvtChargeIntegrator.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		System.err.println("SvtChargeIntegrator <CSV log file> <MYA dump file>");
		formatter.printHelp("Need to adhere to these options", options);
	}
}
