package edu.ntu.ds.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Fixed Header structure (32 bytes) as defined in Protocol Specification v1.1
 * 
 * Offset | Size | Type | Field       | Description
 * -------|------|------|-------------|------------------------
 *   0    |  2   | u16  | magic       | Fixed value 0xD5D5
 *   2    |  1   | u8   | version     | Protocol version (1)
 *   3    |  1   | u8   | msgType     | 0=REQ, 1=REP, 2=CBK
 *   4    |  2   | u16  | headerLen   | Always 32
 *   6    |  2   | u16  | opCode      | Operation code
 *   8    |  1   | u8   | semantics   | 0=ALO, 1=AMO
 *   9    |  1   | u8   | flags       | bit0=CRC, bit1=Error
 *  10    |  2   | u16  | status      | Reply status code
 *  12    |  8   | u64  | requestId   | Unique request identifier
 *  20    |  4   | u32  | clientId    | Client identifier
 *  24    |  4   | u32  | seqNo       | Client sequence number
 *  28    |  4   | u32  | payloadLen  | Payload length in bytes
 */
public class Header {
    
    private short magic;
    private byte version;
    private MessageType msgType;
    private short headerLen;
    private OpCode opCode;
    private Semantics semantics;
    private byte flags;
    private StatusCode status;
    private long requestId;
    private int clientId;
    private int seqNo;
    private int payloadLen;
    
    /**
     * Default constructor - initializes with default values
     */
    public Header() {
        this.magic = Constants.MAGIC;
        this.version = Constants.VERSION;
        this.headerLen = Constants.HEADER_LENGTH;
        this.msgType = MessageType.REQ;
        this.opCode = OpCode.QUERY_BALANCE;
        this.semantics = Semantics.ALO;
        this.flags = 0;
        this.status = StatusCode.OK;
        this.requestId = 0;
        this.clientId = 0;
        this.seqNo = 0;
        this.payloadLen = 0;
    }
    
    /**
     * Encode header to byte array (32 bytes, Big-Endian)
     * @return encoded header bytes
     */
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(Constants.HEADER_LENGTH);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        buffer.putShort(magic);                    // offset 0: magic (u16)
        buffer.put(version);                       // offset 2: version (u8)
        buffer.put(msgType.getValue());            // offset 3: msgType (u8)
        buffer.putShort(headerLen);                // offset 4: headerLen (u16)
        buffer.putShort(opCode.getValue());        // offset 6: opCode (u16)
        buffer.put(semantics.getValue());          // offset 8: semantics (u8)
        buffer.put(flags);                         // offset 9: flags (u8)
        buffer.putShort(status.getValue());        // offset 10: status (u16)
        buffer.putLong(requestId);                 // offset 12: requestId (u64)
        buffer.putInt(clientId);                   // offset 20: clientId (u32)
        buffer.putInt(seqNo);                      // offset 24: seqNo (u32)
        buffer.putInt(payloadLen);                 // offset 28: payloadLen (u32)
        
