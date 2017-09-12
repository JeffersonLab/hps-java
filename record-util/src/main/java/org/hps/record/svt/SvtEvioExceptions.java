/**
 * 
 */
package org.hps.record.svt;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class SvtEvioExceptions {

    public static class SvtEvioReaderException extends Exception {

        public SvtEvioReaderException(String message) {
            super(message);
        }

        public SvtEvioReaderException(SvtEvioReaderException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderException extends SvtEvioReaderException {

        public SvtEvioHeaderException(String message) {
            super(message);
        }

        public SvtEvioHeaderException(SvtEvioHeaderException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderSyncErrorException extends SvtEvioHeaderException {

        public SvtEvioHeaderSyncErrorException(String message) {
            super(message);
        }

        public SvtEvioHeaderSyncErrorException(SvtEvioHeaderSyncErrorException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderApvBufferAddressException extends SvtEvioHeaderException {

        public SvtEvioHeaderApvBufferAddressException(String message) {
            super(message);
        }

        public SvtEvioHeaderApvBufferAddressException(SvtEvioHeaderApvBufferAddressException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderApvFrameCountException extends SvtEvioHeaderException {

        public SvtEvioHeaderApvFrameCountException(String message) {
            super(message);
        }

        public SvtEvioHeaderApvFrameCountException(SvtEvioHeaderApvFrameCountException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderApvReadErrorException extends SvtEvioHeaderException {

        public SvtEvioHeaderApvReadErrorException(String message) {
            super(message);
        }

        public SvtEvioHeaderApvReadErrorException(SvtEvioHeaderApvReadErrorException e) {
            super(e);
        }

    }

    public static class SvtEvioHeaderMultisampleErrorBitException extends SvtEvioHeaderException {

        public SvtEvioHeaderMultisampleErrorBitException(String message) {
            super(message);
        }

        public SvtEvioHeaderMultisampleErrorBitException(SvtEvioHeaderMultisampleErrorBitException e) {
            super(e);
        }

    }

    public static class SvtEvioHeaderOFErrorException extends SvtEvioHeaderException {

        public SvtEvioHeaderOFErrorException(String message) {
            super(message);
        }

        public SvtEvioHeaderOFErrorException(SvtEvioHeaderOFErrorException e) {
            super(e);
        }
    }

    public static class SvtEvioHeaderSkipCountException extends SvtEvioHeaderException {

        public SvtEvioHeaderSkipCountException(String message) {
            super(message);
        }

        public SvtEvioHeaderSkipCountException(SvtEvioHeaderSkipCountException e) {
            super(e);
        }
    }

    /**
     * Private constructor to avoid instantiation
     */
    private SvtEvioExceptions() {
        // TODO Auto-generated constructor stub
    }

}
