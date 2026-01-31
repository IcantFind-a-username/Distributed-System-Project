package edu.ntu.ds.service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for callback subscriptions.
 * 
 * Clients can register to receive ACCOUNT_UPDATE notifications for a specified TTL.
 * When state-changing operations occur, registered clients are notified.
 */
public class CallbackRegistry {
    
    /**
     * Callback registration entry
     */
    private static class Registration {
        final int clientId;
        final InetSocketAddress address;
        final long expiresAt;
        
        Registration(int clientId, InetSocketAddress address, int ttlSeconds) {
            this.clientId = clientId;
            this.address = address;
            this.expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
    
    private final Map<Integer, Registration> registrations;  // clientId -> registration
    
    public CallbackRegistry() {
        this.registrations = new ConcurrentHashMap<>();
    }
    
    /**
     * Register a client for callbacks
     * @param clientId client identifier
     * @param address client's address for sending callbacks
     * @param ttlSeconds time-to-live in seconds
     */
    public void register(int clientId, InetSocketAddress address, int ttlSeconds) {
        Registration reg = new Registration(clientId, address, ttlSeconds);
        registrations.put(clientId, reg);
    }
    
    /**
     * Unregister a client
     * @param clientId client identifier
     * @return true if client was registered
     */
    public boolean unregister(int clientId) {
        return registrations.remove(clientId) != null;
    }
    
    /**
     * Check if a client is registered
     */
    public boolean isRegistered(int clientId) {
        Registration reg = registrations.get(clientId);
        if (reg != null && !reg.isExpired()) {
            return true;
        }
        if (reg != null && reg.isExpired()) {
            registrations.remove(clientId);
        }
        return false;
    }
    
    /**
     * Get addresses of all registered (non-expired) clients except the specified one.
     * This is used to send notifications to other clients about account updates.
     * 
     * @param excludeClientId client to exclude (typically the one who made the change)
     * @return set of addresses to notify
     */
    public Set<InetSocketAddress> getRegisteredAddresses(int excludeClientId) {
        cleanupExpired();
        return registrations.values().stream()
            .filter(r -> r.clientId != excludeClientId && !r.isExpired())
            .map(r -> r.address)
            .collect(Collectors.toSet());
    }
    
    /**
     * Get all registered addresses (including the specified client)
     */
    public Set<InetSocketAddress> getAllRegisteredAddresses() {
        cleanupExpired();
        return registrations.values().stream()
            .filter(r -> !r.isExpired())
            .map(r -> r.address)
            .collect(Collectors.toSet());
    }
    
    /**
     * Get client address by ID
     */
    public InetSocketAddress getAddress(int clientId) {
        Registration reg = registrations.get(clientId);
        if (reg != null && !reg.isExpired()) {
            return reg.address;
        }
        return null;
    }
    
    /**
     * Remove expired registrations
     */
    public void cleanupExpired() {
        registrations.entrySet().removeIf(e -> e.getValue().isExpired());
    }
    
    /**
     * Get number of active registrations
     */
    public int size() {
        cleanupExpired();
        return registrations.size();
    }
    
    /**
     * Clear all registrations
     */
    public void clear() {
        registrations.clear();
    }
    
    @Override
    public String toString() {
        cleanupExpired();
        return "CallbackRegistry{registered=" + registrations.size() + " clients}";
    }
}
