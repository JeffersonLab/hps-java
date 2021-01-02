package org.hps.online.recon;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.json.JSONObject;

/**
 * Command results that can be sent back to the client
 * from a server request
 */
public abstract class CommandResult {

    /**
     * Error status string.
     */
    public static final String STATUS_ERROR = "ERROR";

    /**
     * Success status string.
     */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * Return a result which describes result of command execution
     * i.e. success or failure (JSON format).
     */
    public static class CommandStatus extends CommandResult {

        String message;
        String status;

        public CommandStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            jo.put("status", status);
            jo.put("message", message);
            return jo;
        }

        public String toString() {
            return toJSON().toString();
        }
    }

    /**
     * Error status
     */
    public static class Error extends CommandStatus {
        public Error(String message) {
            super(CommandResult.STATUS_ERROR, message);
        }
    }

    /**
     * Success status
     */
    public static class Success extends CommandStatus {
        public Success(String message) {
            super(CommandResult.STATUS_SUCCESS, message);
        }
    }

    /**
     * Return a generic result with an object that can be converted to
     * a JSON string.
     */
    public static class GenericResult extends CommandResult {

        final Object o;

        public GenericResult(Object o) {
            this.o = o;
        }

        public String toString() {
            return o.toString();
        }
    }

    /**
     * Return a JSON object.
     */
    public static class JSONResult extends CommandResult {

        final JSONObject jo;

        public JSONResult(JSONObject jo) {
            this.jo = jo;
        }

        public String toString() {
            return jo.toString();
        }
    }

    /**
     * Return a result which encapsulates information needed for
     * streaming a log file back to the client.
     */
    public static class LogStreamResult extends CommandResult {

        SimpleLogListener listener;
        Tailer tailer;
        File log;

        public LogStreamResult(Tailer tailer, SimpleLogListener listener, File log) {
            this.listener = listener;
            this.tailer = tailer;
            this.log = log;
        }
    }
}
