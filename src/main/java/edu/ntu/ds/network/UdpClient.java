package edu.ntu.ds.network;

import edu.ntu.ds.protocol.*;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UDP Client for the Distributed Banking System.
 * 
 * Implements the client retry policy as specified in Protocol v1.1:
 * - Initial timeout: 500ms
 * - Maximum retries: 5
 * - Backoff strategy: exponential backoff
 * - All retransmissions reuse the same requestId
 */
public class UdpClient {
    
    private static final int BUFFER_SIZE = 65535;
    
    private final int clientId;
    private final InetSocketAddress serverAddress;
    private final Logger logger;
    
    private DatagramSocket socket;
    private final AtomicInteger seqNoCounter;
    
    // Retry policy configuration (per Protocol v1.1)
    private int initialTimeoutMs = Constants.INITIAL_TIMEOUT_MS;  // 500ms
    private int maxRetries = Constants.MAX_RETRIES;               // 5
    
    // Default semantics for this client
    private Semantics defaultSemantics = Semantics.AMO;
    
    /**
     * Callback listener for receiving async notifications
     */
    public interface CallbackListener {
        void onCallback(Message callback);
    }
    
    private CallbackListener callbackListener;
    private volatile boolean listening;
    private Thread listenerThread;
    
    public UdpClient(int clientId, String serverHost, int serverPort) throws SocketException {
        this.clientId = clientId;
        this.serverAddress = new InetSocketAddress(serverHost, serverPort);
        this.logger = new Logger("CLIENT-" + clientId);
        this.seqNoCounter = new AtomicInteger(0);
        this.socket = new DatagramSocket();
    }
    
    /**
     * Set the default invocation semantics
     */
    public void setDefaultSemantics(Semantics semantics) {
        this.defaultSemantics = semantics;
        logger.info("Default semantics set to: " + semantics);
    }
    
    /**
     * Set retry policy parameters
     */
    public void setRetryPolicy(int initialTimeoutMs, int maxRetries) {
        this.initialTimeoutMs = initialTimeoutMs;
        this.maxRetries = maxRetries;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    /**
     * Send a request and wait for reply with retry logic
     * @param request the request message (seqNo and requestId will be set)
     * @return reply message, or null if max retries exceeded
     */
    public Message sendRequest(Message request) throws IOException {
        return sendRequest(request, defaultSemantics);
    }
    
    /**
     * Send a request with specific semantics
     */
    public Message sendRequest(Message request, Semantics semantics) throws IOException {
        // Set client identification
        int seqNo = seqNoCounter.incrementAndGet();
        request.getHeader().setClientId(clientId);
        request.getHeader().setSeqNo(seqNo);
        request.getHeader().setSemantics(semantics);
        request.getHeader().generateRequestId();
        
        long requestId = request.getHeader().getRequestId();
        byte[] requestData = request.encode();
        
        int currentTimeout = initialTimeoutMs;
        String serverAddrStr = serverAddress.getAddress().getHostAddress() + ":" + serverAddress.getPort();
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                // Send request
                logger.logSend(request, serverAddrStr, attempt);
                DatagramPacket sendPacket = new DatagramPacket(
                    requestData, requestData.length, 
                    serverAddress.getAddress(), serverAddress.getPort());
                socket.send(sendPacket);
                
                // Wait for reply
                socket.setSoTimeout(currentTimeout);
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                
                while (true) {
                    socket.receive(receivePacket);
                    
                    try {
                        Message reply = Message.decode(
                            receivePacket.getData(),
                            receivePacket.getOffset(),
                            receivePacket.getLength()
                        );
                        
                        // Check if this is a callback notification (handle async)
                        if (reply.getHeader().getMsgType() == MessageType.CBK) {
                            if (callbackListener != null) {
                                callbackListener.onCallback(reply);
                            } else {
                                logger.info("Received callback notification (no listener registered)");
                            }
                            // Continue waiting for actual reply
                            continue;
                        }
                        
                        // Check if reply matches our request
                        if (reply.getHeader().getRequestId() != requestId) {
                            logger.debug("Ignoring reply with mismatched requestId: " + 
                                reply.getHeader().getRequestId() + " (expected " + requestId + ")");
                            continue;
                        }
                        
                        logger.logReceive(reply, serverAddrStr);
                        return reply;
                        
                    } catch (edu.ntu.ds.protocol.ProtocolException e) {
                        logger.warn("Failed to decode reply: " + e.getMessage());
                        // Continue waiting
                    }
                }
                
            } catch (SocketTimeoutException e) {
                logger.logTimeout(attempt, maxRetries + 1, currentTimeout);
                
                if (attempt <= maxRetries) {
                    // Exponential backoff
                    currentTimeout *= 2;
                }
            }
        }
        
