package edu.ntu.ds.protocol;

/**
 * Operation codes as defined in Protocol Specification v1.1
 * 
 * opCode field (offset 6, 2 bytes, unsigned 16-bit):
 * 
 * Standard operations (0x0001 - 0x00FF):
 * - 0x0001: OPEN_ACCOUNT (non-idempotent)
 * - 0x0002: CLOSE_ACCOUNT (non-idempotent)
 * - 0x0003: DEPOSIT (non-idempotent)
 * - 0x0004: WITHDRAW (non-idempotent)
 * - 0x0005: REGISTER_CALLBACK (idempotent)
 * - 0x0006: UNREGISTER_CALLBACK (idempotent)
 * 
 * Extended operations (0x0100 - 0x01FF):
 * - 0x0101: QUERY_BALANCE (idempotent)
 * - 0x0102: TRANSFER (non-idempotent)
 * 
 * Callback operations (0x8000+):
 * - 0x8001: ACCOUNT_UPDATE (callback notification)
 */
public enum OpCode {
    OPEN_ACCOUNT((short) 0x0001, false),
    CLOSE_ACCOUNT((short) 0x0002, false),
    DEPOSIT((short) 0x0003, false),
    WITHDRAW((short) 0x0004, false),
    REGISTER_CALLBACK((short) 0x0005, true),
    UNREGISTER_CALLBACK((short) 0x0006, true),
    QUERY_BALANCE((short) 0x0101, true),
    TRANSFER((short) 0x0102, false),
    ACCOUNT_UPDATE((short) 0x8001, false);  // N/A for idempotency (callback only)
    
    private final short value;
    private final boolean idempotent;
    
    OpCode(short value, boolean idempotent) {
        this.value = value;
        this.idempotent = idempotent;
    }
    
    public short getValue() {
        return value;
    }
    
    public boolean isIdempotent() {
        return idempotent;
    }
    
    /**
     * Convert short value to OpCode enum
     * @param value short value from protocol (treated as unsigned 16-bit)
     * @return OpCode enum
     * @throws ProtocolException if value is invalid
     */
    public static OpCode fromShort(short value) throws ProtocolException {
        for (OpCode op : values()) {
            if (op.value == value) {
                return op;
            }
        }
        throw new ProtocolException("Invalid operation code: 0x" + String.format("%04X", value & 0xFFFF));
    }
}
