package edu.ntu.ds.service;

import edu.ntu.ds.network.Logger;
import edu.ntu.ds.network.UdpServer;
import edu.ntu.ds.protocol.*;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Request Processor that integrates banking service with protocol handling.
 * 
 * Implements At-Least-Once (ALO) and At-Most-Once (AMO) invocation semantics:
 * - ALO: Execute every received request (may execute same request multiple times)
 * - AMO: Cache replies and return cached reply for duplicate requests
 * 
 * For demonstration, both semantics are supported and can be controlled per-request
 * via the header.semantics field.
 */
public class RequestProcessor implements UdpServer.RequestHandler {
    
    private final BankingService bankingService;
    private final CallbackRegistry callbackRegistry;
    private final AmoCache amoCache;
    private final Logger logger;
    
    // Reference to server for sending callbacks
    private UdpServer server;
    
    public RequestProcessor(BankingService bankingService) {
        this.bankingService = bankingService;
        this.callbackRegistry = new CallbackRegistry();
        this.amoCache = new AmoCache();
        this.logger = new Logger("PROCESSOR");
    }
    
    /**
     * Set server reference for sending callbacks
     */
    public void setServer(UdpServer server) {
        this.server = server;
    }
    
    public CallbackRegistry getCallbackRegistry() {
        return callbackRegistry;
    }
    
    public AmoCache getAmoCache() {
        return amoCache;
    }
    
    @Override
    public Message handleRequest(Message request, InetSocketAddress clientAddress) {
        Header reqHeader = request.getHeader();
        int clientId = reqHeader.getClientId();
        long requestId = reqHeader.getRequestId();
        Semantics semantics = reqHeader.getSemantics();
        OpCode opCode = reqHeader.getOpCode();
        
        // Check for duplicate request (AMO semantics)
        if (semantics == Semantics.AMO) {
            byte[] cachedReply = amoCache.get(clientId, requestId);
            if (cachedReply != null) {
                logger.info("AMO cache hit for clientId=" + clientId + ", requestId=" + requestId);
                // Return cached reply (decode it to Message for logging, but send original bytes)
                try {
                    Message cachedMsg = Message.decode(cachedReply);
                    if (server != null) {
                        server.sendReply(cachedMsg, clientAddress, true);
                    }
                    return null; // Already sent via server.sendReply
                } catch (ProtocolException e) {
                    logger.error("Failed to decode cached reply", e);
                }
            }
        }
        
        // Execute the requested operation
        Message reply;
        boolean stateChanged = false;
        String affectedAccountNo = null;
        Long newBalance = null;
        
        try {
            Payload payload = request.getPayload();
            
            // Validate required TLVs
            payload.validateRequired(opCode);
            
            // Process based on operation
            BankingService.OperationResult result;
            
            switch (opCode) {
                case OPEN_ACCOUNT:
                    Long initialBalance = payload.getAmountCents();
                    result = bankingService.openAccount(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getCurrency(),
                        initialBalance != null ? initialBalance : 0L
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.accountNo(result.accountNo));
                        reply.addField(TlvField.amountCents(result.balanceCents)); // Return initial balance
                        stateChanged = true;
                        affectedAccountNo = result.accountNo;
                        newBalance = result.balanceCents;
                    }
                    break;
                    
                case CLOSE_ACCOUNT:
                    result = bankingService.closeAccount(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getAccountNo()
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.amountCents(result.balanceCents)); // Final balance returned
                        stateChanged = true;
                        affectedAccountNo = payload.getAccountNo();
                    }
                    break;
                    
                case DEPOSIT:
                    result = bankingService.deposit(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getAccountNo(),
                        payload.getCurrency(),  // Currency type for validation
                        payload.getAmountCents()
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.amountCents(result.balanceCents)); // New balance
                        stateChanged = true;
                        affectedAccountNo = payload.getAccountNo();
                        newBalance = result.balanceCents;
                    }
                    break;
                    
