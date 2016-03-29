package uk.ac.abdn.iotstreams.util;

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
     * Constructs an Exception to throw when an internal error happened
     * @param msg The error message
     * @return The constructed Exception
     */
    public static IotStreamsException internalError(final String msg) {
        return new IotStreamsException(msg);
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