        return buffer.array();
    }
    
    /**
     * Decode header from byte array (Big-Endian)
     * @param data byte array containing header (at least 32 bytes)
     * @return decoded Header object
     * @throws ProtocolException if decoding fails
     */
    public static Header decode(byte[] data) throws ProtocolException {
        return decode(data, 0);
    }
    
    /**
     * Decode header from byte array at specified offset
     * @param data byte array containing header
     * @param offset starting offset in the array
     * @return decoded Header object
     * @throws ProtocolException if decoding fails
     */
    public static Header decode(byte[] data, int offset) throws ProtocolException {
        if (data == null || data.length - offset < Constants.HEADER_LENGTH) {
            throw new ProtocolException("Insufficient data for header: expected " + 
                Constants.HEADER_LENGTH + " bytes, got " + (data == null ? 0 : data.length - offset));
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, Constants.HEADER_LENGTH);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        Header header = new Header();
        
        header.magic = buffer.getShort();          // offset 0
        if (header.magic != Constants.MAGIC) {
            throw new ProtocolException("Invalid magic number: 0x" + 
                String.format("%04X", header.magic & 0xFFFF) + ", expected 0xD5D5");
        }
        
        header.version = buffer.get();             // offset 2
        if (header.version != Constants.VERSION) {
            throw new ProtocolException("Unsupported protocol version: " + header.version);
        }
        
        header.msgType = MessageType.fromByte(buffer.get());  // offset 3
        
        header.headerLen = buffer.getShort();      // offset 4
        if (header.headerLen != Constants.HEADER_LENGTH) {
            throw new ProtocolException("Invalid header length: " + header.headerLen);
        }
        
        header.opCode = OpCode.fromShort(buffer.getShort());  // offset 6
        header.semantics = Semantics.fromByte(buffer.get());  // offset 8
        header.flags = buffer.get();               // offset 9
        header.status = StatusCode.fromShort(buffer.getShort()); // offset 10
        header.requestId = buffer.getLong();       // offset 12
        header.clientId = buffer.getInt();         // offset 20
        header.seqNo = buffer.getInt();            // offset 24
        header.payloadLen = buffer.getInt();       // offset 28
        
        // Validate payload length
        if (header.payloadLen < 0) {
            throw new ProtocolException("Invalid payload length: " + header.payloadLen);
        }
        
        return header;
    }
    
    /**
     * Generate requestId from clientId and seqNo
     * requestId = (clientId << 32) | seqNo
     */
    public void generateRequestId() {
        this.requestId = ((long) clientId << 32) | (seqNo & 0xFFFFFFFFL);
    }
    
    /**
     * Set error flag based on status code
     * Error flag (bit1) MUST be set to 1 if and only if status != OK
     */
    public void updateErrorFlag() {
        if (status != StatusCode.OK) {
            flags |= Constants.FLAG_ERROR;
        } else {
            flags &= ~Constants.FLAG_ERROR;
        }
    }
    
    /**
     * Check if CRC flag is set
     */
    public boolean hasCrc() {
        return (flags & Constants.FLAG_CRC) != 0;
    }
    
    /**
     * Check if error flag is set
     */
    public boolean hasError() {
        return (flags & Constants.FLAG_ERROR) != 0;
    }
    
    /**
     * Set CRC flag
     */
    public void setCrcEnabled(boolean enabled) {
        if (enabled) {
            flags |= Constants.FLAG_CRC;
        } else {
            flags &= ~Constants.FLAG_CRC;
        }
    }
    
    // Getters and setters
    
    public short getMagic() {
        return magic;
    }
    
    public byte getVersion() {
        return version;
    }
    
    public MessageType getMsgType() {
        return msgType;
    }
    
    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }
    
    public short getHeaderLen() {
        return headerLen;
    }
    
    public OpCode getOpCode() {
        return opCode;
    }
    
    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }
    
    public Semantics getSemantics() {
        return semantics;
    }
    
    public void setSemantics(Semantics semantics) {
        this.semantics = semantics;
    }
    
    public byte getFlags() {
        return flags;
    }
    
    public void setFlags(byte flags) {
        this.flags = flags;
    }
    
    public StatusCode getStatus() {
        return status;
    }
    
    public void setStatus(StatusCode status) {
        this.status = status;
        updateErrorFlag();
    }
    
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
    
    public int getSeqNo() {
        return seqNo;
    }
    
    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }
    
    public int getPayloadLen() {
        return payloadLen;
    }
    
    public void setPayloadLen(int payloadLen) {
        this.payloadLen = payloadLen;
    }
    
    @Override
    public String toString() {
        return String.format("Header{magic=0x%04X, version=%d, msgType=%s, opCode=%s, " +
            "semantics=%s, flags=0x%02X, status=%s, requestId=%d, clientId=%d, seqNo=%d, payloadLen=%d}",
            magic & 0xFFFF, version, msgType, opCode, semantics, flags & 0xFF, 
            status, requestId, clientId, seqNo, payloadLen);
    }
}
