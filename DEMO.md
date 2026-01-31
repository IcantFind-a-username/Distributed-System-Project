# Distributed Banking System - Demo Script

This document provides step-by-step instructions for demonstrating the system's features.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Multiple terminal windows
- **Important**: First compile the project once: `mvn compile`

## Demo 1: Basic Banking Operations

### Step 1: Start the Server

```bash
# Terminal 1
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
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
# Terminal 2 (compile already done, can use exec:java directly)
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
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
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

This is the **key demonstration** showing why At-Most-Once semantics matter.

### Important Notes

1. **Packet loss is PROBABILISTIC** - You may need to run multiple times to see retries
2. **Higher loss rate = more obvious results** - Use 50% for clearer demonstration
3. **Watch the server logs** to see `[FROM AMO CACHE]` entries

### Step 1: Start Server WITH Packet Loss

```bash
# Terminal 1 - 50% reply loss for more obvious demonstration
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 50"
```

Note the loss simulation is enabled:
```
║  Loss Simulation: req=0%, rep=50%                 ║
```

### Step 2: Run Automated Demo

```bash
# Terminal 2
mvn exec:java -Dexec.mainClass="edu.ntu.ds.demo.SemanticsDemo" -Dexec.args="localhost 8888"
```

### Expected Results

#### Phase 1: AMO Semantics (Correct)

When retries occur (you'll see `attempt=1`, `attempt=2`, etc.), AMO cache ensures correctness:

```
[Step 3] Transferring $100 from Account 1 to Account 2...
  Semantics: AMO
[19:11:26.774] [CLIENT-10795] [INFO] SEND to 127.0.0.1:8888 | op=TRANSFER | reqId=... | attempt=1
[19:11:27.276] [CLIENT-10795] [WARN] Timeout (attempt 1/6, timeout=500ms)
[19:11:27.277] [CLIENT-10795] [INFO] SEND to 127.0.0.1:8888 | op=TRANSFER | reqId=... | attempt=2
[19:11:27.278] [CLIENT-10795] [INFO] RECV from 127.0.0.1:8888 | status=OK | reqId=...
  Transfer completed!

[Verification]
  ✓ CORRECT: Balances match expected values
    Account 1: Expected $900.00, Got $900.00
    Account 2: Expected $1100.00, Got $1100.00
```

**Server log shows AMO cache in action:**
```
[INFO] REPLY to ... | status=OK | reqId=... | [FROM AMO CACHE]
```

#### Phase 2: ALO Semantics (Shows the Problem!)

ALO may fail in two ways:

**Case A: Balance becomes incorrect (transfer executed multiple times)**
```
[Verification]
  ✗ INCORRECT: Balances do not match!
    Account 1: Expected $900.00, Got $800.00  <-- $100 transferred twice!
    Account 2: Expected $1100.00, Got $1200.00
  This is expected with ALO semantics under packet loss!
```

**Case B: Operation fails with ALREADY_EXISTS (more common)**
```
[Step 1] Creating two accounts...
[INFO] SEND to 127.0.0.1:8888 | op=OPEN_ACCOUNT | reqId=... | attempt=1
[WARN] Timeout (attempt 1/6, timeout=500ms)
[INFO] SEND to 127.0.0.1:8888 | op=OPEN_ACCOUNT | reqId=... | attempt=2
[WARN] Timeout (attempt 2/6, timeout=1000ms)
[INFO] SEND to 127.0.0.1:8888 | op=OPEN_ACCOUNT | reqId=... | attempt=3
[INFO] RECV from 127.0.0.1:8888 | status=ALREADY_EXISTS | reqId=...

  *** ALO PROBLEM DETECTED! ***
  Account creation returned ALREADY_EXISTS!
  This means:
    1. First request executed successfully (account created)
    2. Reply was lost (simulated packet loss)
    3. Client retried with same request
    4. Server executed AGAIN (ALO mode) -> account already exists!
  This demonstrates why ALO breaks non-idempotent operations.
```

### Server Log Analysis

**Watch the server logs carefully during the demo:**

**AMO Mode - Duplicate suppression:**
```
[INFO] REQUEST from ... | op=TRANSFER | clientId=10795 | reqId=46364171960323
[INFO] Processing TRANSFER: from=ACC-1001 to=ACC-1002 amount=$100.00
[INFO] REPLY to ... | status=OK | reqId=46364171960323
[WARN] [SIMULATED LOSS] REPLY dropped | reqId=46364171960323

[INFO] REQUEST from ... | op=TRANSFER | clientId=10795 | reqId=46364171960323  <-- Same reqId!
[INFO] AMO cache hit for clientId=10795, requestId=46364171960323
[INFO] REPLY to ... | status=OK | reqId=46364171960323 | [FROM AMO CACHE]  <-- Cached reply!
```

**ALO Mode - Double execution:**
```
[INFO] REQUEST from ... | op=TRANSFER | clientId=10245 | reqId=44001939947523
[INFO] Processing TRANSFER: from=ACC-1003 to=ACC-1004 amount=$100.00  <-- First execution
[INFO] REPLY to ... | status=OK | reqId=44001939947523
[WARN] [SIMULATED LOSS] REPLY dropped | reqId=44001939947523

[INFO] REQUEST from ... | op=TRANSFER | clientId=10245 | reqId=44001939947523  <-- Same reqId!
[INFO] Processing TRANSFER: from=ACC-1003 to=ACC-1004 amount=$100.00  <-- Second execution!
[INFO] REPLY to ... | status=OK | reqId=44001939947523
```

## Demo 4: Manual ALO vs AMO Testing

### Step 1: Start Server with Loss

```bash
# Terminal 1
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 50"
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

In Terminal 2 (Alice):
```
> semantics ALO
Semantics set to: ALO

> deposit 100
```

If retries occur (check logs), balance may increase by more than $100!

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

### No retries visible in demo
- Packet loss is probabilistic
- Increase loss rate to 50%: `-Dexec.args="8888 0 50"`
- Run the demo multiple times

### No callbacks received
- Ensure monitor registered before operations
- Check monitor TTL hasn't expired

### Incorrect balances in ALO mode
- This is **EXPECTED behavior** - it demonstrates why AMO is important!

### ALREADY_EXISTS error in ALO mode
- This is also **EXPECTED** - it shows ALO re-executed the operation
- The first execution succeeded, but reply was lost
- Retry caused second execution which found the resource already exists
