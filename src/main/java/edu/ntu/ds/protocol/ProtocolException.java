package edu.ntu.ds.protocol;

/**
 * Exception thrown when protocol encoding/decoding fails
 */
public class ProtocolException extends Exception {
    
    public ProtocolException(String message) {
        super(message);
    }
    
    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
