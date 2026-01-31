package edu.ntu.ds.protocol;

/**
 * TLV (Type-Length-Value) field types as defined in Protocol Specification v1.1
 * 
 * TLV Format: Type (u16) | Length (u16) | Value (bytes)
 * 
 * Type values:
 * - 0x0001: username (string, UTF-8 with u16 length prefix)
 * - 0x0002: password (string)
 * - 0x0003: accountNo (string)
 * - 0x0004: currency (u8 enum)
 * - 0x0005: amountCents (int64, signed 64-bit integer representing cents)
 * - 0x0006: toAccountNo (string)
 * - 0x0007: ttlSeconds (u32, unsigned 32-bit integer)
 * - 0x0008: note (string, optional)
 */
public enum TlvType {
    USERNAME((short) 0x0001, "username", ValueType.STRING),
    PASSWORD((short) 0x0002, "password", ValueType.STRING),
    ACCOUNT_NO((short) 0x0003, "accountNo", ValueType.STRING),
    CURRENCY((short) 0x0004, "currency", ValueType.UINT8),
    AMOUNT_CENTS((short) 0x0005, "amountCents", ValueType.INT64),
    TO_ACCOUNT_NO((short) 0x0006, "toAccountNo", ValueType.STRING),
    TTL_SECONDS((short) 0x0007, "ttlSeconds", ValueType.UINT32),
    NOTE((short) 0x0008, "note", ValueType.STRING);
    
    /**
     * Value type for encoding/decoding
     */
    public enum ValueType {
        STRING,  // UTF-8 string (NO additional length prefix in TLV value, Length field is the string length)
        UINT8,   // Unsigned 8-bit integer
        UINT32,  // Unsigned 32-bit integer
        INT64    // Signed 64-bit integer
    }
    
    private final short value;
    private final String name;
    private final ValueType valueType;
    
    TlvType(short value, String name, ValueType valueType) {
        this.value = value;
        this.name = name;
        this.valueType = valueType;
    }
    
    public short getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public ValueType getValueType() {
        return valueType;
    }
    
    /**
     * Convert short value to TlvType enum
     * @param value short value from protocol
     * @return TlvType enum
     * @throws ProtocolException if value is invalid
     */
    public static TlvType fromShort(short value) throws ProtocolException {
        for (TlvType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new ProtocolException("Invalid TLV type: 0x" + String.format("%04X", value & 0xFFFF));
    }
    
    @Override
    public String toString() {
        return name + "(0x" + String.format("%04X", value & 0xFFFF) + ")";
    }
}