                case WITHDRAW:
                    result = bankingService.withdraw(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getAccountNo(),
                        payload.getCurrency(),  // Currency type for validation
                        payload.getAmountCents()
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.amountCents(result.balanceCents)); // New balance
                        stateChanged = true;
                        affectedAccountNo = payload.getAccountNo();
                        newBalance = result.balanceCents;
                    }
                    break;
                    
                case QUERY_BALANCE:
                    result = bankingService.queryBalance(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getAccountNo()
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.amountCents(result.balanceCents));
                        reply.addField(TlvField.currency(result.account.getCurrency()));
                    }
                    // Query is idempotent, no state change
                    break;
                    
                case TRANSFER:
                    logger.info("Processing TRANSFER: from=" + payload.getAccountNo() + 
                        " to=" + payload.getToAccountNo() + 
                        " amount=" + Logger.formatCents(payload.getAmountCents()));
                    
                    result = bankingService.transfer(
                        payload.getUsername(),
                        payload.getPassword(),
                        payload.getAccountNo(),
                        payload.getToAccountNo(),
                        payload.getAmountCents()
                    );
                    reply = createReply(request, result);
                    if (result.status == StatusCode.OK) {
                        reply.addField(TlvField.amountCents(result.balanceCents)); // New balance of source
                        stateChanged = true;
                        affectedAccountNo = payload.getAccountNo();
                        newBalance = result.balanceCents;
                        
                        // Also notify about destination account update
                        Account toAccount = bankingService.getAccountStore()
                            .getByAccountNo(payload.getToAccountNo());
                        if (toAccount != null) {
                            sendAccountUpdateCallback(payload.getToAccountNo(), 
                                toAccount.getBalanceCents(), clientId);
                        }
                    }
                    break;
                    
                case REGISTER_CALLBACK:
                    Integer ttl = payload.getTtlSeconds();
                    if (ttl == null || ttl <= 0) {
                        reply = createReply(request, BankingService.OperationResult.error(StatusCode.BAD_REQUEST));
                    } else {
                        callbackRegistry.register(clientId, clientAddress, ttl);
                        logger.info("Client " + clientId + " registered for callbacks, TTL=" + ttl + "s");
                        reply = createReply(request, BankingService.OperationResult.success());
                    }
                    break;
                    
                case UNREGISTER_CALLBACK:
                    boolean wasRegistered = callbackRegistry.unregister(clientId);
                    logger.info("Client " + clientId + " unregistered from callbacks (was registered: " + wasRegistered + ")");
                    reply = createReply(request, BankingService.OperationResult.success());
                    break;
                    
                default:
                    logger.warn("Unsupported operation: " + opCode);
                    reply = createReply(request, BankingService.OperationResult.error(StatusCode.BAD_REQUEST));
            }
            
        } catch (ProtocolException e) {
            logger.error("Protocol error processing request", e);
            reply = Message.createReply(request, StatusCode.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Internal error processing request", e);
            reply = Message.createReply(request, StatusCode.INTERNAL_ERROR);
        }
        
        // Cache reply for AMO semantics
        if (semantics == Semantics.AMO) {
            byte[] replyBytes = reply.encode();
            amoCache.put(clientId, requestId, replyBytes);
            logger.debug("Cached reply for AMO: clientId=" + clientId + ", requestId=" + requestId);
        }
        
        // Send callbacks to registered monitors
        if (stateChanged && affectedAccountNo != null && newBalance != null) {
            sendAccountUpdateCallback(affectedAccountNo, newBalance, clientId);
        }
        
        return reply;
    }
    
    /**
     * Create a reply message from operation result
     */
    private Message createReply(Message request, BankingService.OperationResult result) {
        return Message.createReply(request, result.status);
    }
    
    /**
     * Send ACCOUNT_UPDATE callback to registered clients
     */
    private void sendAccountUpdateCallback(String accountNo, long newBalance, int excludeClientId) {
        if (server == null) {
            return;
        }
        
        Set<InetSocketAddress> recipients = callbackRegistry.getRegisteredAddresses(excludeClientId);
        if (recipients.isEmpty()) {
            return;
        }
        
        // Create callback message
        Message callback = Message.createCallback(OpCode.ACCOUNT_UPDATE, 0);
        callback.addField(TlvField.accountNo(accountNo));
        callback.addField(TlvField.amountCents(newBalance));
        
        // Send to all registered clients (best-effort)
        for (InetSocketAddress addr : recipients) {
            server.sendCallback(callback, addr);
        }
    }
}
