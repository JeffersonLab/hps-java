package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task to add ROOT histograms from the reconstruction stations using the hadd utility.
 * 
 * @author jeremym
 */
final class PlotAddTask extends TimerTask {

    /**
     * Package logger.
     */
    private static Logger LOGGER = Logger.getLogger(PlotAddTask.class.getPackage().getName());
    
    /**
     * Reference to the online reconstruction server.
     */
    private final Server server;
    
    /**
     * Target output file.
     */
    private final File targetFile;
           
    /**
     * Command for adding ROOT plots.
     */
    private static final String HADD = "hadd";
    
    /**
     * Number of CPUs to use when running hadd (hard-coded to 4).
     */
    private Integer threads = 1;
    
    /**
     * Verbosity of the ROOT "hadd" command.
     */
    private Integer verbosity = 99;
   
    /**
     * List of station IDs with directories to look for plot files.
     * 
     * If this is empty then all station IDs will be used.
     */
    private List<Integer> ids = new ArrayList<Integer>();
    
    /**
     * Whether to delete intermediate plot files when done adding them.
     */
    private boolean delete = false;
    
    /**
     * Whether to append to an existing ROOT target file.
     */
    private boolean append = false;
        
    /**
     * Class constructor.
     * @param server Reference to the online reconstruction server
     * @param targetFile The target output file
     * @param delete True to delete existing station plot files
     */
    PlotAddTask(Server server, File targetFile, boolean delete, boolean append) {
        this.server = server;        
        this.targetFile = targetFile;
        this.delete = delete;
        this.append = append;
        
        // Check that the hadd command exists.
        checkHadd();
    }
   
    /**
     * Set number of CPU threads to use when running hadd.
     * @param threads Number of CPU threads to use when running hadd
     */
    void setThreadCount(int threads) {
        this.threads = threads;
    }
    
    /**
     * Set verbosity level of hadd command (0-99).
     * @param verbosity Verbosity of the hadd command
     */
    void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }
    
    /**
     * Add a list of station IDs for adding plots.
     * @param ids The list of station IDs for adding plots
     */
    void addStationIDs(List<Integer> ids) {
        this.ids.addAll(ids);
    }

    /**
     * Check that the hadd command runs okay.
     */
    private void checkHadd() {
        ProcessBuilder pb = new ProcessBuilder(HADD);
        try {
            Process p = pb.start();
            p.waitFor();            
        } catch (IOException e) {
            throw new RuntimeException("The hadd command was not found (did you source the thisroot.sh setup script?).", e);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
                
    /**
     * Find ROOT files using pattern matching.
     */
    static class RootFileFinder extends SimpleFileVisitor<Path> {
                
        List<File> files = new ArrayList<File>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.root");
        
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                // Ignore temp files written by the PlotDriver.
                if (!name.toFile().getName().startsWith("tmp.")) {
                    files.add(file.toFile());
                    LOGGER.info("Found matching file: " + name.getFileName().toString());
                } /*else {
                    LOGGER.info("Ignored temp file: " + name.getFileName());
                }*/
            }
        }
        
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            //LOGGER.info("Visiting: " + file.toFile().toPath());
            find(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) {
            find(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException e) {
            LOGGER.log(Level.WARNING, "Error visiting file: " + file.toFile().getPath(), e);
            return FileVisitResult.CONTINUE;
        }
        
        List<File> getFiles() {
            return this.files;
        }
    }
    
    /**
     * Get the list of ROOT files.
     * @return The list of ROOT files
     * @throws IOException If there is a problem getting the list of files
     */
    private List<File> findRootFiles(List<File> dirs) throws IOException {
        RootFileFinder finder = new RootFileFinder();
        for (File dir : dirs) {
            LOGGER.info("Checking for plot files in dir: " + dir.getPath());
            Files.walkFileTree(dir.toPath(), finder);
        }
        return finder.getFiles();
    }
    
    /**
     * Add plots and write to a target file.
     * @param target The target output file
     * @param inFiles The list of input files
     * @throws IOException If there is a problem adding the plots
     * @throws InterruptedException If the method is interrupted
     */
    private void addPlots(File target, List<File> inFiles) throws IOException, InterruptedException { 
        if (inFiles.size() > 0) {
            LOGGER.info("Adding plots with target <" + target.getPath() + "> and plot files: " + inFiles.toString());
            List<File> files = new ArrayList<File>(inFiles);
            
            // If target exists then move it to a backup file.
            if (target.exists()) {
                File oldTarget = new File(target.getPath() + ".old");
                File newTarget = new File(target.getPath());
                LOGGER.info("Moving existing target: " + oldTarget);
                newTarget.renameTo(oldTarget);
                // If appending to existing output then include old target in hadd command.
                if (this.append) {
                    LOGGER.info("Including old target in hadd: " + oldTarget.getPath());
                    files.add(0, oldTarget);
                }
            }

            /*
            Usage: hadd [-f[fk][0-9]] [-k] [-T] [-O] [-a]
            [-n maxopenedfiles] [-cachesize size] [-j ncpus] [-v [verbosity]]
            targetfile source1 [source2 source3 ...]
            */

            List<String> cmd = new ArrayList<String>();
            cmd.add("hadd");
            cmd.add("-j" + this.threads.toString());
            cmd.add("-v" + this.verbosity.toString());
            cmd.add(target.getPath());
            for (File file : files) {
                cmd.add(file.getPath());
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);        
            LOGGER.info("Running hadd command: " + String.join(" ", cmd));            
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String readline;
            while ((readline = reader.readLine()) != null) {
                sb.append(readline + '\n');
            }            
            LOGGER.fine("hadd output: " + '\n' + sb.toString());
            
            p.waitFor();
            LOGGER.fine("hadd is done!");
            if (!target.exists()) {
                throw new IOException("Failed to create new plot file: " + target.getPath());
            } else {
                LOGGER.info("Wrote new plot file: " + target.getPath());
            }
        } else {
            LOGGER.warning("No plot files found so hadd did not run!");
        }
    }

    /**
     * Run the plot task to add files.
     */
    @Override
    public void run() {
        try {            
            List<File> dirs;
            StationManager mgr = this.server.getStationManager();
            if (this.ids.size() == 0) {                
                LOGGER.info("Getting plot files from all stations");
                dirs = mgr.getStationDirectories();
            } else {
                LOGGER.info("Getting plot files from stations IDs: " + ids.toString());
                dirs = mgr.getStationDirectories(this.ids);
            }
            List<File> files = this.findRootFiles(dirs);
            if (files.size() != 0) {
                this.addPlots(this.targetFile, files);
                if (this.delete) {
                    for (File file : files) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            LOGGER.info("Deleted plot file: " + file.getPath());
                        } else {
                            LOGGER.warning("Failed to delete plot file: " + file.getPath());
                        }
                    }
                }    
            } else {
                LOGGER.warning("Task did not run because no plot files were found!");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error adding plots", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while adding plots", e);
        }
    }    
}
