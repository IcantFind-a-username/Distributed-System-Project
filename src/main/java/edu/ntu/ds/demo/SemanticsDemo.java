package edu.ntu.ds.demo;

import edu.ntu.ds.network.Logger;
import edu.ntu.ds.network.UdpClient;
import edu.ntu.ds.protocol.*;

import java.io.IOException;
import java.util.Random;

/**
 * Demonstration of ALO vs AMO Invocation Semantics
 * 
 * This demo shows the difference between At-Least-Once and At-Most-Once
 * semantics when packet loss occurs.
 * 
 * Usage: java SemanticsDemo <serverHost> <serverPort>
 * 
 * Prerequisites:
 * - Server must be running with reply loss enabled (e.g., 30%)
 *   mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 30"
 */
public class SemanticsDemo {
    
    private static final String USERNAME = "demo_user";
    private static final String PASSWORD = "demo_pass";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SemanticsDemo <serverHost> <serverPort>");
            System.exit(1);
        }
        
        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║     ALO vs AMO Invocation Semantics Demonstration             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("This demo shows how AMO semantics prevent double execution");
        System.out.println("of non-idempotent operations when packet loss causes retries.");
        System.out.println();
        System.out.println("Make sure the server is running with reply loss enabled!");
        System.out.println("Example: mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.server.BankServer\" -Dexec.args=\"8888 0 30\"");
        System.out.println();
        System.out.println("NOTE: Packet loss is PROBABILISTIC. You may need to run multiple times");
        System.out.println("to see retries. Higher loss = more likely to demonstrate the effect.");
        System.out.println("Try 50% loss for more obvious results: -Dexec.args=\"8888 0 50\"");
        System.out.println();
        
        try {
            // Run demo with AMO semantics
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("PHASE 1: Testing with AMO (At-Most-Once) Semantics");
            System.out.println("═══════════════════════════════════════════════════════════════");
            runTransferDemo(serverHost, serverPort, Semantics.AMO, "AMO");
            
            Thread.sleep(2000);
            
            // Run demo with ALO semantics
            System.out.println("\n═══════════════════════════════════════════════════════════════");
            System.out.println("PHASE 2: Testing with ALO (At-Least-Once) Semantics");
            System.out.println("═══════════════════════════════════════════════════════════════");
            runTransferDemo(serverHost, serverPort, Semantics.ALO, "ALO");
            
            System.out.println("\n═══════════════════════════════════════════════════════════════");
            System.out.println("DEMO COMPLETE");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Key Observations:");
            System.out.println("- AMO: Even if retries occurred, balance is correct (transfer executed once)");
            System.out.println("- ALO: If retries occurred, balance may be wrong (transfer executed multiple times)");
            System.out.println();
            System.out.println("Check the server logs to see:");
            System.out.println("- '[FROM AMO CACHE]' entries showing duplicate suppression in AMO mode");
            System.out.println("- Multiple TRANSFER executions in ALO mode");
            System.out.println();
            System.out.println("NOTE: Packet loss is probabilistic (30%). If you didn't see retries,");
            System.out.println("run the demo again. The more loss, the more likely to see the effect.");
            System.out.println("You can increase loss rate: -Dexec.args=\"8888 0 50\" for 50% reply loss");
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runTransferDemo(String host, int port, Semantics semantics, String label) 
            throws IOException, ProtocolException, InterruptedException {
        
        Random random = new Random();
        int clientId1 = 10000 + random.nextInt(1000);
        int clientId2 = 20000 + random.nextInt(1000);
        
        UdpClient client1 = new UdpClient(clientId1, host, port);
        UdpClient client2 = new UdpClient(clientId2, host, port);
        
        client1.setDefaultSemantics(semantics);
        client2.setDefaultSemantics(semantics);
        client1.getLogger().setVerbose(false);
        client2.getLogger().setVerbose(false);
        
        String username1 = "alice_" + label.toLowerCase() + "_" + System.currentTimeMillis();
        String username2 = "bob_" + label.toLowerCase() + "_" + System.currentTimeMillis();
        
        try {
            // Step 1: Create accounts with $1000 initial balance
            System.out.println("\n[Step 1] Creating two accounts with $1000 each...");
            
            Message openReq1 = client1.createOpenAccountRequest(username1, PASSWORD, Currency.SGD, 100000L);
            Message openRep1 = client1.sendRequest(openReq1);
            if (openRep1 == null) {
                throw new RuntimeException("Failed to create account 1 - timeout");
            }
            if (openRep1.getHeader().getStatus() == StatusCode.ALREADY_EXISTS && semantics == Semantics.ALO) {
                System.out.println("\n  *** ALO PROBLEM DETECTED! ***");
                System.out.println("  Account creation returned ALREADY_EXISTS!");
                System.out.println("  This means:");
                System.out.println("    1. First request executed successfully (account created)");
                System.out.println("    2. Reply was lost (simulated packet loss)");
                System.out.println("    3. Client retried with same request");
                System.out.println("    4. Server executed AGAIN (ALO mode) -> account already exists!");
                System.out.println("  This demonstrates why ALO breaks non-idempotent operations.\n");
                return; // Demo complete - we've shown the problem
            }
            if (openRep1.getHeader().getStatus() != StatusCode.OK) {
                throw new RuntimeException("Failed to create account 1: " + openRep1.getHeader().getStatus());
            }
            String account1 = openRep1.getPayload().getAccountNo();
            Long balance1Init = openRep1.getPayload().getAmountCents();
            System.out.println("  Account 1 created: " + account1 + 
                (balance1Init != null ? " (Balance: " + Logger.formatCents(balance1Init) + ")" : ""));
            
            Message openReq2 = client2.createOpenAccountRequest(username2, PASSWORD, Currency.SGD, 100000L);
            Message openRep2 = client2.sendRequest(openReq2);
            if (openRep2 == null) {
                throw new RuntimeException("Failed to create account 2 - timeout");
            }
            if (openRep2.getHeader().getStatus() == StatusCode.ALREADY_EXISTS && semantics == Semantics.ALO) {
                System.out.println("\n  *** ALO PROBLEM DETECTED during account 2 creation! ***");
                System.out.println("  Same issue as above - ALO caused duplicate execution.\n");
                return;
            }
            if (openRep2.getHeader().getStatus() != StatusCode.OK) {
                throw new RuntimeException("Failed to create account 2: " + openRep2.getHeader().getStatus());
            }
            String account2 = openRep2.getPayload().getAccountNo();
            Long balance2Init = openRep2.getPayload().getAmountCents();
            System.out.println("  Account 2 created: " + account2 + 
                (balance2Init != null ? " (Balance: " + Logger.formatCents(balance2Init) + ")" : ""));
            
            // Step 2: Perform transfer (this is where semantics matter!)
            System.out.println("\n[Step 2] Transferring $100 from Account 1 to Account 2...");
            System.out.println("  Semantics: " + semantics);
            System.out.println("  (Watch for retries in the client log due to simulated reply loss)");
            
            client1.getLogger().setVerbose(true);
            Message transferReq = client1.createTransferRequest(username1, PASSWORD, account1, account2, 10000);
            Message transferRep = client1.sendRequest(transferReq);
            client1.getLogger().setVerbose(false);
            
            if (transferRep == null) {
                System.out.println("  Transfer request timed out!");
            } else if (transferRep.getHeader().getStatus() == StatusCode.OK) {
                System.out.println("  Transfer completed!");
            } else {
                System.out.println("  Transfer failed: " + transferRep.getHeader().getStatus());
            }
            
            // Step 3: Check final balances
            System.out.println("\n[Step 3] Checking final balances...");
            
            Message balReq1 = client1.createQueryBalanceRequest(username1, PASSWORD, account1);
            Message balRep1 = client1.sendRequest(balReq1);
            long balance1 = balRep1 != null && balRep1.getHeader().getStatus() == StatusCode.OK 
                ? balRep1.getPayload().getAmountCents() : -1;
            
            Message balReq2 = client2.createQueryBalanceRequest(username2, PASSWORD, account2);
            Message balRep2 = client2.sendRequest(balReq2);
            long balance2 = balRep2 != null && balRep2.getHeader().getStatus() == StatusCode.OK 
                ? balRep2.getPayload().getAmountCents() : -1;
            
            System.out.println("  " + account1 + " balance: " + Logger.formatCents(balance1));
            System.out.println("  " + account2 + " balance: " + Logger.formatCents(balance2));
            
            // Verify results
            System.out.println("\n[Verification]");
            long expectedBalance1 = 90000; // $1000 - $100 = $900
            long expectedBalance2 = 110000; // $1000 + $100 = $1100
            
            if (balance1 == expectedBalance1 && balance2 == expectedBalance2) {
                System.out.println("  ✓ CORRECT: Balances match expected values");
                System.out.println("    Account 1: Expected " + Logger.formatCents(expectedBalance1) + 
                    ", Got " + Logger.formatCents(balance1));
                System.out.println("    Account 2: Expected " + Logger.formatCents(expectedBalance2) + 
                    ", Got " + Logger.formatCents(balance2));
            } else {
                System.out.println("  ✗ INCORRECT: Balances do not match!");
                System.out.println("    Account 1: Expected " + Logger.formatCents(expectedBalance1) + 
                    ", Got " + Logger.formatCents(balance1));
                System.out.println("    Account 2: Expected " + Logger.formatCents(expectedBalance2) + 
                    ", Got " + Logger.formatCents(balance2));
                
                if (semantics == Semantics.ALO) {
                    System.out.println("  This is expected with ALO semantics under packet loss!");
                    System.out.println("  The transfer was likely executed multiple times due to retries.");
                }
            }
            
        } finally {
            client1.close();
            client2.close();
        }
    }
}
