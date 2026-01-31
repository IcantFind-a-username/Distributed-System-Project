package edu.ntu.ds.protocol;

/**
 * Protocol constants as defined in Protocol Specification v1.1
 */
public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    
    // Magic number to identify protocol messages
    public static final short MAGIC = (short) 0xD5D5;
    
    // Protocol version
    public static final byte VERSION = 1;
    
    // Fixed header length in bytes
    public static final short HEADER_LENGTH = 32;
    
    // Header field offsets
    public static final int OFFSET_MAGIC = 0;
    public static final int OFFSET_VERSION = 2;
    public static final int OFFSET_MSG_TYPE = 3;
    public static final int OFFSET_HEADER_LEN = 4;
    public static final int OFFSET_OP_CODE = 6;
    public static final int OFFSET_SEMANTICS = 8;
    public static final int OFFSET_FLAGS = 9;
    public static final int OFFSET_STATUS = 10;
    public static final int OFFSET_REQUEST_ID = 12;
    public static final int OFFSET_CLIENT_ID = 20;
    public static final int OFFSET_SEQ_NO = 24;
    public static final int OFFSET_PAYLOAD_LEN = 28;
    
    // Flag bit masks
    public static final byte FLAG_CRC = 0x01;      // bit0 = CRC enabled
    public static final byte FLAG_ERROR = 0x02;    // bit1 = Error flag
    
    // Client retry policy
    public static final int INITIAL_TIMEOUT_MS = 500;
    public static final int MAX_RETRIES = 5;
    
    // Maximum payload size (reasonable limit for UDP)
    public static final int MAX_PAYLOAD_SIZE = 65000;
    
    // CRC32 size
    public static final int CRC32_SIZE = 4;
}
