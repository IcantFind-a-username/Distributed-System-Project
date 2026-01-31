package edu.ntu.ds.protocol;

/**
 * Status codes as defined in Protocol Specification v1.1
 * 
 * status field (offset 10, 2 bytes, unsigned 16-bit):
 * - 0: OK - Success
 * - 1: BAD_REQUEST - Malformed or missing fields
 * - 2: AUTH_FAIL - Authentication failed
 * - 3: NOT_FOUND - Account not found
 * - 4: INSUFFICIENT_FUNDS - Insufficient balance
 * - 5: CURRENCY_MISMATCH - Currency mismatch
 * - 6: ALREADY_EXISTS - Resource exists
 * - 7: INTERNAL_ERROR - Server error
 * 
 * Error Flag Rule: flags.bit1 MUST be set to 1 if and only if status != 0
 */
public enum StatusCode {
    OK((short) 0, "Success"),
    BAD_REQUEST((short) 1, "Malformed or missing fields"),
    AUTH_FAIL((short) 2, "Authentication failed"),
    NOT_FOUND((short) 3, "Account not found"),
    INSUFFICIENT_FUNDS((short) 4, "Insufficient balance"),
    CURRENCY_MISMATCH((short) 5, "Currency mismatch"),
    ALREADY_EXISTS((short) 6, "Resource already exists"),
    INTERNAL_ERROR((short) 7, "Server internal error");
    
    private final short value;
    private final String description;
    
    StatusCode(short value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public short getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isError() {
        return this != OK;
    }
    
    /**
     * Convert short value to StatusCode enum
     * @param value short value from protocol (treated as unsigned 16-bit)
     * @return StatusCode enum
     * @throws ProtocolException if value is invalid
     */
    public static StatusCode fromShort(short value) throws ProtocolException {
        for (StatusCode sc : values()) {
            if (sc.value == value) {
                return sc;
            }
        }
        throw new ProtocolException("Invalid status code: " + value);
    }
    
    @Override
    public String toString() {
        return name() + "(" + value + "): " + description;
    }
}
