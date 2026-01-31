package edu.ntu.ds.service;

import edu.ntu.ds.protocol.Currency;

/**
 * Account entity representing a bank account.
 * 
 * All monetary values are stored as signed 64-bit integers representing the smallest
 * currency unit (e.g., cents for dollars). This avoids floating-point precision issues.
 */
public class Account {
    
    private final String accountNo;
    private final String username;
    private final String passwordHash;  // In production, this would be hashed
    private final Currency currency;
    private long balanceCents;
    private final long createdAt;
    
    public Account(String accountNo, String username, String password, Currency currency, long initialBalance) {
        this.accountNo = accountNo;
        this.username = username;
        this.passwordHash = password;  // In production: hash the password
        this.currency = currency;
        this.balanceCents = initialBalance;  // Support initial balance as per project requirement
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Constructor without initial balance (defaults to 0)
     */
    public Account(String accountNo, String username, String password, Currency currency) {
        this(accountNo, username, password, currency, 0);
    }
    
    /**
     * Verify password matches
     */
    public boolean verifyPassword(String password) {
        return this.passwordHash.equals(password);
    }
    
    /**
     * Deposit funds (add to balance)
     * @param amountCents amount in cents (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     */
    public synchronized void deposit(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balanceCents += amountCents;
    }
    
    /**
     * Withdraw funds (subtract from balance)
     * @param amountCents amount in cents (must be positive)
     * @return true if withdrawal successful, false if insufficient funds
     * @throws IllegalArgumentException if amount is not positive
     */
    public synchronized boolean withdraw(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (this.balanceCents < amountCents) {
            return false; // Insufficient funds
        }
        this.balanceCents -= amountCents;
        return true;
    }
    
    // Getters
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public synchronized long getBalanceCents() {
        return balanceCents;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return String.format("Account{no=%s, user=%s, currency=%s, balance=%d cents}",
            accountNo, username, currency, balanceCents);
    }
}
