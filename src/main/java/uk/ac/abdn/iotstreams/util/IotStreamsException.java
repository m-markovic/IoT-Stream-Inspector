package uk.ac.abdn.iotstreams.util;

import java.io.IOException;
import java.net.URL;

/**
 * 
 * @author nhc
 *
 * The only Exception expected from the IotStreams application.
 */
@SuppressWarnings("serial")
public final class IotStreamsException extends RuntimeException {
    /**
     * Constructor.
     * @param msg The error message
     * @param cause The Exception behind this situation 
     */
    private IotStreamsException(
        final String msg,
        final Exception cause) {
        super(String.format("%s, caused by %s", msg, cause));
    }    

    /**
     * Constructor.
     * @param msg The error message
     */
    public IotStreamsException(String msg) {
        super(msg);
    }

    /**
     * Wraps an Exception caught during communication with mywirelesstag.com 
     * @param e IOException caught while communicating with mywirelesstag.com
     * @return An IotStreamsException representing the known facts about the situation
     */
    public static IotStreamsException wirelessTagConnectionFailed(final IOException e) {
        return new IotStreamsException("Failed to connect to mywirelesstag.com", e);
    }

    /**
     * Creates an Exception for HTTP errors during communication with mywirelesstag.com
     * @param responseCode A non-200 HTTP response from mywirelesstag.com
     * @param url The url that returned the response
     * @return An IotStreamsException representing the known facts about the situation
     */
    public static IotStreamsException wirelessTagSentError(
            final int responseCode, 
            final URL url) {
        return new IotStreamsException(
                String.format(
                    "Got HTTP response code %d on request for %s", 
                    responseCode, 
                    url), 
                null);
    }
    
    /**
     * Exception to use when an internal error caused a caught Exception
     * @param e The caught Exception
     * @return Wrapping Exception
     */
    public static IotStreamsException internalError(
            final Exception e) {
        return new IotStreamsException("Internal IoStreams application error", e);
    }

    /**
     * Constructs an Exception to throw when some user input was malformed.
     * @param userInput The malformed user input.
     * @param e The Exception caught when processing the malformed input.
     * @return The wrapping Exception
     */
    public static IotStreamsException userInputError(
            final String userInput,
            final Exception e) {
        return new IotStreamsException(String.format("Malformed input: '%s'", userInput), e);
    }
    
    /**
     * Constructs an Exception to throw when some file IO related to the meat probe failed.
     * @param e Exception caught during file IO
     * @return The wrapping Exception
     */
    public static IotStreamsException meatProbeIOfailed(final IOException e) {
        return new IotStreamsException("Failure reading a meat probe file", e);
    }

    /**
     * Constructs an Exception to throw when an internal error happened
     * @param msg The error message
     * @return The constructed Exception
     */
    public static IotStreamsException internalError(final String msg) {
        return new IotStreamsException(msg);
    }

    /**
     * Constructs an Exception to throw when some file IO related to annotations failed.
     * @param e Exception caught during file IO
     * @return The wrapping Exception
     */
    public static IotStreamsException annotationIOfailed(IOException e) {
        return new IotStreamsException("Failure reading annotation file", e);
    }

    /**
     * Constructs an Exception to throw when reading the configuration files failed.
     * @param e Exception caught during configuration
     * @return The wrapping Exception
     */
    public static IotStreamsException configurationError(final Exception e) {
        return new IotStreamsException("Failure reading configuration", e);
    }

    /**
     * Constructs an Exception to throw when reading the configuration files failed.
     * @param msg error message
     * @return The wrapping Exception
     */
    public static IotStreamsException configurationError(final String msg) {
        return new IotStreamsException(String.format("Failure reading configuration: %s", msg));
    }
}
