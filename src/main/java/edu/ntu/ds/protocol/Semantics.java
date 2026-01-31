package edu.ntu.ds.protocol;

/**
 * Invocation semantics as defined in Protocol Specification v1.1
 * 
 * semantics field (offset 8, 1 byte):
 * - 0 = ALO (At-Least-Once): server executes every received request
 * - 1 = AMO (At-Most-Once): server suppresses duplicate execution by caching replies
 * 
 * For non-idempotent operations, AMO semantics ensures correctness by returning
 * cached replies for duplicate requests instead of re-executing logic.
 */
public enum Semantics {
    ALO((byte) 0),  // At-Least-Once
    AMO((byte) 1);  // At-Most-Once
    
    private final byte value;
    
    Semantics(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return value;
    }
    
    /**
     * Convert byte value to Semantics enum
     * @param value byte value from protocol
     * @return Semantics enum
     * @throws ProtocolException if value is invalid
     */
    public static Semantics fromByte(byte value) throws ProtocolException {
        for (Semantics s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        throw new ProtocolException("Invalid semantics value: " + value);
    }
}
