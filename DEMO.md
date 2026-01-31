# Distributed Banking System - Demo Script

This document provides step-by-step instructions for demonstrating the system's features.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Multiple terminal windows

## Demo 1: Basic Banking Operations

### Step 1: Start the Server

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
```

Expected output:
```
╔═══════════════════════════════════════════════════╗
║     Distributed Banking System - Server v1.1      ║
╠═══════════════════════════════════════════════════╣
║  Protocol: UDP                                    ║
║  Port: 8888                                       ║
║  Loss Simulation: DISABLED                        ║
╠═══════════════════════════════════════════════════╣
║  Press Ctrl+C to stop                             ║
╚═══════════════════════════════════════════════════╝
```

### Step 2: Start Interactive Client

```bash
# Terminal 2
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888"
```

### Step 3: Perform Banking Operations

```
> login alice secret123
Logged in as: alice

> open SGD
Account created successfully!
Account Number: ACC-1001

> deposit 1000
Deposit successful!
Deposited: $1000.00
New balance: $1000.00

> withdraw 250.50
Withdrawal successful!
Withdrew: $250.50
New balance: $749.50

> balance
Account: ACC-1001
Balance: $749.50 SGD

> quit
```

## Demo 2: Callback Notifications

### Step 1: Start Server

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
```

### Step 2: Start Monitor Client

```bash
# Terminal 2
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="localhost 8888 9999 300 300"
```

Expected output:
```
╔═══════════════════════════════════════════════════╗
║     Distributed Banking System - Monitor v1.1     ║
╠═══════════════════════════════════════════════════╣
║  Client ID: 9999                                  ║
║  Server: localhost:8888                           ║
║  TTL: 300 s                                       ║
║  Listen Duration: 300 s                           ║
╚═══════════════════════════════════════════════════╝

Registering for callbacks...
Successfully registered for callbacks!

Listening for ACCOUNT_UPDATE notifications...
Press Ctrl+C to stop.

═══════════════════════════════════════════════════
```

### Step 3: Start Interactive Client

```bash
# Terminal 3
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1001"
```

### Step 4: Perform Operations and Watch Callbacks

In Terminal 3:
```
> login alice secret
> open SGD
> deposit 500
```

In Terminal 2 (Monitor), you should see:
```
┌─────────────────────────────────────────────────┐
│ CALLBACK #1
├─────────────────────────────────────────────────┤
│ Account: ACC-1001
│ Balance: $0.00
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ CALLBACK #2
├─────────────────────────────────────────────────┤
│ Account: ACC-1001
│ Balance: $500.00
└─────────────────────────────────────────────────┘
```

## Demo 3: ALO vs AMO Semantics Under Packet Loss

This is the key demonstration showing why At-Most-Once semantics matter.

### Step 1: Start Server WITH Packet Loss

```bash
# Terminal 1 - 30% reply loss to trigger retries
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 30"
```

Note the loss simulation is enabled:
```
║  Loss Simulation: req=0%, rep=30%                 ║
```

### Step 2: Run Automated Demo

```bash
# Terminal 2
mvn exec:java -Dexec.mainClass="edu.ntu.ds.demo.SemanticsDemo" -Dexec.args="localhost 8888"
```

### Expected Results

#### Phase 1: AMO Semantics (Correct)
```
[Step 4] Checking final balances...
  ACC-1001 balance: $900.00
  ACC-1002 balance: $1100.00

[Verification]
  ✓ CORRECT: Balances match expected values
```

Even with retries (visible in logs), the AMO cache prevents double execution.

#### Phase 2: ALO Semantics (May Be Incorrect)
```
[Step 4] Checking final balances...
  ACC-1003 balance: $800.00  (Should be $900!)
  ACC-1004 balance: $1200.00 (Should be $1100!)

[Verification]
  ✗ INCORRECT: Balances do not match!
  This is expected with ALO semantics under packet loss!
  The transfer was likely executed multiple times due to retries.
```

### Server Log Analysis

Watch the server logs during the demo:

**AMO Mode:**
```
[INFO] REPLY to ... | status=OK | reqId=... | [FROM AMO CACHE]
```
The "[FROM AMO CACHE]" indicates duplicate requests were handled correctly.

**ALO Mode:**
```
[INFO] Processing TRANSFER: from=ACC-... to=ACC-... amount=$100.00
[INFO] Processing TRANSFER: from=ACC-... to=ACC-... amount=$100.00
```
Multiple TRANSFER log entries show the operation was executed multiple times.

## Demo 4: Manual ALO vs AMO Testing

### Step 1: Start Server with Loss

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 30"
```

### Step 2: Client A (Alice) - AMO Mode

```bash
# Terminal 2
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1001 AMO"
```

```
> login alice pass
> open SGD
Account Number: ACC-1001
> deposit 1000
New balance: $1000.00
```

### Step 3: Client B (Bob) - AMO Mode

```bash
# Terminal 3
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1002 AMO"
```

```
> login bob pass
> open SGD
Account Number: ACC-1002
> deposit 1000
New balance: $1000.00
```

### Step 4: Transfer with AMO

In Terminal 2 (Alice):
```
> transfer ACC-1002 500
Transfer successful!
Your new balance: $500.00

> balance
Balance: $500.00
```

In Terminal 3 (Bob):
```
> balance
Balance: $1500.00
```

Even if you see retries in the logs, balances are correct!

### Step 5: Switch to ALO and Repeat

In Terminal 2:
```
> semantics ALO
Semantics set to: ALO

> transfer ACC-1002 100
```

Now if retries occur, balances may become incorrect!

## Key Points to Highlight

1. **Protocol Correctness**: All messages use manual byte-level marshalling with Big-Endian encoding
2. **Retry Logic**: Client implements exponential backoff (500ms → 1s → 2s → 4s → 8s → 16s)
3. **AMO Cache**: Server caches replies keyed by (clientId, requestId) to suppress duplicates
4. **Callbacks**: Best-effort UDP notifications for monitoring
5. **Loss Simulation**: Configurable to demonstrate semantic differences

## Troubleshooting

### Client times out repeatedly
- Check server is running on correct port
- Check firewall settings

### No callbacks received
- Ensure monitor registered before operations
- Check monitor TTL hasn't expired

### Incorrect balances in ALO mode
- This is EXPECTED behavior - it demonstrates why AMO is important!
