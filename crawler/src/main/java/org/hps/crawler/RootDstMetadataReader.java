package org.hps.crawler;

import hep.io.root.RootClassNotFound;
import hep.io.root.RootFileReader;
import hep.io.root.interfaces.TLeafElement;
import hep.io.root.interfaces.TObjArray;
import hep.io.root.interfaces.TTree;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a very simple metadata reader for ROOT DST files.
 * <p>
 * It currently only sets the standard metadata for event count and run number.
 *
 * @author Jeremy McCormick, SLAC
 */
public class RootDstMetadataReader implements FileMetadataReader {

    /**
     * Get the metadata for a ROOT DST file.
     *
     * @return the metadata for a ROOT DST file
     */
    @Override
    public Map<String, Object> getMetadata(final File file) throws IOException {
        final Map<String, Object> metadata = new HashMap<String, Object>();
        Long run = FileUtilities.getRunFromFileName(file);
        metadata.put("runMin", run);
        metadata.put("runMax", run);
        /*
        RootFileReader rootReader = null;
        long eventCount = 0;
        int runMin = 0;
        int runMax = 0;
        long size = 0;
        try {
            rootReader = new RootFileReader(file.getAbsolutePath());
            final TTree tree = (TTree) rootReader.get("HPS_Event");
            // TBranch branch = tree.getBranch("Event");
            eventCount = tree.getEntries();
            size = tree.getTotBytes();
            final TObjArray leaves = tree.getLeaves();

            for (final Object object : leaves) {
                final TLeafElement leaf = (TLeafElement) object;
                if ("run_number".equals(leaf.getName())) {
                    runMin = (int) leaf.getWrappedValue(0);
                    runMax = (int) leaf.getWrappedValue(0);
                    break;
                }
            }
        } catch (IOException | RootClassNotFound e) {
            throw new IOException(e);
        } finally {
            if (rootReader != null) {
                rootReader.close();
            }
        }
        metadata.put("eventCount", eventCount);
        metadata.put("runMin", runMin);
        metadata.put("runMax", runMax);
        metadata.put("size", size);
        */
        return metadata;
    }
}
