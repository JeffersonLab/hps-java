package org.hps.users.jeremym.crawler;

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

public class EvioFileUtilities {

    private static final Logger LOGGER = LogUtil.create(EvioFileUtilities.class);

    private static final long MILLISECONDS = 1000L;

    static void cache(final File file) {
        if (!file.getPath().startsWith("/mss")) {
            throw new IllegalArgumentException("Only files on /mss can be cached.");
        }
        try {
            new ProcessBuilder("jcache", "submit", "default", file.getPath()).start();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("process started to cache " + file.getPath());
    }

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
        final long start = System.currentTimeMillis();
        final EvioReader reader = new EvioReader(file, false, false);
        final long end = System.currentTimeMillis() - start;
        LOGGER.info("opened " + file.getPath() + " in " + end / MILLISECONDS + " seconds");
        return reader;
    }
}
