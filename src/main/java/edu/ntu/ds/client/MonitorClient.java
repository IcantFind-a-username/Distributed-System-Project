package edu.ntu.ds.client;

import edu.ntu.ds.network.Logger;
import edu.ntu.ds.network.UdpClient;
import edu.ntu.ds.protocol.*;

import java.io.IOException;
import java.util.Random;

/**
 * Monitor Client - Listens for callback notifications only.
 * 
 * This client registers for callbacks and prints all ACCOUNT_UPDATE notifications
 * it receives. Useful for demonstrating the callback mechanism.
 * 
 * Usage: java MonitorClient <serverHost> <serverPort> [clientId] [ttlSeconds] [listenSeconds]
 */
public class MonitorClient {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String serverHost = args[0];
        int serverPort;
        int clientId;
        int ttlSeconds = 300;      // Default: 5 minutes TTL
        int listenSeconds = 300;   // Default: 5 minutes listening
        
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
            clientId = new Random().nextInt(90000) + 10000;
        }
        
        if (args.length >= 4) {
            try {
                ttlSeconds = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid TTL: " + args[3]);
                System.exit(1);
                return;
            }
        }
        
        if (args.length >= 5) {
            try {
                listenSeconds = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid listen duration: " + args[4]);
                System.exit(1);
                return;
            }
        }
        
        try {
            runMonitor(serverHost, serverPort, clientId, ttlSeconds, listenSeconds);
        } catch (Exception e) {
            System.err.println("Monitor error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runMonitor(String serverHost, int serverPort, 
            int clientId, int ttlSeconds, int listenSeconds) throws IOException {
        
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     Distributed Banking System - Monitor v1.1     ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.printf("║  Client ID: %-37d ║%n", clientId);
        System.out.printf("║  Server: %-40s ║%n", serverHost + ":" + serverPort);
        System.out.printf("║  TTL: %-39d s ║%n", ttlSeconds);
        System.out.printf("║  Listen Duration: %-27d s ║%n", listenSeconds);
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println();
        
        UdpClient client = new UdpClient(clientId, serverHost, serverPort);
        
        // Register for callbacks
        System.out.println("Registering for callbacks...");
        Message registerReq = client.createRegisterCallbackRequest(ttlSeconds);
        Message registerRep = client.sendRequest(registerReq);
        
        if (registerRep == null) {
            System.err.println("Failed to register - request timed out");
            client.close();
            return;
        }
        
        if (registerRep.getHeader().getStatus() != StatusCode.OK) {
            System.err.println("Failed to register: " + registerRep.getHeader().getStatus().getDescription());
            client.close();
            return;
        }
        
        System.out.println("Successfully registered for callbacks!");
        System.out.println("\nListening for ACCOUNT_UPDATE notifications...");
        System.out.println("Press Ctrl+C to stop.\n");
        System.out.println("═══════════════════════════════════════════════════");
        
        // Callback counter
        final int[] callbackCount = {0};
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nShutting down monitor...");
            System.out.println("Total callbacks received: " + callbackCount[0]);
            
            // Try to unregister
            try {
                Message unregReq = client.createUnregisterCallbackRequest();
                client.sendRequest(unregReq);
            } catch (IOException e) {
                // Ignore errors during shutdown
            }
            
            client.close();
        }));
        
        // Listen for callbacks
        client.listenForCallbacks(listenSeconds, callback -> {
            callbackCount[0]++;
            
            String accountNo = callback.getPayload().getAccountNo();
            Long balance = callback.getPayload().getAmountCents();
            
            System.out.println("\n┌─────────────────────────────────────────────────┐");
            System.out.println("│ CALLBACK #" + callbackCount[0]);
            System.out.println("├─────────────────────────────────────────────────┤");
            System.out.println("│ Account: " + (accountNo != null ? accountNo : "N/A"));
            System.out.println("│ Balance: " + (balance != null ? Logger.formatCents(balance) : "N/A"));
            System.out.println("└─────────────────────────────────────────────────┘");
        });
        
        System.out.println("\nListening period ended.");
        System.out.println("Total callbacks received: " + callbackCount[0]);
        client.close();
    }
    
    private static void printUsage() {
        System.out.println("Usage: java MonitorClient <serverHost> <serverPort> [clientId] [ttlSeconds] [listenSeconds]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  serverHost    - Server hostname or IP");
        System.out.println("  serverPort    - Server port number");
        System.out.println("  clientId      - Client identifier (default: random)");
        System.out.println("  ttlSeconds    - Callback registration TTL (default: 300)");
        System.out.println("  listenSeconds - How long to listen (default: 300)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java MonitorClient localhost 8888");
        System.out.println("  java MonitorClient localhost 8888 9999 600 300");
    }
}
