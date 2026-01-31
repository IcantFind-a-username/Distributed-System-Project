package edu.ntu.ds.service;

import edu.ntu.ds.protocol.Currency;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory storage for bank accounts.
 * 
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class AccountStore {
    
    private final Map<String, Account> accountsByNo;
    private final Map<String, Account> accountsByUsername;
    private final AtomicInteger accountCounter;
    
    public AccountStore() {
        this.accountsByNo = new ConcurrentHashMap<>();
        this.accountsByUsername = new ConcurrentHashMap<>();
        this.accountCounter = new AtomicInteger(1000); // Start account numbers from 1001
    }
    
    /**
     * Create a new account
     * @param username unique username
     * @param password account password
     * @param currency account currency
     * @return the created account, or null if username already exists
     */
    public Account createAccount(String username, String password, Currency currency) {
        // Check if username already exists
        if (accountsByUsername.containsKey(username)) {
            return null;
        }
        
        // Generate unique account number
        String accountNo = "ACC-" + accountCounter.incrementAndGet();
        
        Account account = new Account(accountNo, username, password, currency);
        
        // Use putIfAbsent for thread safety
        Account existing = accountsByUsername.putIfAbsent(username, account);
        if (existing != null) {
            return null; // Race condition: another thread created account first
        }
        
        accountsByNo.put(accountNo, account);
        return account;
    }
    
    /**
     * Get account by account number
     */
    public Account getByAccountNo(String accountNo) {
        return accountsByNo.get(accountNo);
    }
    
    /**
     * Get account by username
     */
    public Account getByUsername(String username) {
        return accountsByUsername.get(username);
    }
    
    /**
     * Delete an account
     * @return the deleted account, or null if not found
     */
    public Account deleteAccount(String accountNo) {
        Account account = accountsByNo.remove(accountNo);
        if (account != null) {
            accountsByUsername.remove(account.getUsername());
        }
        return account;
    }
    
    /**
     * Check if account exists
     */
    public boolean exists(String accountNo) {
        return accountsByNo.containsKey(accountNo);
    }
    
    /**
     * Get all accounts
     */
    public Collection<Account> getAllAccounts() {
        return accountsByNo.values();
    }
    
    /**
     * Get total number of accounts
     */
    public int size() {
        return accountsByNo.size();
    }
    
    /**
     * Clear all accounts (for testing)
     */
    public void clear() {
        accountsByNo.clear();
        accountsByUsername.clear();
    }
}
