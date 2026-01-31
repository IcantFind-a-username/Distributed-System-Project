package edu.ntu.ds.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

/**
 * Complete protocol message containing header, payload, and optional CRC32
 * 
 * Message structure:
 * - Fixed Header (32 bytes)
 * - Payload (variable length)
 * - Optional CRC32 checksum (4 bytes) if flags.bit0 is set
 */
public class Message {
    
    private Header header;
    private Payload payload;
    private Integer crc32; // null if CRC not enabled
    
    public Message() {
        this.header = new Header();
        this.payload = new Payload();
        this.crc32 = null;
    }
    
    public Message(Header header, Payload payload) {
        this.header = header;
        this.payload = payload;
        this.crc32 = null;
    }
    
    // Factory methods
    
    /**
     * Create a request message
     */
    public static Message createRequest(OpCode opCode, int clientId, int seqNo, Semantics semantics) {
        Message msg = new Message();
        msg.header.setMsgType(MessageType.REQ);
        msg.header.setOpCode(opCode);
        msg.header.setClientId(clientId);
        msg.header.setSeqNo(seqNo);
        msg.header.setSemantics(semantics);
        msg.header.generateRequestId();
        return msg;
    }
    
    /**
     * Create a reply message from a request
     */
    public static Message createReply(Message request, StatusCode status) {
        Message msg = new Message();
        msg.header.setMsgType(MessageType.REP);
        msg.header.setOpCode(request.getHeader().getOpCode());
        msg.header.setClientId(request.getHeader().getClientId());
        msg.header.setSeqNo(request.getHeader().getSeqNo());
        msg.header.setRequestId(request.getHeader().getRequestId());
        msg.header.setSemantics(request.getHeader().getSemantics());
        msg.header.setStatus(status);
        return msg;
    }
    
    /**
     * Create a callback notification message
     */
    public static Message createCallback(OpCode opCode, int clientId) {
        Message msg = new Message();
        msg.header.setMsgType(MessageType.CBK);
        msg.header.setOpCode(opCode);
        msg.header.setClientId(clientId);
        msg.header.setSeqNo(0);
        msg.header.setRequestId(0);
        return msg;
    }
    
    // Encoding
    
    /**
     * Encode complete message to bytes
     * @return encoded message bytes
     */
    public byte[] encode() {
        // Encode payload first to get length
        byte[] payloadBytes = payload.encode();
        header.setPayloadLen(payloadBytes.length);
        
        // Calculate total message size
        int totalSize = Constants.HEADER_LENGTH + payloadBytes.length;
        if (header.hasCrc()) {
            totalSize += Constants.CRC32_SIZE;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        // Encode header
        buffer.put(header.encode());
        
        // Encode payload
        buffer.put(payloadBytes);
        
        // Calculate and append CRC32 if enabled
        if (header.hasCrc()) {
            CRC32 crc = new CRC32();
            crc.update(buffer.array(), 0, Constants.HEADER_LENGTH + payloadBytes.length);
            int crcValue = (int) crc.getValue();
            buffer.putInt(crcValue);
            this.crc32 = crcValue;
        }
        
        return buffer.array();
    }
    
    // Decoding
    
    /**
     * Decode message from bytes
     * @param data byte array containing complete message
     * @return decoded Message
     * @throws ProtocolException if decoding fails
     */
    public static Message decode(byte[] data) throws ProtocolException {
        return decode(data, 0, data.length);
    }
    
    /**
     * Decode message from bytes with offset and length
     * @param data byte array
     * @param offset starting offset
     * @param length total message length
     * @return decoded Message
     * @throws ProtocolException if decoding fails
     */
    public static Message decode(byte[] data, int offset, int length) throws ProtocolException {
        if (length < Constants.HEADER_LENGTH) {
            throw new ProtocolException("Message too short: " + length + " bytes");
        }
        
        Message msg = new Message();
        
        // Decode header
        msg.header = Header.decode(data, offset);
        
        int payloadLen = msg.header.getPayloadLen();
        int expectedLen = Constants.HEADER_LENGTH + payloadLen;
        
        // Check for CRC
        if (msg.header.hasCrc()) {
            expectedLen += Constants.CRC32_SIZE;
        }
        
        if (length < expectedLen) {
            throw new ProtocolException("Message length mismatch: expected " + 
                expectedLen + ", got " + length);
        }
        
        // Decode payload
        int payloadOffset = offset + Constants.HEADER_LENGTH;
        msg.payload = Payload.decode(data, payloadOffset, payloadLen);
        
        // Verify CRC if present
        if (msg.header.hasCrc()) {
            int crcOffset = payloadOffset + payloadLen;
            ByteBuffer crcBuffer = ByteBuffer.wrap(data, crcOffset, 4);
            crcBuffer.order(ByteOrder.BIG_ENDIAN);
            int receivedCrc = crcBuffer.getInt();
            msg.crc32 = receivedCrc;
            
            // Calculate CRC over header + payload
            CRC32 crc = new CRC32();
            crc.update(data, offset, Constants.HEADER_LENGTH + payloadLen);
            int calculatedCrc = (int) crc.getValue();
            
            if (receivedCrc != calculatedCrc) {
                throw new ProtocolException("CRC mismatch: received 0x" + 
                    String.format("%08X", receivedCrc) + ", calculated 0x" + 
                    String.format("%08X", calculatedCrc));
            }
        }
        
        return msg;
    }
    
    // Getters and setters
    
    public Header getHeader() {
        return header;
    }
    
    public void setHeader(Header header) {
        this.header = header;
    }
    
    public Payload getPayload() {
        return payload;
    }
    
    public void setPayload(Payload payload) {
        this.payload = payload;
    }
    
    public Integer getCrc32() {
        return crc32;
    }
    
    /**
     * Enable or disable CRC32 checksum
     */
    public void setCrcEnabled(boolean enabled) {
        header.setCrcEnabled(enabled);
        if (!enabled) {
            crc32 = null;
        }
    }
    
    /**
     * Add a TLV field to the payload
     */
    public Message addField(TlvField field) {
        payload.addField(field);
        return this;
    }
    
    /**
     * Get the total encoded length of the message
     */
    public int getEncodedLength() {
        int len = Constants.HEADER_LENGTH + payload.getEncodedLength();
        if (header.hasCrc()) {
            len += Constants.CRC32_SIZE;
        }
        return len;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message{\n");
        sb.append("  ").append(header).append("\n");
        sb.append("  ").append(payload).append("\n");
        if (crc32 != null) {
            sb.append("  CRC32=0x").append(String.format("%08X", crc32)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
