package uk.ac.abdn.iotstreams.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author nhc
 *
 * Utilities for logging. Use pom.xml to control which logging framework to use.
 */
public final class Logging {
    /** All application-specific logging will be associated with this class. */
    private static final Logger logger = LoggerFactory.getLogger("IoT-streams");

    /**
     * Log msg on level warn
     * @param msg Message to log
     */
    public static void warn(final String msg) {
        logger.warn(msg);
    }

    /**
     * Log msg on level info
     * @param msg Message to log
     */
    public static void info(String msg) {
        logger.info(msg);
    }
}
