package edu.ntu.ds.network;

import java.util.Random;

/**
 * Simulates packet loss for demonstrating ALO vs AMO semantics.
 * 
 * Configurable loss probability allows testing behavior under unreliable network conditions.
 */
public class PacketLossSimulator {
    
    private final Random random;
    private double requestLossProbability;   // Probability of losing incoming requests (0.0 - 1.0)
    private double replyLossProbability;     // Probability of losing outgoing replies (0.0 - 1.0)
    private boolean enabled;
    
    // Statistics
    private long requestsReceived;
    private long requestsDropped;
    private long repliesSent;
    private long repliesDropped;
    
    public PacketLossSimulator() {
        this.random = new Random();
        this.requestLossProbability = 0.0;
        this.replyLossProbability = 0.0;
        this.enabled = false;
        resetStats();
    }
    
    /**
     * Enable loss simulation with specified probabilities
     * @param requestLoss probability of losing incoming requests (0.0 - 1.0)
     * @param replyLoss probability of losing outgoing replies (0.0 - 1.0)
     */
    public void enable(double requestLoss, double replyLoss) {
        if (requestLoss < 0.0 || requestLoss > 1.0 || replyLoss < 0.0 || replyLoss > 1.0) {
            throw new IllegalArgumentException("Loss probability must be between 0.0 and 1.0");
        }
        this.requestLossProbability = requestLoss;
        this.replyLossProbability = replyLoss;
        this.enabled = true;
    }
    
    /**
     * Disable loss simulation
     */
    public void disable() {
        this.enabled = false;
    }
    
    /**
     * Check if an incoming request should be dropped (simulated loss)
     * @return true if the request should be dropped
     */
    public boolean shouldDropRequest() {
        requestsReceived++;
        if (enabled && random.nextDouble() < requestLossProbability) {
            requestsDropped++;
            return true;
        }
        return false;
    }
    
    /**
     * Check if an outgoing reply should be dropped (simulated loss)
     * @return true if the reply should be dropped
     */
    public boolean shouldDropReply() {
        repliesSent++;
        if (enabled && random.nextDouble() < replyLossProbability) {
            repliesDropped++;
            return true;
        }
        return false;
    }
    
    /**
     * Reset statistics counters
     */
    public void resetStats() {
        this.requestsReceived = 0;
        this.requestsDropped = 0;
        this.repliesSent = 0;
        this.repliesDropped = 0;
    }
    
    // Getters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public double getRequestLossProbability() {
        return requestLossProbability;
    }
    
    public double getReplyLossProbability() {
        return replyLossProbability;
    }
    
    public long getRequestsReceived() {
        return requestsReceived;
    }
    
    public long getRequestsDropped() {
        return requestsDropped;
    }
    
    public long getRepliesSent() {
        return repliesSent;
    }
    
    public long getRepliesDropped() {
        return repliesDropped;
    }
    
    @Override
    public String toString() {
        return String.format("PacketLossSimulator{enabled=%s, reqLoss=%.1f%%, repLoss=%.1f%%, " +
            "stats=[reqRecv=%d, reqDrop=%d, repSent=%d, repDrop=%d]}",
            enabled, requestLossProbability * 100, replyLossProbability * 100,
            requestsReceived, requestsDropped, repliesSent, repliesDropped);
    }
}
