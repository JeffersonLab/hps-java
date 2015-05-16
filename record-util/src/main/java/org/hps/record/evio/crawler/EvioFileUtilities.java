package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

public final class EvioFileUtilities {

    private static final Logger LOGGER = LogUtil.create(EvioFileUtilities.class);

    private static final long MILLISECONDS = 1000L;

    static Date getControlDate(final File file, final int eventTag, final int gotoEvent) {
        Date date = null;
        EvioReader reader = null;
        try {
            reader = open(file);
            EvioEvent event;
            if (gotoEvent > 0) {
                reader.gotoEventNumber(gotoEvent);
            } else if (gotoEvent < 0) {
                reader.gotoEventNumber(reader.getEventCount() + gotoEvent);
            }
            while ((event = reader.parseNextEvent()) != null) {
                if (event.getHeader().getTag() == eventTag) {
                    final int[] data = EvioEventUtilities.getControlEventData(event);
                    final long seconds = data[0];
                    date = new Date(seconds * MILLISECONDS);
                    break;
                }
            }
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return date;
    }

    static Date getHeadBankDate(final EvioEvent event) {
        Date date = null;
        final BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
        if (headBank != null) {
            final int[] data = headBank.getIntData();
            final long time = data[3];
            if (time != 0L) {
                date = new Date(time * MILLISECONDS);
            }
        }
        return date;
    }

    static Date getRunEnd(final File file) {
        Date date = getControlDate(file, EvioEventConstants.END_EVENT_TAG, -10);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file);
                reader.gotoEventNumber(reader.getEventCount() - 11);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    static Integer getRunFromName(final File file) {
        final String name = file.getName();
        final int startIndex = name.lastIndexOf("_") + 1;
        final int endIndex = name.indexOf(".");
        return Integer.parseInt(name.substring(startIndex, endIndex));
    }

    static Date getRunStart(final File file) {
        Date date = getControlDate(file, EvioEventConstants.PRESTART_EVENT_TAG, 0);
        if (date == null) {
            EvioReader reader = null;
            try {
                reader = open(file);
                EvioEvent event = null;
                while ((event = reader.parseNextEvent()) != null) {
                    if (EvioEventUtilities.isPhysicsEvent(event)) {
                        if ((date = getHeadBankDate(event)) != null) {
                            break;
                        }
                    }
                }
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return date;
    }

    static Integer getSequenceNumber(final File file) {
        final String name = file.getName();
        return Integer.parseInt(name.substring(name.lastIndexOf(".") + 1));
    }

    static EvioReader open(final File file) throws IOException, EvioException {
        return open(file, false);
    }
    
    static EvioReader open(final File file, boolean sequential) throws IOException, EvioException {
        File openFile = file;
        if (isMssFile(file)) {
            openFile = getCachedFile(file);
        }        
        final long start = System.currentTimeMillis();
        final EvioReader reader = new EvioReader(openFile, false, sequential);
        final long end = System.currentTimeMillis() - start;
        LOGGER.info("opened " + openFile.getPath() + " in " + end / MILLISECONDS + " seconds");
        return reader;
    }
    
    static EvioReader open(final String path) throws IOException, EvioException {
        return open(new File(path));
    }    
    
    static File getCachedFile(File file) {
        if (!isMssFile(file)) {
            throw new IllegalArgumentException("File " + file.getPath() + " is not on the JLab MSS.");
        }
        if (isCachedFile(file)) {
            throw new IllegalArgumentException("File " + file.getPath() + " is already on the cache disk.");
        }
        return new File("/cache" + file.getPath());
    }
    
    static boolean isMssFile(File file) {
        return file.getPath().startsWith("/mss");
    }
    
    static boolean isCachedFile(File file) {
        return file.getPath().startsWith("/cache");
    }
}
