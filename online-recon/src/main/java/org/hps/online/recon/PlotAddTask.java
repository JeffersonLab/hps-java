package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.StationManager.StationInfo;

/**
 * Task to add ROOT histograms from the reconstruction stations periodically using the hadd utility.
 * 
 * @author jeremym
 */
final class PlotAddTask extends TimerTask {

    private static Logger LOGGER = Logger.getLogger(PlotAddTask.class.getPackageName());
    
    private final Server server;
    
    private final File targetFile;
       
    private boolean dryRun = true;
    
    private static final String HADD = "hadd";
    
    PlotAddTask(Server server, File targetFile, boolean dryRun) {
        this.server = server;       
        this.targetFile = targetFile;
        if (this.targetFile.exists()) {
            throw new RuntimeException("Target plot file already exists: " + this.targetFile.getPath());
        }
        this.dryRun = dryRun;
        
        // Check that the hadd command exists.
        checkHadd();
    }

    private void checkHadd() {
        ProcessBuilder pb = new ProcessBuilder(HADD);
        try {
            Process p = pb.start();
            p.waitFor();            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "The hadd command does not exist (did you run the thisroot.sh setup script?).", e);
            throw new RuntimeException("The hadd command does not exist.", e);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
    
    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
            
    static class RootFileFinder extends SimpleFileVisitor<Path> {
                
        List<File> files = new ArrayList<File>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.root");
        
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                files.add(file.toFile());
                LOGGER.info("Found matching file: " + name.getFileName().toString());
            }
        }
        
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            LOGGER.info("Visiting " + file.toFile().toPath());
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
    
    List<File> getRootFiles() throws IOException {
        final StationManager mgr = server.getStationManager();
        RootFileFinder finder = new RootFileFinder();
        for (StationInfo statInfo : mgr.getStations()) {
            LOGGER.info("Checking for plot files from station: " + statInfo.stationName);
            final File statDir = statInfo.dir;
            Files.walkFileTree(statDir.toPath(), finder);
        }
        return finder.getFiles();
    }
    
    void addPlots(File target, List<File> inFiles) throws IOException, InterruptedException { 
        LOGGER.info("Adding plots with target " + target.getPath());
        LOGGER.info("Plot files: " + inFiles.toString());
        if (inFiles.size() > 0) {
            List<File> files = new ArrayList<File>(inFiles);
            if (target.exists() && !this.dryRun) {
                Path oldTarget = Paths.get(target.getPath() + ".old");
                Path newTarget = Paths.get(target.getPath());
                LOGGER.info("Copying existing target to <" + oldTarget + ">");
                Files.copy(newTarget, oldTarget, StandardCopyOption.REPLACE_EXISTING);
                File oldTargetFile = oldTarget.toFile();
                if (oldTargetFile.exists()) {
                    newTarget.toFile().delete();
                    if (newTarget.toFile().exists()) {
                        throw new RuntimeException("Failed to delete existing target file: " + newTarget.toFile().getPath());
                    }
                } else {
                    throw new RuntimeException("Failed to move existing target file: " + oldTargetFile.getPath());
                }
            } else {
                LOGGER.info("Did not check for existing target (dry run enabled)");
            }

            /*
        Usage: hadd [-f[fk][0-9]] [-k] [-T] [-O] [-a]
                [-n maxopenedfiles] [-cachesize size] [-j ncpus] [-v [verbosity]]
                targetfile source1 [source2 source3 ...]
             */

            List<String> cmd = new ArrayList<String>();
            cmd.add("hadd");
            cmd.add("-v");
            cmd.add(target.getPath());
            for (File file : files) {
                cmd.add(file.getPath());
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);        
            LOGGER.info("hadd command " + cmd);
            if (!dryRun) {
                LOGGER.info("Running hadd on " + files.size() + " files");
                Process p = pb.start();
                //int retCode = 
                p.waitFor();
                /*
                if (retCode != 0) {
                    LOGGER.warning("hadd returned non-zero return code <" + retCode + ">");            
                }
                */
                if (!target.exists()) {
                    throw new IOException("Failed to create new plot file: " + target.getPath());
                } else {
                    LOGGER.info("Wrote new plot file: " + target.getPath());
                }
            } else {
                LOGGER.info("The hadd command was not run (dry run enabled).");
            }
        } else {
            LOGGER.info("hadd did not run (not enough new plot files).");
        }
    }

    @Override
    public void run() {
        try {
            List<File> files = this.getRootFiles();
            this.addPlots(this.targetFile, files);            
            if (!this.dryRun) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        LOGGER.info("Deleted plot file: " + file.getPath());
                    } else {
                        LOGGER.warning("Failed to delete plot file: " + file.getPath());
                    }
                }
            } else {
                LOGGER.info("Plot files were not deleted (dry run enabled).");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error adding plots", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while adding plots", e);
        }
    }    
}
