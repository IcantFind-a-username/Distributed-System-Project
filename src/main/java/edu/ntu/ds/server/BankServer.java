package edu.ntu.ds.server;

import edu.ntu.ds.network.UdpServer;
import edu.ntu.ds.service.AccountStore;
import edu.ntu.ds.service.BankingService;
import edu.ntu.ds.service.RequestProcessor;

import java.net.SocketException;

/**
 * Distributed Banking System - UDP Server
 * 
 * Usage: java BankServer [port] [requestLoss%] [replyLoss%]
 * 
 * Examples:
 *   java BankServer 8888                  # Start on port 8888, no loss simulation
 *   java BankServer 8888 20 20            # Start with 20% request/reply loss
 */
public class BankServer {
    
    public static void main(String[] args) {
        // Parse command line arguments
        int port = 8888;
        double requestLoss = 0.0;
        double replyLoss = 0.0;
        
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                printUsage();
                System.exit(1);
            }
        }
        
        if (args.length >= 3) {
            try {
                requestLoss = Double.parseDouble(args[1]) / 100.0;
                replyLoss = Double.parseDouble(args[2]) / 100.0;
                
                if (requestLoss < 0 || requestLoss > 1 || replyLoss < 0 || replyLoss > 1) {
                    throw new NumberFormatException("Loss must be 0-100");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid loss percentage: " + e.getMessage());
                printUsage();
                System.exit(1);
            }
        }
        
        // Initialize components
        AccountStore accountStore = new AccountStore();
        BankingService bankingService = new BankingService(accountStore);
        RequestProcessor processor = new RequestProcessor(bankingService);
        
        // Create and configure server
        UdpServer server = new UdpServer(port, processor);
        processor.setServer(server);
        
        // Enable loss simulation if specified
        if (requestLoss > 0 || replyLoss > 0) {
            server.enableLossSimulation(requestLoss, replyLoss);
        }
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
            System.out.println("AMO Cache stats: " + processor.getAmoCache());
            System.out.println("Callback Registry: " + processor.getCallbackRegistry());
            System.out.println("Loss Simulator: " + server.getLossSimulator());
        }));
        
        // Print startup banner
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     Distributed Banking System - Server v1.1      ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║  Protocol: UDP                                    ║");
        System.out.printf("║  Port: %-42d ║%n", port);
        System.out.printf("║  Loss Simulation: %-30s ║%n", 
            (requestLoss > 0 || replyLoss > 0) ? 
            String.format("req=%.0f%%, rep=%.0f%%", requestLoss * 100, replyLoss * 100) : "DISABLED");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║  Press Ctrl+C to stop                             ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println();
        
        // Start server
        try {
            server.start();
        } catch (SocketException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java BankServer [port] [requestLoss%] [replyLoss%]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  port         - Server port (default: 8888)");
        System.out.println("  requestLoss  - Request drop percentage 0-100 (default: 0)");
        System.out.println("  replyLoss    - Reply drop percentage 0-100 (default: 0)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java BankServer 8888");
        System.out.println("  java BankServer 8888 20 20");
    }
}
