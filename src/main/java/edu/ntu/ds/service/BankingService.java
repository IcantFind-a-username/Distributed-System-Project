package edu.ntu.ds.service;

import edu.ntu.ds.protocol.*;

/**
 * Banking Service implementing core business logic.
 * 
 * This class handles the actual banking operations and returns appropriate
 * status codes and data. It is separated from networking and protocol concerns.
 */
public class BankingService {
    
    private final AccountStore accountStore;
    
    /**
     * Result of a banking operation
     */
    public static class OperationResult {
        public final StatusCode status;
        public final Account account;        // For operations that return account info
        public final Long balanceCents;      // For operations that return balance
        public final String accountNo;       // For open account
        
        private OperationResult(StatusCode status, Account account, Long balanceCents, String accountNo) {
            this.status = status;
            this.account = account;
            this.balanceCents = balanceCents;
            this.accountNo = accountNo;
        }
        
        public static OperationResult success() {
            return new OperationResult(StatusCode.OK, null, null, null);
        }
        
        public static OperationResult success(Account account) {
            return new OperationResult(StatusCode.OK, account, account.getBalanceCents(), account.getAccountNo());
        }
        
        public static OperationResult successWithBalance(long balanceCents) {
            return new OperationResult(StatusCode.OK, null, balanceCents, null);
        }
        
        public static OperationResult successWithAccountNo(String accountNo) {
            return new OperationResult(StatusCode.OK, null, null, accountNo);
        }
        
        public static OperationResult error(StatusCode status) {
            return new OperationResult(status, null, null, null);
        }
    }
    
    public BankingService(AccountStore accountStore) {
        this.accountStore = accountStore;
    }
    
    /**
     * Open a new account with initial balance
     */
    public OperationResult openAccount(String username, String password, Currency currency, long initialBalance) {
        if (username == null || password == null || currency == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        if (initialBalance < 0) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account account = accountStore.createAccount(username, password, currency, initialBalance);
        if (account == null) {
            return OperationResult.error(StatusCode.ALREADY_EXISTS);
        }
        
        return OperationResult.success(account);
    }
    
    /**
     * Open account without initial balance (for backward compatibility)
     */
    public OperationResult openAccount(String username, String password, Currency currency) {
        return openAccount(username, password, currency, 0);
    }
    
    /**
     * Close an existing account
     * Required TLVs: username, password, accountNo
     */
    public OperationResult closeAccount(String username, String password, String accountNo) {
        if (username == null || password == null || accountNo == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account account = accountStore.getByAccountNo(accountNo);
        if (account == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        // Verify ownership and password
        if (!account.getUsername().equals(username)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        if (!account.verifyPassword(password)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        
        // Get final balance before closing
        long finalBalance = account.getBalanceCents();
        
        accountStore.deleteAccount(accountNo);
        return OperationResult.successWithBalance(finalBalance);
    }
    
    /**
     * Deposit funds into an account
     */
    public OperationResult deposit(String username, String password, String accountNo, 
            Currency currency, long amountCents) {
        if (username == null || password == null || accountNo == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        if (amountCents <= 0) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account account = accountStore.getByAccountNo(accountNo);
        if (account == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        // Verify ownership and password
        if (!account.getUsername().equals(username)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        if (!account.verifyPassword(password)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        
        // Verify currency type matches
        if (currency != null && account.getCurrency() != currency) {
            return OperationResult.error(StatusCode.CURRENCY_MISMATCH);
        }
        
        account.deposit(amountCents);
        return OperationResult.success(account);
    }
    
    /**
     * Deposit without explicit currency (for backward compatibility)
     */
    public OperationResult deposit(String username, String password, String accountNo, long amountCents) {
        return deposit(username, password, accountNo, null, amountCents);
    }
    
    /**
     * Withdraw funds from an account
     */
    public OperationResult withdraw(String username, String password, String accountNo, 
            Currency currency, long amountCents) {
        if (username == null || password == null || accountNo == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        if (amountCents <= 0) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account account = accountStore.getByAccountNo(accountNo);
        if (account == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        // Verify ownership and password
        if (!account.getUsername().equals(username)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        if (!account.verifyPassword(password)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        
        // Verify currency type matches
        if (currency != null && account.getCurrency() != currency) {
            return OperationResult.error(StatusCode.CURRENCY_MISMATCH);
        }
        
        if (!account.withdraw(amountCents)) {
            return OperationResult.error(StatusCode.INSUFFICIENT_FUNDS);
        }
        
        return OperationResult.success(account);
    }
    
    /**
     * Withdraw without explicit currency (for backward compatibility)
     */
    public OperationResult withdraw(String username, String password, String accountNo, long amountCents) {
        return withdraw(username, password, accountNo, null, amountCents);
    }
    
    /**
     * Query account balance
     * Required TLVs: username, password, accountNo
     */
    public OperationResult queryBalance(String username, String password, String accountNo) {
        if (username == null || password == null || accountNo == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account account = accountStore.getByAccountNo(accountNo);
        if (account == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        // Verify ownership and password
        if (!account.getUsername().equals(username)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        if (!account.verifyPassword(password)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        
        return OperationResult.success(account);
    }
    
    /**
     * Transfer funds between accounts
     * Required TLVs: username, password, accountNo (from), toAccountNo, amountCents
     */
    public OperationResult transfer(String username, String password, 
            String fromAccountNo, String toAccountNo, long amountCents) {
        if (username == null || password == null || fromAccountNo == null || 
            toAccountNo == null) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        if (amountCents <= 0) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        if (fromAccountNo.equals(toAccountNo)) {
            return OperationResult.error(StatusCode.BAD_REQUEST);
        }
        
        Account fromAccount = accountStore.getByAccountNo(fromAccountNo);
        if (fromAccount == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        Account toAccount = accountStore.getByAccountNo(toAccountNo);
        if (toAccount == null) {
            return OperationResult.error(StatusCode.NOT_FOUND);
        }
        
        // Verify ownership and password of source account
        if (!fromAccount.getUsername().equals(username)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        if (!fromAccount.verifyPassword(password)) {
            return OperationResult.error(StatusCode.AUTH_FAIL);
        }
        
        // Check currency match
        if (fromAccount.getCurrency() != toAccount.getCurrency()) {
            return OperationResult.error(StatusCode.CURRENCY_MISMATCH);
        }
        
        // Perform transfer (withdraw + deposit atomically)
        synchronized (fromAccount) {
            synchronized (toAccount) {
                if (!fromAccount.withdraw(amountCents)) {
                    return OperationResult.error(StatusCode.INSUFFICIENT_FUNDS);
                }
                toAccount.deposit(amountCents);
            }
        }
        
        return OperationResult.success(fromAccount);
    }
    
    /**
     * Get account store (for callback notifications)
     */
    public AccountStore getAccountStore() {
        return accountStore;
    }
}
