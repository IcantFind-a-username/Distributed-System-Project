package edu.ntu.ds.protocol;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Payload container for TLV fields
 * 
 * Payloads are encoded as a sequence of TLV fields.
 * Fields may appear in any order unless otherwise specified.
 */
public class Payload {
    
    private final Map<TlvType, TlvField> fields;
    
    public Payload() {
        this.fields = new LinkedHashMap<>(); // Preserve insertion order
    }
    
    /**
     * Add a TLV field to the payload
     * If a field of the same type already exists, it will be replaced.
     */
    public Payload addField(TlvField field) {
        fields.put(field.getType(), field);
        return this;
    }
    
    /**
     * Get a TLV field by type
     * @return TlvField or null if not found
     */
    public TlvField getField(TlvType type) {
        return fields.get(type);
    }
    
    /**
     * Check if payload contains a field of specified type
     */
    public boolean hasField(TlvType type) {
        return fields.containsKey(type);
    }
    
    /**
     * Get all fields
     */
    public Collection<TlvField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }
    
    /**
     * Get number of fields
     */
    public int size() {
        return fields.size();
    }
    
    /**
     * Check if payload is empty
     */
    public boolean isEmpty() {
        return fields.isEmpty();
    }
    
    // ========== Convenience getters ==========
    
    public String getUsername() {
        TlvField field = fields.get(TlvType.USERNAME);
        return field != null ? field.getStringValue() : null;
    }
    
    public String getPassword() {
        TlvField field = fields.get(TlvType.PASSWORD);
        return field != null ? field.getStringValue() : null;
    }
    
    public String getAccountNo() {
        TlvField field = fields.get(TlvType.ACCOUNT_NO);
        return field != null ? field.getStringValue() : null;
    }
    
    public String getToAccountNo() {
        TlvField field = fields.get(TlvType.TO_ACCOUNT_NO);
        return field != null ? field.getStringValue() : null;
    }
    
    public Currency getCurrency() throws ProtocolException {
        TlvField field = fields.get(TlvType.CURRENCY);
        return field != null ? field.getCurrencyValue() : null;
    }
    
    public Long getAmountCents() {
        TlvField field = fields.get(TlvType.AMOUNT_CENTS);
        return field != null ? field.getInt64Value() : null;
    }
    
    public Integer getTtlSeconds() {
        TlvField field = fields.get(TlvType.TTL_SECONDS);
        return field != null ? field.getUint32Value() : null;
    }
    
    public String getNote() {
        TlvField field = fields.get(TlvType.NOTE);
        return field != null ? field.getStringValue() : null;
    }
    
    // ========== Encoding ==========
    
    /**
     * Encode payload to bytes
     * @return encoded payload bytes (may be empty if no fields)
     */
    public byte[] encode() {
        if (fields.isEmpty()) {
            return new byte[0];
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (TlvField field : fields.values()) {
            byte[] encoded = field.encode();
            baos.write(encoded, 0, encoded.length);
        }
        return baos.toByteArray();
    }
    
    /**
     * Get the total encoded length of the payload
     */
    public int getEncodedLength() {
        int length = 0;
        for (TlvField field : fields.values()) {
            length += field.getEncodedLength();
        }
        return length;
    }
    
    // ========== Decoding ==========
    
    /**
     * Decode payload from bytes
     * @param data byte array containing payload
     * @param offset starting offset
     * @param length payload length in bytes
     * @return decoded Payload
     * @throws ProtocolException if decoding fails
     */
    public static Payload decode(byte[] data, int offset, int length) throws ProtocolException {
        Payload payload = new Payload();
        
        if (length == 0) {
            return payload;
        }
        
        // Bounds check
        if (data.length - offset < length) {
            throw new ProtocolException("Insufficient data for payload: expected " + 
                length + " bytes, got " + (data.length - offset));
        }
        
        int currentOffset = offset;
        int endOffset = offset + length;
        
        while (currentOffset < endOffset) {
            TlvField field = TlvField.decode(data, currentOffset);
            payload.addField(field);
            currentOffset += field.getEncodedLength();
        }
        
        // Verify we consumed exactly the expected number of bytes
        if (currentOffset != endOffset) {
            throw new ProtocolException("Payload parsing error: consumed " + 
                (currentOffset - offset) + " bytes, expected " + length);
        }
        
        return payload;
    }
    
    // ========== Validation ==========
    
    /**
     * Validate that all required TLVs are present for an operation
     * @param opCode operation code
     * @throws ProtocolException if required fields are missing
     */
    public void validateRequired(OpCode opCode) throws ProtocolException {
        List<TlvType> required = getRequiredFields(opCode);
        List<TlvType> missing = new ArrayList<>();
        
        for (TlvType type : required) {
            if (!hasField(type)) {
                missing.add(type);
            }
        }
        
        if (!missing.isEmpty()) {
            throw new ProtocolException("Missing required TLV fields for " + opCode + ": " + missing);
        }
    }
    
    /**
     * Get list of required TLV types for an operation
     */
    public static List<TlvType> getRequiredFields(OpCode opCode) {
        switch (opCode) {
            case OPEN_ACCOUNT:
                return Arrays.asList(TlvType.USERNAME, TlvType.PASSWORD, TlvType.CURRENCY);
            case CLOSE_ACCOUNT:
                return Arrays.asList(TlvType.USERNAME, TlvType.PASSWORD, TlvType.ACCOUNT_NO);
            case DEPOSIT:
            case WITHDRAW:
                return Arrays.asList(TlvType.USERNAME, TlvType.PASSWORD, TlvType.ACCOUNT_NO, TlvType.AMOUNT_CENTS);
            case REGISTER_CALLBACK:
                return Arrays.asList(TlvType.TTL_SECONDS);
            case UNREGISTER_CALLBACK:
                return Collections.emptyList();
            case QUERY_BALANCE:
                return Arrays.asList(TlvType.USERNAME, TlvType.PASSWORD, TlvType.ACCOUNT_NO);
            case TRANSFER:
                return Arrays.asList(TlvType.USERNAME, TlvType.PASSWORD, TlvType.ACCOUNT_NO, 
                    TlvType.TO_ACCOUNT_NO, TlvType.AMOUNT_CENTS);
            case ACCOUNT_UPDATE:
                // Callback message - no required fields from client
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Payload{");
        boolean first = true;
        for (TlvField field : fields.values()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(field);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
