package uk.ac.abdn.iotstreams.util;

import java.time.ZoneId;

/**
 * 
 * @author nhc
 * 
 * Shared constants between IotStreams packages.
 */
public final class Constants {
    /** All date/time stamps without time zone are assigned to this one: */
    public static final ZoneId UK = ZoneId.of("Europe/London");
}
