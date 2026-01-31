package edu.ntu.ds.protocol;

import java.util.Arrays;

/**
 * Stage 1 Verification: Protocol Encoding/Decoding Round-Trip Tests
 * 
 * Run this class to verify that encoding and decoding works correctly.
 * All tests should pass (print PASS) for the protocol implementation to be correct.
 */
public class ProtocolTest {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Protocol v1.1 Round-Trip Tests");
        System.out.println("========================================\n");
        
        testHeaderEncodeDecode();
        testTlvStringField();
        testTlvUint8Field();
        testTlvUint32Field();
        testTlvInt64Field();
        testPayloadEncodeDecode();
        testFullMessageOpenAccount();
        testFullMessageQueryBalance();
        testFullMessageTransfer();
        testMessageWithCrc();
        testReplyMessage();
        testCallbackMessage();
        
        System.out.println("\n========================================");
        System.out.println("Test Results: " + testsPassed + " passed, " + testsFailed + " failed");
        System.out.println("========================================");
        
        if (testsFailed > 0) {
            System.exit(1);
        }
    }
    
    // ========== Header Tests ==========
    
    private static void testHeaderEncodeDecode() {
        System.out.println("Test: Header Encode/Decode");
        try {
            Header original = new Header();
            original.setMsgType(MessageType.REQ);
            original.setOpCode(OpCode.TRANSFER);
            original.setSemantics(Semantics.AMO);
            original.setStatus(StatusCode.OK);
            original.setClientId(12345);
            original.setSeqNo(67890);
            original.generateRequestId();
            original.setPayloadLen(100);
            
            // Encode
            byte[] encoded = original.encode();
            assertEquals("Header length", 32, encoded.length);
            
            // Check magic bytes (Big-Endian)
            assertEquals("Magic byte 0", (byte) 0xD5, encoded[0]);
            assertEquals("Magic byte 1", (byte) 0xD5, encoded[1]);
            
            // Check version
            assertEquals("Version", (byte) 1, encoded[2]);
            
            // Check msgType
            assertEquals("MsgType", (byte) 0, encoded[3]);
            
            // Decode
            Header decoded = Header.decode(encoded);
            
            assertEquals("Magic", original.getMagic(), decoded.getMagic());
            assertEquals("Version", original.getVersion(), decoded.getVersion());
            assertEquals("MsgType", original.getMsgType(), decoded.getMsgType());
            assertEquals("HeaderLen", original.getHeaderLen(), decoded.getHeaderLen());
            assertEquals("OpCode", original.getOpCode(), decoded.getOpCode());
            assertEquals("Semantics", original.getSemantics(), decoded.getSemantics());
            assertEquals("Status", original.getStatus(), decoded.getStatus());
            assertEquals("RequestId", original.getRequestId(), decoded.getRequestId());
            assertEquals("ClientId", original.getClientId(), decoded.getClientId());
            assertEquals("SeqNo", original.getSeqNo(), decoded.getSeqNo());
            assertEquals("PayloadLen", original.getPayloadLen(), decoded.getPayloadLen());
            
            // Verify requestId formula: (clientId << 32) | seqNo
            long expectedRequestId = ((long) 12345 << 32) | (67890 & 0xFFFFFFFFL);
            assertEquals("RequestId formula", expectedRequestId, decoded.getRequestId());
            
            pass("Header Encode/Decode");
        } catch (Exception e) {
            fail("Header Encode/Decode", e);
        }
    }
    
    // ========== TLV Tests ==========
    
    private static void testTlvStringField() {
        System.out.println("Test: TLV String Field");
        try {
            String testValue = "test_username_123";
            TlvField original = TlvField.username(testValue);
            
            byte[] encoded = original.encode();
            
            // Check TLV structure: type(2) + length(2) + value
            assertEquals("TLV type byte 0", (byte) 0x00, encoded[0]);
            assertEquals("TLV type byte 1", (byte) 0x01, encoded[1]);
            
            int length = ((encoded[2] & 0xFF) << 8) | (encoded[3] & 0xFF);
            assertEquals("TLV length", testValue.length(), length);
            
            // Decode
            TlvField decoded = TlvField.decode(encoded, 0);
            assertEquals("TLV type", TlvType.USERNAME, decoded.getType());
            assertEquals("TLV string value", testValue, decoded.getStringValue());
            
            pass("TLV String Field");
        } catch (Exception e) {
            fail("TLV String Field", e);
        }
    }
    
    private static void testTlvUint8Field() {
        System.out.println("Test: TLV Uint8 Field (Currency)");
        try {
            TlvField original = TlvField.currency(Currency.USD);
            
            byte[] encoded = original.encode();
            assertEquals("TLV encoded length", 5, encoded.length); // type(2) + len(2) + value(1)
            
            TlvField decoded = TlvField.decode(encoded, 0);
            assertEquals("TLV type", TlvType.CURRENCY, decoded.getType());
            assertEquals("TLV value", Currency.USD.getValue(), decoded.getUint8Value());
            
            pass("TLV Uint8 Field");
        } catch (Exception e) {
            fail("TLV Uint8 Field", e);
        }
    }
    
    private static void testTlvUint32Field() {
        System.out.println("Test: TLV Uint32 Field (TTL)");
        try {
            int ttlValue = 300;
            TlvField original = TlvField.ttlSeconds(ttlValue);
            
            byte[] encoded = original.encode();
            assertEquals("TLV encoded length", 8, encoded.length); // type(2) + len(2) + value(4)
            
            TlvField decoded = TlvField.decode(encoded, 0);
            assertEquals("TLV type", TlvType.TTL_SECONDS, decoded.getType());
            assertEquals("TLV value", ttlValue, decoded.getUint32Value());
            
            pass("TLV Uint32 Field");
        } catch (Exception e) {
            fail("TLV Uint32 Field", e);
        }
    }
    
    private static void testTlvInt64Field() {
        System.out.println("Test: TLV Int64 Field (Amount)");
        try {
            // Test with a large amount: $1,234,567.89 = 123456789 cents
            long amountCents = 123456789L;
            TlvField original = TlvField.amountCents(amountCents);
            
            byte[] encoded = original.encode();
            assertEquals("TLV encoded length", 12, encoded.length); // type(2) + len(2) + value(8)
            
            TlvField decoded = TlvField.decode(encoded, 0);
            assertEquals("TLV type", TlvType.AMOUNT_CENTS, decoded.getType());
            assertEquals("TLV value", amountCents, decoded.getInt64Value());
            
            // Test negative amount (should work for refunds, etc.)
            TlvField negativeAmount = TlvField.amountCents(-50000L);
            byte[] encodedNeg = negativeAmount.encode();
            TlvField decodedNeg = TlvField.decode(encodedNeg, 0);
            assertEquals("Negative amount", -50000L, decodedNeg.getInt64Value());
            
            pass("TLV Int64 Field");
        } catch (Exception e) {
            fail("TLV Int64 Field", e);
        }
    }
    
    // ========== Payload Tests ==========
    
    private static void testPayloadEncodeDecode() {
        System.out.println("Test: Payload Encode/Decode");
        try {
            Payload original = new Payload();
            original.addField(TlvField.username("alice"));
            original.addField(TlvField.password("secret123"));
            original.addField(TlvField.accountNo("ACC001"));
            original.addField(TlvField.amountCents(50000L)); // $500.00
            
            byte[] encoded = original.encode();
            int expectedLength = original.getEncodedLength();
            assertEquals("Payload encoded length", expectedLength, encoded.length);
            
            Payload decoded = Payload.decode(encoded, 0, encoded.length);
            assertEquals("Field count", 4, decoded.size());
            assertEquals("Username", "alice", decoded.getUsername());
            assertEquals("Password", "secret123", decoded.getPassword());
            assertEquals("AccountNo", "ACC001", decoded.getAccountNo());
            assertEquals("AmountCents", Long.valueOf(50000L), decoded.getAmountCents());
            
            pass("Payload Encode/Decode");
        } catch (Exception e) {
            fail("Payload Encode/Decode", e);
        }
    }
    
    // ========== Full Message Tests ==========
    
    private static void testFullMessageOpenAccount() {
        System.out.println("Test: Full Message - OPEN_ACCOUNT Request");
        try {
            Message original = Message.createRequest(OpCode.OPEN_ACCOUNT, 1001, 1, Semantics.AMO);
            original.addField(TlvField.username("bob"));
            original.addField(TlvField.password("mypassword"));
            original.addField(TlvField.currency(Currency.SGD));
            
            // Validate required fields
            original.getPayload().validateRequired(OpCode.OPEN_ACCOUNT);
            
            byte[] encoded = original.encode();
            
            // Decode
            Message decoded = Message.decode(encoded);
            
            assertEquals("MsgType", MessageType.REQ, decoded.getHeader().getMsgType());
            assertEquals("OpCode", OpCode.OPEN_ACCOUNT, decoded.getHeader().getOpCode());
            assertEquals("Semantics", Semantics.AMO, decoded.getHeader().getSemantics());
            assertEquals("ClientId", 1001, decoded.getHeader().getClientId());
            assertEquals("SeqNo", 1, decoded.getHeader().getSeqNo());
            assertEquals("Username", "bob", decoded.getPayload().getUsername());
            assertEquals("Password", "mypassword", decoded.getPayload().getPassword());
            assertEquals("Currency", Currency.SGD, decoded.getPayload().getCurrency());
            
            pass("Full Message - OPEN_ACCOUNT Request");
        } catch (Exception e) {
            fail("Full Message - OPEN_ACCOUNT Request", e);
        }
    }
    
    private static void testFullMessageQueryBalance() {
        System.out.println("Test: Full Message - QUERY_BALANCE Request");
        try {
            Message original = Message.createRequest(OpCode.QUERY_BALANCE, 2001, 5, Semantics.ALO);
            original.addField(TlvField.username("charlie"));
            original.addField(TlvField.password("pass456"));
            original.addField(TlvField.accountNo("ACC-12345"));
            
            byte[] encoded = original.encode();
            Message decoded = Message.decode(encoded);
            
            assertEquals("OpCode", OpCode.QUERY_BALANCE, decoded.getHeader().getOpCode());
            assertEquals("Semantics", Semantics.ALO, decoded.getHeader().getSemantics());
            assertEquals("Username", "charlie", decoded.getPayload().getUsername());
            assertEquals("AccountNo", "ACC-12345", decoded.getPayload().getAccountNo());
            
            pass("Full Message - QUERY_BALANCE Request");
        } catch (Exception e) {
            fail("Full Message - QUERY_BALANCE Request", e);
        }
    }
    
    private static void testFullMessageTransfer() {
        System.out.println("Test: Full Message - TRANSFER Request");
        try {
            Message original = Message.createRequest(OpCode.TRANSFER, 3001, 10, Semantics.AMO);
            original.addField(TlvField.username("david"));
            original.addField(TlvField.password("transferpwd"));
            original.addField(TlvField.accountNo("FROM-ACC"));
            original.addField(TlvField.toAccountNo("TO-ACC"));
            original.addField(TlvField.amountCents(100000L)); // $1000.00
            original.addField(TlvField.note("Payment for services"));
            
            // Validate required fields
            original.getPayload().validateRequired(OpCode.TRANSFER);
            
            byte[] encoded = original.encode();
            Message decoded = Message.decode(encoded);
            
            assertEquals("OpCode", OpCode.TRANSFER, decoded.getHeader().getOpCode());
            assertEquals("FromAccount", "FROM-ACC", decoded.getPayload().getAccountNo());
            assertEquals("ToAccount", "TO-ACC", decoded.getPayload().getToAccountNo());
            assertEquals("Amount", Long.valueOf(100000L), decoded.getPayload().getAmountCents());
            assertEquals("Note", "Payment for services", decoded.getPayload().getNote());
            
            pass("Full Message - TRANSFER Request");
        } catch (Exception e) {
            fail("Full Message - TRANSFER Request", e);
        }
    }
    
    private static void testMessageWithCrc() {
        System.out.println("Test: Message with CRC32");
        try {
            Message original = Message.createRequest(OpCode.DEPOSIT, 4001, 1, Semantics.ALO);
            original.setCrcEnabled(true);
            original.addField(TlvField.username("eve"));
            original.addField(TlvField.password("crctest"));
            original.addField(TlvField.accountNo("CRC-ACC"));
            original.addField(TlvField.amountCents(25000L));
            
            byte[] encoded = original.encode();
            
            // Verify CRC is appended (message should be header + payload + 4 bytes CRC)
            int expectedLen = Constants.HEADER_LENGTH + original.getPayload().getEncodedLength() + 4;
            assertEquals("Encoded length with CRC", expectedLen, encoded.length);
            
            // Decode and verify CRC
            Message decoded = Message.decode(encoded);
            assertTrue("CRC flag set", decoded.getHeader().hasCrc());
            assertNotNull("CRC value present", decoded.getCrc32());
            
            // Verify round-trip
            assertEquals("Username", "eve", decoded.getPayload().getUsername());
            
            pass("Message with CRC32");
        } catch (Exception e) {
            fail("Message with CRC32", e);
        }
    }
    
    private static void testReplyMessage() {
        System.out.println("Test: Reply Message");
        try {
            // Create request
            Message request = Message.createRequest(OpCode.QUERY_BALANCE, 5001, 3, Semantics.AMO);
            request.addField(TlvField.username("frank"));
            request.addField(TlvField.password("pwd"));
            request.addField(TlvField.accountNo("ACC-5001"));
            
            // Create reply
            Message reply = Message.createReply(request, StatusCode.OK);
            reply.addField(TlvField.amountCents(500000L)); // $5000.00 balance
            
            byte[] encoded = reply.encode();
            Message decoded = Message.decode(encoded);
            
            assertEquals("MsgType", MessageType.REP, decoded.getHeader().getMsgType());
            assertEquals("Status", StatusCode.OK, decoded.getHeader().getStatus());
            assertEquals("RequestId matches", request.getHeader().getRequestId(), decoded.getHeader().getRequestId());
            assertEquals("Error flag", false, decoded.getHeader().hasError());
            assertEquals("Balance", Long.valueOf(500000L), decoded.getPayload().getAmountCents());
            
            // Test error reply
            Message errorReply = Message.createReply(request, StatusCode.NOT_FOUND);
            byte[] errorEncoded = errorReply.encode();
            Message decodedError = Message.decode(errorEncoded);
            
            assertEquals("Error status", StatusCode.NOT_FOUND, decodedError.getHeader().getStatus());
            assertEquals("Error flag set", true, decodedError.getHeader().hasError());
            
            pass("Reply Message");
        } catch (Exception e) {
            fail("Reply Message", e);
        }
    }
    
    private static void testCallbackMessage() {
        System.out.println("Test: Callback Message (ACCOUNT_UPDATE)");
        try {
            Message callback = Message.createCallback(OpCode.ACCOUNT_UPDATE, 6001);
            callback.addField(TlvField.accountNo("ACC-UPDATED"));
            callback.addField(TlvField.amountCents(750000L)); // New balance
            
            byte[] encoded = callback.encode();
            Message decoded = Message.decode(encoded);
            
            assertEquals("MsgType", MessageType.CBK, decoded.getHeader().getMsgType());
            assertEquals("OpCode", OpCode.ACCOUNT_UPDATE, decoded.getHeader().getOpCode());
            assertEquals("AccountNo", "ACC-UPDATED", decoded.getPayload().getAccountNo());
            assertEquals("Balance", Long.valueOf(750000L), decoded.getPayload().getAmountCents());
            
            pass("Callback Message (ACCOUNT_UPDATE)");
        } catch (Exception e) {
            fail("Callback Message", e);
        }
    }
    
    // ========== Test Utilities ==========
    
    private static void assertEquals(String name, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
    
    private static void assertTrue(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name + ": expected true");
        }
    }
    
    private static void assertNotNull(String name, Object obj) {
        if (obj == null) {
            throw new AssertionError(name + ": expected non-null");
        }
    }
    
    private static void pass(String testName) {
        System.out.println("  [PASS] " + testName);
        testsPassed++;
    }
    
    private static void fail(String testName, Exception e) {
        System.out.println("  [FAIL] " + testName + ": " + e.getMessage());
        e.printStackTrace();
        testsFailed++;
    }
}
