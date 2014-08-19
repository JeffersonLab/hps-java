package org.hps.record;

import java.io.IOException;

/**
 * An Exception thrown when an end run occurs.
 */
class EndRunException extends IOException {
    EndRunException(String message) {
        super(message);
    }
}
