package org.srs.datacat.server;


public class LcioContentCheckerCreator implements ContentCheckerCreator {

    public ContentChecker create() {
        return new LcioContentChecker();
    }
    
    public void free(ContentChecker cc) {
        return;
    }
    
}
