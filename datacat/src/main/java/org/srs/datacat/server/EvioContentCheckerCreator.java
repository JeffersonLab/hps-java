package org.srs.datacat.server;


public class EvioContentCheckerCreator implements ContentCheckerCreator {

    public ContentChecker create() {
        return new EvioContentChecker();
    }

    public void free(ContentChecker arg0) {
        return;
    }

}
