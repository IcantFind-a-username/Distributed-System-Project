package edu.ntu.ds.protocol;

/**
 * Message types as defined in Protocol Specification v1.1
 * 
 * msgType field (offset 3, 1 byte):
 * - 0 = REQ (Request)
 * - 1 = REP (Reply)
 * - 2 = CBK (Callback)
 */
public enum MessageType {
    REQ((byte) 0),  // Request
    REP((byte) 1),  // Reply
    CBK((byte) 2);  // Callback notification
    
    private final byte value;
    
    MessageType(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return value;
    }
    
    /**
     * Convert byte value to MessageType enum
     * @param value byte value from protocol
     * @return MessageType enum
     * @throws ProtocolException if value is invalid
     */
    public static MessageType fromByte(byte value) throws ProtocolException {
        for (MessageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new ProtocolException("Invalid message type: " + value);
    }
}
