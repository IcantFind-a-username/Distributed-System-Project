package edu.ntu.ds.network;

import edu.ntu.ds.protocol.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP Server for the Distributed Banking System.
 * 
 * Receives request messages, processes them through a RequestHandler,
 * and sends back reply messages.
 * 
 * Features:
 * - Single-threaded event loop for simplicity
 * - Packet loss simulation support
 * - Structured logging
 */
public class UdpServer {
    
    private static final int BUFFER_SIZE = 65535;
    
    private final int port;
    private final RequestHandler handler;
    private final PacketLossSimulator lossSimulator;
    private final Logger logger;
    
    private DatagramSocket socket;
    private final AtomicBoolean running;
    
    /**
     * Callback interface for processing requests
     */
    public interface RequestHandler {
        /**
         * Process a request message and return a reply
         * @param request the incoming request message
         * @param clientAddress client's address (for callback registration)
         * @return reply message to send back
         */
        Message handleRequest(Message request, InetSocketAddress clientAddress);
    }
    
    public UdpServer(int port, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.lossSimulator = new PacketLossSimulator();
        this.logger = new Logger("SERVER");
        this.running = new AtomicBoolean(false);
    }
    
    /**
     * Enable packet loss simulation
     * @param requestLoss probability of dropping incoming requests (0.0-1.0)
     * @param replyLoss probability of dropping outgoing replies (0.0-1.0)
     */
    public void enableLossSimulation(double requestLoss, double replyLoss) {
        lossSimulator.enable(requestLoss, replyLoss);
        logger.info("Packet loss simulation enabled: request=" + 
            (requestLoss * 100) + "%, reply=" + (replyLoss * 100) + "%");
    }
    
    /**
     * Disable packet loss simulation
     */
    public void disableLossSimulation() {
        lossSimulator.disable();
        logger.info("Packet loss simulation disabled");
    }
    
    public PacketLossSimulator getLossSimulator() {
        return lossSimulator;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    /**
     * Start the server
     */
    public void start() throws SocketException {
        socket = new DatagramSocket(port);
        running.set(true);
        
        logger.info("Server started on port " + port);
        logger.info("Loss simulation: " + (lossSimulator.isEnabled() ? "ENABLED" : "DISABLED"));
        
        // Main event loop
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                processPacket(packet);
                
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("Error receiving packet", e);
                }
            }
        }
        
        logger.info("Server stopped");
    }
    
    /**
     * Process a received packet
     */
    private void processPacket(DatagramPacket packet) {
        InetSocketAddress clientAddr = new InetSocketAddress(
            packet.getAddress(), packet.getPort());
        String clientAddrStr = clientAddr.getAddress().getHostAddress() + ":" + clientAddr.getPort();
        
        try {
            // Simulate request loss
            if (lossSimulator.shouldDropRequest()) {
                // We need to peek at the requestId for logging, so decode header only
                if (packet.getLength() >= Constants.HEADER_LENGTH) {
                    Header h = Header.decode(packet.getData(), packet.getOffset());
                    logger.logSimulatedLoss("REQUEST", h.getRequestId());
                }
                return;
            }
            
            // Decode the message
            Message request = Message.decode(
                packet.getData(), 
                packet.getOffset(), 
                packet.getLength()
            );
            
            // Validate message type
            if (request.getHeader().getMsgType() != MessageType.REQ) {
                logger.warn("Received non-request message type: " + 
                    request.getHeader().getMsgType() + " from " + clientAddrStr);
                return;
            }
            
            // Log the request
            logger.logRequest(request, clientAddrStr);
            
            // Process through handler
            Message reply = handler.handleRequest(request, clientAddr);
            
            if (reply != null) {
                sendReply(reply, clientAddr, false);
            }
            
        } catch (ProtocolException e) {
            logger.error("Protocol error from " + clientAddrStr, e);
            // Could send an error reply here, but without a valid request we can't
        } catch (Exception e) {
            logger.error("Error processing request from " + clientAddrStr, e);
        }
    }
    
    /**
     * Send a reply message to a client
     * @param reply the reply message
     * @param clientAddr destination address
     * @param fromCache whether this reply is from AMO cache
     */
    public void sendReply(Message reply, InetSocketAddress clientAddr, boolean fromCache) {
        String clientAddrStr = clientAddr.getAddress().getHostAddress() + ":" + clientAddr.getPort();
        
        try {
            // Simulate reply loss
            if (lossSimulator.shouldDropReply()) {
                logger.logSimulatedLoss("REPLY", reply.getHeader().getRequestId());
                return;
            }
            
            byte[] data = reply.encode();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, clientAddr.getAddress(), clientAddr.getPort());
            socket.send(packet);
            
            logger.logReply(reply, fromCache, clientAddrStr);
            
        } catch (IOException e) {
            logger.error("Error sending reply to " + clientAddrStr, e);
        }
    }
    
    /**
     * Send a callback notification to a client (best-effort, no reliability)
     */
    public void sendCallback(Message callback, InetSocketAddress clientAddr) {
        String clientAddrStr = clientAddr.getAddress().getHostAddress() + ":" + clientAddr.getPort();
        
        try {
            byte[] data = callback.encode();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, clientAddr.getAddress(), clientAddr.getPort());
            socket.send(packet);
            
            logger.logCallback(callback, clientAddrStr);
            
        } catch (IOException e) {
            logger.error("Error sending callback to " + clientAddrStr, e);
        }
    }
    
    /**
     * Stop the server
     */
    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return running.get();
    }
}
