package edu.ntu.ds.protocol;

/**
 * Currency enumeration for the currency TLV field (0x0004)
 * 
 * Encoded as u8 (unsigned 8-bit integer)
 */
public enum Currency {
    SGD((byte) 0, "Singapore Dollar"),
    USD((byte) 1, "US Dollar"),
    EUR((byte) 2, "Euro"),
    GBP((byte) 3, "British Pound"),
    JPY((byte) 4, "Japanese Yen"),
    CNY((byte) 5, "Chinese Yuan");
    
    private final byte value;
    private final String name;
    
    Currency(byte value, String name) {
        this.value = value;
        this.name = name;
    }
    
    public byte getValue() {
        return value;
    }
    
    public String getCurrencyName() {
        return name;
    }
    
    /**
     * Convert byte value to Currency enum
     * @param value byte value from protocol
     * @return Currency enum
     * @throws ProtocolException if value is invalid
     */
    public static Currency fromByte(byte value) throws ProtocolException {
        for (Currency c : values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new ProtocolException("Invalid currency code: " + (value & 0xFF));
    }
    
    @Override
    public String toString() {
        return name() + " (" + name + ")";
    }
}
