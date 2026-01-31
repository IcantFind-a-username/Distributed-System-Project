package edu.ntu.ds.client;

import edu.ntu.ds.network.Logger;
import edu.ntu.ds.network.UdpClient;
import edu.ntu.ds.protocol.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Distributed Banking System - Interactive CLI Client
 * 
 * Usage: java BankClient <serverHost> <serverPort> [clientId] [semantics]
 * 
 * Examples:
 *   java BankClient localhost 8888              # Auto-generated clientId, AMO semantics
 *   java BankClient localhost 8888 1001 ALO    # Explicit clientId, ALO semantics
 */
public class BankClient {
    
    private final UdpClient client;
    private final BufferedReader reader;
    
    // Session state
    private String currentUsername;
    private String currentPassword;
    private String currentAccountNo;
    
    public BankClient(UdpClient client) {
        this.client = client;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public void run() {
        printBanner();
        printHelp();
        
        while (true) {
            try {
                System.out.print("\n> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String args = parts.length > 1 ? parts[1] : "";
                
                switch (command) {
                    case "help":
                    case "?":
                        printHelp();
                        break;
                    case "login":
                        handleLogin(args);
                        break;
                    case "logout":
                        handleLogout();
                        break;
                    case "open":
                        handleOpenAccount(args);
                        break;
                    case "close":
                        handleCloseAccount();
                        break;
                    case "deposit":
                        handleDeposit(args);
                        break;
                    case "withdraw":
                        handleWithdraw(args);
                        break;
                    case "balance":
                        handleQueryBalance();
                        break;
                    case "transfer":
                        handleTransfer(args);
                        break;
                    case "use":
                        handleUseAccount(args);
                        break;
                    case "register":
                        handleRegisterCallback(args);
                        break;
                    case "unregister":
                        handleUnregisterCallback();
                        break;
                    case "semantics":
                        handleSetSemantics(args);
                        break;
                    case "status":
                        printStatus();
                        break;
                    case "quit":
                    case "exit":
                        System.out.println("Goodbye!");
                        client.close();
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type 'help' for available commands.");
                }
                
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     Distributed Banking System - Client v1.1      ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.printf("║  Client ID: %-37d ║%n", client.getClientId());
        System.out.println("╚═══════════════════════════════════════════════════╝");
    }
    
    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  login <username> <password>        - Set credentials for operations");
        System.out.println("  logout                             - Clear credentials");
        System.out.println("  open <currency> <initialBalance>   - Open new account (e.g., open SGD 1000)");
        System.out.println("  close                              - Close current account");
        System.out.println("  use <accountNo>                    - Set current account");
        System.out.println("  deposit <currency> <amount>        - Deposit (e.g., deposit SGD 100)");
        System.out.println("  withdraw <currency> <amount>       - Withdraw (e.g., withdraw SGD 50)");
        System.out.println("  balance                            - Query current account balance");
        System.out.println("  transfer <toAccount> <amount>      - Transfer from current account");
        System.out.println("  register <ttlSeconds>              - Register for callback notifications");
        System.out.println("  unregister                         - Unregister from callbacks");
        System.out.println("  semantics <ALO|AMO>                - Set invocation semantics");
        System.out.println("  status                             - Show current session status");
        System.out.println("  help                               - Show this help");
        System.out.println("  quit                               - Exit client");
        System.out.println("\nCurrencies: SGD, USD, EUR, GBP, JPY, CNY");
    }
    
    private void printStatus() {
        System.out.println("\n=== Session Status ===");
        System.out.println("Username: " + (currentUsername != null ? currentUsername : "(not logged in)"));
        System.out.println("Account:  " + (currentAccountNo != null ? currentAccountNo : "(not selected)"));
    }
    
    private void handleLogin(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        currentUsername = parts[0];
        currentPassword = parts[1];
        System.out.println("Logged in as: " + currentUsername);
    }
    
    private void handleLogout() {
        currentUsername = null;
        currentPassword = null;
        currentAccountNo = null;
        System.out.println("Logged out.");
    }
    
    private void handleUseAccount(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: use <accountNo>");
            return;
        }
        currentAccountNo = args.trim();
        System.out.println("Current account set to: " + currentAccountNo);
    }
    
    private void handleOpenAccount(String args) throws IOException {
        if (!checkLoggedIn()) return;
        
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: open <currency> <initialBalance>");
            System.out.println("Currencies: SGD, USD, EUR, GBP, JPY, CNY");
            System.out.println("Example: open SGD 1000");
            return;
        }
        
        Currency currency;
        try {
            currency = Currency.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid currency. Use: SGD, USD, EUR, GBP, JPY, CNY");
            return;
        }
        
        long initialBalance = parseAmount(parts[1]);
        if (initialBalance < 0) {
            System.out.println("Invalid initial balance. Must be non-negative.");
            return;
        }
        
        Message request = client.createOpenAccountRequest(currentUsername, currentPassword, currency, initialBalance);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            String accountNo = reply.getPayload().getAccountNo();
            Long balance = reply.getPayload().getAmountCents();
            System.out.println("Account created successfully!");
            System.out.println("Account Number: " + accountNo);
            if (balance != null) {
                System.out.println("Initial Balance: " + Logger.formatCents(balance));
            }
            currentAccountNo = accountNo;
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleCloseAccount() throws IOException {
        if (!checkLoggedIn()) return;
        if (!checkAccountSelected()) return;
        
        Message request = client.createCloseAccountRequest(currentUsername, currentPassword, currentAccountNo);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            Long finalBalance = reply.getPayload().getAmountCents();
            System.out.println("Account closed successfully!");
            if (finalBalance != null) {
                System.out.println("Final balance returned: " + Logger.formatCents(finalBalance));
            }
            currentAccountNo = null;
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleDeposit(String args) throws IOException {
        if (!checkLoggedIn()) return;
        if (!checkAccountSelected()) return;
        
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: deposit <currency> <amount>");
            System.out.println("Currencies: SGD, USD, EUR, GBP, JPY, CNY");
            System.out.println("Example: deposit SGD 100");
            return;
        }
        
        Currency currency;
        try {
            currency = Currency.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid currency. Use: SGD, USD, EUR, GBP, JPY, CNY");
            return;
        }
        
        long amountCents = parseAmount(parts[1]);
        if (amountCents <= 0) {
            System.out.println("Invalid amount. Must be positive.");
            return;
        }
        
        Message request = client.createDepositRequest(currentUsername, currentPassword, currentAccountNo, currency, amountCents);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            Long newBalance = reply.getPayload().getAmountCents();
            System.out.println("Deposit successful!");
            System.out.println("Deposited: " + Logger.formatCents(amountCents) + " " + currency.name());
            if (newBalance != null) {
                System.out.println("New balance: " + Logger.formatCents(newBalance));
            }
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleWithdraw(String args) throws IOException {
        if (!checkLoggedIn()) return;
        if (!checkAccountSelected()) return;
        
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: withdraw <currency> <amount>");
            System.out.println("Currencies: SGD, USD, EUR, GBP, JPY, CNY");
            System.out.println("Example: withdraw SGD 100");
            return;
        }
        
        Currency currency;
        try {
            currency = Currency.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid currency. Use: SGD, USD, EUR, GBP, JPY, CNY");
            return;
        }
        
        long amountCents = parseAmount(parts[1]);
        if (amountCents <= 0) {
            System.out.println("Invalid amount. Must be positive.");
            return;
        }
        
        Message request = client.createWithdrawRequest(currentUsername, currentPassword, currentAccountNo, currency, amountCents);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            Long newBalance = reply.getPayload().getAmountCents();
            System.out.println("Withdrawal successful!");
            System.out.println("Withdrew: " + Logger.formatCents(amountCents));
            if (newBalance != null) {
                System.out.println("New balance: " + Logger.formatCents(newBalance));
            }
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleQueryBalance() throws IOException {
        if (!checkLoggedIn()) return;
        if (!checkAccountSelected()) return;
        
        Message request = client.createQueryBalanceRequest(currentUsername, currentPassword, currentAccountNo);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            Long balance = reply.getPayload().getAmountCents();
            Currency currency = null;
            try {
                currency = reply.getPayload().getCurrency();
            } catch (Exception e) {
                // Currency not in reply
            }
            
            System.out.println("Account: " + currentAccountNo);
            if (balance != null) {
                System.out.println("Balance: " + Logger.formatCents(balance) + 
                    (currency != null ? " " + currency.name() : ""));
            }
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleTransfer(String args) throws IOException {
        if (!checkLoggedIn()) return;
        if (!checkAccountSelected()) return;
        
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: transfer <toAccountNo> <amount>");
            return;
        }
        
        String toAccount = parts[0];
        long amountCents = parseAmount(parts[1]);
        if (amountCents <= 0) {
            System.out.println("Invalid amount. Must be positive.");
            return;
        }
        
        System.out.println("Transferring " + Logger.formatCents(amountCents) + 
            " from " + currentAccountNo + " to " + toAccount + "...");
        
        Message request = client.createTransferRequest(
            currentUsername, currentPassword, currentAccountNo, toAccount, amountCents);
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            Long newBalance = reply.getPayload().getAmountCents();
            System.out.println("Transfer successful!");
            System.out.println("Transferred: " + Logger.formatCents(amountCents));
            if (newBalance != null) {
                System.out.println("Your new balance: " + Logger.formatCents(newBalance));
            }
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleRegisterCallback(String args) throws IOException {
        if (args.isEmpty()) {
            System.out.println("Usage: register <ttlSeconds>");
            return;
        }
        
        int ttl;
        try {
            ttl = Integer.parseInt(args.trim());
            if (ttl <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid TTL. Must be a positive integer.");
            return;
        }
        
        Message request = client.createRegisterCallbackRequest(ttl);
        
        // Start callback listener
        client.startCallbackListener(callback -> {
            System.out.println("\n*** CALLBACK RECEIVED ***");
            System.out.println("Account: " + callback.getPayload().getAccountNo());
            Long balance = callback.getPayload().getAmountCents();
            if (balance != null) {
                System.out.println("New Balance: " + Logger.formatCents(balance));
            }
            System.out.println("*************************");
            System.out.print("> "); // Restore prompt
        });
        
        Message reply = client.sendRequest(request);
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            System.out.println("Registered for callbacks (TTL: " + ttl + " seconds)");
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleUnregisterCallback() throws IOException {
        Message request = client.createUnregisterCallbackRequest();
        Message reply = client.sendRequest(request);
        
        client.stopCallbackListener();
        
        if (reply == null) {
            System.out.println("Request timed out after max retries.");
            return;
        }
        
        if (reply.getHeader().getStatus() == StatusCode.OK) {
            System.out.println("Unregistered from callbacks.");
        } else {
            System.out.println("Failed: " + reply.getHeader().getStatus().getDescription());
        }
    }
    
    private void handleSetSemantics(String args) {
        if (args.isEmpty()) {
            System.out.println("Usage: semantics <ALO|AMO>");
            System.out.println("  ALO = At-Least-Once (server executes every request)");
            System.out.println("  AMO = At-Most-Once (server caches replies, deduplicates)");
            return;
        }
        
        try {
            Semantics sem = Semantics.valueOf(args.trim().toUpperCase());
            client.setDefaultSemantics(sem);
            System.out.println("Semantics set to: " + sem);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid semantics. Use ALO or AMO.");
        }
    }
    
    private boolean checkLoggedIn() {
        if (currentUsername == null || currentPassword == null) {
            System.out.println("Please login first. Use: login <username> <password>");
            return false;
        }
        return true;
    }
    
    private boolean checkAccountSelected() {
        if (currentAccountNo == null) {
            System.out.println("No account selected. Use 'use <accountNo>' or 'open <currency>'");
            return false;
        }
        return true;
    }
    
    /**
     * Parse amount string to cents
     * Supports: "100" (dollars), "100.50" (dollars with cents), "10050c" (cents)
     */
    private long parseAmount(String str) {
        try {
            str = str.toLowerCase().trim();
            
            if (str.endsWith("c")) {
                // Amount in cents
                return Long.parseLong(str.substring(0, str.length() - 1));
            } else if (str.contains(".")) {
                // Amount with decimal
                double amount = Double.parseDouble(str);
                return Math.round(amount * 100);
            } else {
                // Whole dollars
                return Long.parseLong(str) * 100;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // Main entry point
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String serverHost = args[0];
        int serverPort;
        int clientId;
        Semantics semantics = Semantics.AMO;
        
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            System.exit(1);
            return;
        }
        
        // Generate random client ID or use provided one
        if (args.length >= 3) {
            try {
                clientId = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid client ID: " + args[2]);
                System.exit(1);
                return;
            }
        } else {
            clientId = new Random().nextInt(90000) + 10000; // 10000-99999
        }
        
        // Set semantics if provided
        if (args.length >= 4) {
            try {
                semantics = Semantics.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid semantics: " + args[3] + ". Use ALO or AMO.");
                System.exit(1);
                return;
            }
        }
        
        try {
            UdpClient client = new UdpClient(clientId, serverHost, serverPort);
            client.setDefaultSemantics(semantics);
            
            BankClient bankClient = new BankClient(client);
            bankClient.run();
            
        } catch (Exception e) {
            System.err.println("Failed to start client: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java BankClient <serverHost> <serverPort> [clientId] [semantics]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  serverHost - Server hostname or IP");
        System.out.println("  serverPort - Server port number");
        System.out.println("  clientId   - Client identifier (default: random)");
        System.out.println("  semantics  - ALO or AMO (default: AMO)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java BankClient localhost 8888");
        System.out.println("  java BankClient localhost 8888 1001 ALO");
    }
}