        logger.error("Request failed after " + (maxRetries + 1) + " attempts");
        return null;
    }
    
    /**
     * Start listening for callback notifications in background
     */
    public void startCallbackListener(CallbackListener listener) {
        this.callbackListener = listener;
        this.listening = true;
        
        // Note: Callbacks are handled in sendRequest() loop for simplicity
        // For a dedicated listener, we would need a separate socket or thread
        logger.info("Callback listener registered");
    }
    
    /**
     * Stop the callback listener
     */
    public void stopCallbackListener() {
        this.listening = false;
        this.callbackListener = null;
        logger.info("Callback listener stopped");
    }
    
    /**
     * Listen for callback notifications only (monitor mode)
     * @param durationSeconds how long to listen
     */
    public void listenForCallbacks(int durationSeconds, CallbackListener listener) throws IOException {
        this.callbackListener = listener;
        logger.info("Listening for callbacks for " + durationSeconds + " seconds...");
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (System.currentTimeMillis() < endTime) {
            try {
                int remainingTime = (int) (endTime - System.currentTimeMillis());
                if (remainingTime <= 0) break;
                
                socket.setSoTimeout(Math.min(remainingTime, 1000));
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                try {
                    Message msg = Message.decode(packet.getData(), packet.getOffset(), packet.getLength());
                    if (msg.getHeader().getMsgType() == MessageType.CBK) {
                        String source = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                        logger.info("CALLBACK received from " + source);
                        listener.onCallback(msg);
                    }
                } catch (edu.ntu.ds.protocol.ProtocolException e) {
                    logger.warn("Failed to decode message: " + e.getMessage());
                }
                
            } catch (SocketTimeoutException e) {
                // Continue listening
            }
        }
        
        logger.info("Callback listening ended");
    }
    
    /**
     * Close the client socket
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    /**
     * Get the local port this client is bound to (for callback registration)
     */
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    // Request factory methods
    
    /**
     * Create an OPEN_ACCOUNT request with initial balance
     */
    public Message createOpenAccountRequest(String username, String password, Currency currency, long initialBalance) {
        Message msg = Message.createRequest(OpCode.OPEN_ACCOUNT, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.currency(currency));
        msg.addField(TlvField.amountCents(initialBalance));
        return msg;
    }
    
    /**
     * Create an OPEN_ACCOUNT request (backward compatibility, zero initial balance)
     */
    public Message createOpenAccountRequest(String username, String password, Currency currency) {
        return createOpenAccountRequest(username, password, currency, 0);
    }
    
    /**
     * Create a CLOSE_ACCOUNT request
     */
    public Message createCloseAccountRequest(String username, String password, String accountNo) {
        Message msg = Message.createRequest(OpCode.CLOSE_ACCOUNT, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        return msg;
    }
    
    /**
     * Create a DEPOSIT request with currency type
     */
    public Message createDepositRequest(String username, String password, String accountNo, 
            Currency currency, long amountCents) {
        Message msg = Message.createRequest(OpCode.DEPOSIT, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        msg.addField(TlvField.currency(currency));
        msg.addField(TlvField.amountCents(amountCents));
        return msg;
    }
    
    /**
     * Create a DEPOSIT request (backward compatibility, no currency validation)
     */
    public Message createDepositRequest(String username, String password, String accountNo, long amountCents) {
        Message msg = Message.createRequest(OpCode.DEPOSIT, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        msg.addField(TlvField.amountCents(amountCents));
        return msg;
    }
    
    /**
     * Create a WITHDRAW request with currency type
     */
    public Message createWithdrawRequest(String username, String password, String accountNo, 
            Currency currency, long amountCents) {
        Message msg = Message.createRequest(OpCode.WITHDRAW, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        msg.addField(TlvField.currency(currency));
        msg.addField(TlvField.amountCents(amountCents));
        return msg;
    }
    
    /**
     * Create a WITHDRAW request (backward compatibility, no currency validation)
     */
    public Message createWithdrawRequest(String username, String password, String accountNo, long amountCents) {
        Message msg = Message.createRequest(OpCode.WITHDRAW, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        msg.addField(TlvField.amountCents(amountCents));
        return msg;
    }
    
    /**
     * Create a QUERY_BALANCE request
     */
    public Message createQueryBalanceRequest(String username, String password, String accountNo) {
        Message msg = Message.createRequest(OpCode.QUERY_BALANCE, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(accountNo));
        return msg;
    }
    
    /**
     * Create a TRANSFER request
     */
    public Message createTransferRequest(String username, String password, 
            String fromAccount, String toAccount, long amountCents) {
        Message msg = Message.createRequest(OpCode.TRANSFER, clientId, 0, defaultSemantics);
        msg.addField(TlvField.username(username));
        msg.addField(TlvField.password(password));
        msg.addField(TlvField.accountNo(fromAccount));
        msg.addField(TlvField.toAccountNo(toAccount));
        msg.addField(TlvField.amountCents(amountCents));
        return msg;
    }
    
    /**
     * Create a REGISTER_CALLBACK request
     */
    public Message createRegisterCallbackRequest(int ttlSeconds) {
        Message msg = Message.createRequest(OpCode.REGISTER_CALLBACK, clientId, 0, defaultSemantics);
        msg.addField(TlvField.ttlSeconds(ttlSeconds));
        return msg;
    }
    
    /**
     * Create an UNREGISTER_CALLBACK request
     */
    public Message createUnregisterCallbackRequest() {
        Message msg = Message.createRequest(OpCode.UNREGISTER_CALLBACK, clientId, 0, defaultSemantics);
        return msg;
    }
}
