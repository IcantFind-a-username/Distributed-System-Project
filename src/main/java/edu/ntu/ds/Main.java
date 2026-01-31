package edu.ntu.ds;

/**
 * Main entry point showing available commands
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║         Distributed Banking System - Protocol v1.1            ║");
        System.out.println("║                  Distributed Systems Course                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Available Components:");
        System.out.println();
        System.out.println("1. Protocol Tests (verify encoding/decoding):");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"edu.ntu.ds.protocol.ProtocolTest\"");
        System.out.println();
        System.out.println("2. Bank Server:");
        System.out.println("   mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.server.BankServer\" -Dexec.args=\"<port> [reqLoss%] [repLoss%]\"");
        System.out.println("   Example: mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.server.BankServer\" -Dexec.args=\"8888\"");
        System.out.println("   With loss: mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.server.BankServer\" -Dexec.args=\"8888 20 20\"");
        System.out.println();
        System.out.println("3. Interactive Client:");
        System.out.println("   mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.BankClient\" -Dexec.args=\"<host> <port> [clientId] [AMO|ALO]\"");
        System.out.println("   Example: mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.BankClient\" -Dexec.args=\"localhost 8888\"");
        System.out.println();
        System.out.println("4. Monitor Client (callback listener):");
        System.out.println("   mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.MonitorClient\" -Dexec.args=\"<host> <port> [clientId] [ttl] [duration]\"");
        System.out.println("   Example: mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.MonitorClient\" -Dexec.args=\"localhost 8888\"");
        System.out.println();
        System.out.println("5. ALO vs AMO Semantics Demo:");
        System.out.println("   mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.demo.SemanticsDemo\" -Dexec.args=\"<host> <port>\"");
        System.out.println("   (Start server with loss first: -Dexec.args=\"8888 0 30\")");
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Quick Start Demo:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Terminal 1 - Start Server:");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.server.BankServer\" -Dexec.args=\"8888\"");
        System.out.println();
        System.out.println("Terminal 2 - Start Monitor:");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.MonitorClient\" -Dexec.args=\"localhost 8888\"");
        System.out.println();
        System.out.println("Terminal 3 - Start Client:");
        System.out.println("  mvn compile exec:java -Dexec.mainClass=\"edu.ntu.ds.client.BankClient\" -Dexec.args=\"localhost 8888\"");
        System.out.println();
        System.out.println("Then in the client:");
        System.out.println("  > login alice secret");
        System.out.println("  > open SGD");
        System.out.println("  > deposit 1000");
        System.out.println("  > balance");
        System.out.println();
    }
}
