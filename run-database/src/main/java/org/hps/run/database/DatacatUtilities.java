package org.hps.run.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;

final class DatacatUtilities {
    
    private DatacatUtilities() {
        throw new RuntimeException("Do not instantiate this class.");
    }
    
    static final List<File> toFileList(DatasetResultSetModel datasets) {
        List<File> files = new ArrayList<File>();
        for (DatasetModel dataset : datasets.getResults()) {
            String resource = 
                    ((DatasetWithViewModel) dataset).getViewInfo().getLocations().iterator().next().getResource();
            if (resource.startsWith("/ss")) {
                resource = "/cache" + resource;
            }
            files.add(new File(resource));
        }
        return files;
    }
}
