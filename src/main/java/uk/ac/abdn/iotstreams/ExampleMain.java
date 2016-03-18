package uk.ac.abdn.iotstreams;

import eu.larkc.csparql.common.config.Config;

/**
 * @author nhc
 *
 * Examples of how to use this project.
 * Note that the file "csparql.properties" in the project root
 * has a setting that determines whether live data streams
 * will be used. See README.md for details.
 */
public final class ExampleMain {
    /**
     * Main method with examples of how to use this project.
     * @param args not used
     */
    public static void main(final String[] args) {
        //Check csparql.properties to determine whether live or recorded data will be used
        if (useRecordedData()) {
            //TODO
        } else {// Live data stream
            //TODO
        }
    }
    
    private static boolean useRecordedData() {
        return Config.INSTANCE.isEsperUsingExternalTimestamp();
    }
    
}
