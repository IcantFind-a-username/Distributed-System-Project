package edu.ntu.ds.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * TLV (Type-Length-Value) field as defined in Protocol Specification v1.1
 * 
 * TLV Format: Type (u16) | Length (u16) | Value (bytes)
 * 
 * - Type: 2 bytes, unsigned 16-bit, identifies the field type
 * - Length: 2 bytes, unsigned 16-bit, length of Value in bytes
 * - Value: variable length bytes
 * 
 * String encoding: UTF-8 bytes directly in Value (Length is string byte length)
 * Note: The spec mentions "UTF-8 with u16 length prefix" for strings in General Conventions,
 *       but in TLV the Length field already provides the length, so we just store UTF-8 bytes.
 */
public class TlvField {
    
    private TlvType type;
    private byte[] value;
    
    /**
     * Private constructor - use factory methods
     */
    private TlvField(TlvType type, byte[] value) {
        this.type = type;
        this.value = value;
    }
    
    // Factory methods
    
    /**
     * Create a string TLV field
     */
    public static TlvField createString(TlvType type, String str) {
        if (type.getValueType() != TlvType.ValueType.STRING) {
            throw new IllegalArgumentException("TLV type " + type + " is not a string type");
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return new TlvField(type, bytes);
    }
    
    /**
     * Create a uint8 TLV field
     */
    public static TlvField createUint8(TlvType type, byte value) {
        if (type.getValueType() != TlvType.ValueType.UINT8) {
            throw new IllegalArgumentException("TLV type " + type + " is not a uint8 type");
        }
        return new TlvField(type, new byte[]{value});
    }
    
    /**
     * Create a uint32 TLV field
     */
    public static TlvField createUint32(TlvType type, int value) {
        if (type.getValueType() != TlvType.ValueType.UINT32) {
            throw new IllegalArgumentException("TLV type " + type + " is not a uint32 type");
        }
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        return new TlvField(type, buffer.array());
    }
    
    /**
     * Create an int64 TLV field
     */
    public static TlvField createInt64(TlvType type, long value) {
        if (type.getValueType() != TlvType.ValueType.INT64) {
            throw new IllegalArgumentException("TLV type " + type + " is not an int64 type");
        }
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        return new TlvField(type, buffer.array());
    }
    
    /**
     * Create TLV field from raw bytes (for decoding)
     */
    public static TlvField fromRaw(TlvType type, byte[] value) {
        return new TlvField(type, value);
    }
    
    // Convenience factory methods
    
    public static TlvField username(String username) {
        return createString(TlvType.USERNAME, username);
    }
    
    public static TlvField password(String password) {
        return createString(TlvType.PASSWORD, password);
    }
    
    public static TlvField accountNo(String accountNo) {
        return createString(TlvType.ACCOUNT_NO, accountNo);
    }
    
    public static TlvField toAccountNo(String accountNo) {
        return createString(TlvType.TO_ACCOUNT_NO, accountNo);
    }
    
    public static TlvField currency(Currency currency) {
        return createUint8(TlvType.CURRENCY, currency.getValue());
    }
    
    public static TlvField amountCents(long cents) {
        return createInt64(TlvType.AMOUNT_CENTS, cents);
    }
    
    public static TlvField ttlSeconds(int ttl) {
        return createUint32(TlvType.TTL_SECONDS, ttl);
    }
    
    public static TlvField note(String note) {
        return createString(TlvType.NOTE, note);
    }
    
    // Encoding
    
    /**
     * Encode TLV field to bytes
     * Format: Type (u16) | Length (u16) | Value (bytes)
     * @return encoded bytes
     */
    public byte[] encode() {
        int totalLength = 2 + 2 + value.length; // type + length + value
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        buffer.putShort(type.getValue());       // Type (u16)
        buffer.putShort((short) value.length);  // Length (u16)
        buffer.put(value);                       // Value (bytes)
        
        return buffer.array();
    }
    
    /**
     * Get the total encoded length of this TLV field
     */
    public int getEncodedLength() {
        return 4 + value.length; // 2 (type) + 2 (length) + value.length
    }
    
    // Decoding
    
    /**
     * Decode a single TLV field from byte array
     * @param data byte array containing TLV data
     * @param offset starting offset
     * @return decoded TlvField
     * @throws ProtocolException if decoding fails
     */
    public static TlvField decode(byte[] data, int offset) throws ProtocolException {
        // Bounds check for type and length fields
        if (data.length - offset < 4) {
            throw new ProtocolException("Insufficient data for TLV header at offset " + offset);
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, data.length - offset);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        short typeValue = buffer.getShort();
        TlvType type = TlvType.fromShort(typeValue);
        
        int length = buffer.getShort() & 0xFFFF; // Treat as unsigned
        
        // Bounds check for value
        if (buffer.remaining() < length) {
            throw new ProtocolException("Insufficient data for TLV value: expected " + 
                length + " bytes, got " + buffer.remaining() + " at offset " + offset);
        }
        
        byte[] value = new byte[length];
        buffer.get(value);
        
        // Validate value length based on type
        validateValueLength(type, length);
        
        return new TlvField(type, value);
    }
    
    /**
     * Validate that value length is appropriate for the TLV type
     */
    private static void validateValueLength(TlvType type, int length) throws ProtocolException {
        switch (type.getValueType()) {
            case UINT8:
                if (length != 1) {
                    throw new ProtocolException("Invalid length for UINT8 TLV: " + length);
                }
                break;
            case UINT32:
                if (length != 4) {
                    throw new ProtocolException("Invalid length for UINT32 TLV: " + length);
                }
                break;
            case INT64:
                if (length != 8) {
                    throw new ProtocolException("Invalid length for INT64 TLV: " + length);
                }
                break;
            case STRING:
                // String can be any length (including 0)
                break;
        }
    }
    
    // Value getters
    
    public TlvType getType() {
        return type;
    }
    
    public byte[] getRawValue() {
        return Arrays.copyOf(value, value.length);
    }
    
    /**
     * Get value as String (for STRING type fields)
     */
    public String getStringValue() {
        if (type.getValueType() != TlvType.ValueType.STRING) {
            throw new IllegalStateException("TLV type " + type + " is not a string type");
        }
        return new String(value, StandardCharsets.UTF_8);
    }
    
    /**
     * Get value as uint8 (for UINT8 type fields)
     */
    public byte getUint8Value() {
        if (type.getValueType() != TlvType.ValueType.UINT8) {
            throw new IllegalStateException("TLV type " + type + " is not a uint8 type");
        }
        return value[0];
    }
    
    /**
     * Get value as uint32 (for UINT32 type fields)
     */
    public int getUint32Value() {
        if (type.getValueType() != TlvType.ValueType.UINT32) {
            throw new IllegalStateException("TLV type " + type + " is not a uint32 type");
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }
    
    /**
     * Get value as int64 (for INT64 type fields)
     */
    public long getInt64Value() {
        if (type.getValueType() != TlvType.ValueType.INT64) {
            throw new IllegalStateException("TLV type " + type + " is not an int64 type");
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }
    
    /**
     * Get value as Currency (for CURRENCY type field)
     */
    public Currency getCurrencyValue() throws ProtocolException {
        if (type != TlvType.CURRENCY) {
            throw new IllegalStateException("TLV type " + type + " is not currency type");
        }
        return Currency.fromByte(value[0]);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TLV{").append(type).append(", len=").append(value.length).append(", value=");
        
        switch (type.getValueType()) {
            case STRING:
                sb.append("\"").append(getStringValue()).append("\"");
                break;
            case UINT8:
                sb.append(value[0] & 0xFF);
                break;
            case UINT32:
                sb.append(getUint32Value() & 0xFFFFFFFFL);
                break;
            case INT64:
                sb.append(getInt64Value());
                break;
        }
        
        sb.append("}");
        return sb.toString();
    }
}
