# Distributed Banking System - Complete 15-Minute Demo Guide

This document provides a comprehensive step-by-step demo script covering ALL project requirements.

---

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- **3 computers recommended**: 1 server + 2 clients (or use multiple terminals on one machine)
- **Compile first**: Run `mvn compile` once before starting

---

## Demo Overview (15 Minutes)

| Time | Section | Features Demonstrated |
|------|---------|----------------------|
| 0:00-2:00 | Setup & Introduction | UDP transport, server startup |
| 2:00-5:00 | Basic Banking Operations | Open, Deposit, Withdraw, Query Balance, Close |
| 5:00-7:00 | Transfer Between Accounts | Non-idempotent operation, multi-client |
| 7:00-9:00 | Callback Notifications | Monitor registration, concurrent monitoring |
| 9:00-13:00 | ALO vs AMO Comparison | Invocation semantics, packet loss simulation |
| 13:00-15:00 | Summary & Q&A | Key points, answer questions |

---

## Equipment Setup

| Machine | Role | What to Run |
|---------|------|-------------|
| **Computer A** | Server | `BankServer` |
| **Computer B** | Client 1 (Alice) | `BankClient` |
| **Computer C** | Client 2 (Bob) + Monitor | `BankClient` + `MonitorClient` |

**Note**: Replace `<SERVER_IP>` with Computer A's IP address. Use `localhost` if all on same machine.

---

## PART 1: Setup & Introduction (2 minutes)

### Step 1.1: Start the Server (Computer A)

```bash
cd /path/to/project
mvn compile
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
```

**Expected Output:**
```
╔═══════════════════════════════════════════════════╗
║     Distributed Banking System - Server v1.1      ║
╠═══════════════════════════════════════════════════╣
║  Protocol: UDP                                    ║
║  Port: 8888                                       ║
║  Loss Simulation: DISABLED                        ║
╚═══════════════════════════════════════════════════╝
```

**Explain to Professor:**
> "This is our UDP server listening on port 8888. All communication uses UDP DatagramSocket, not TCP. We implemented manual byte-level marshalling using ByteBuffer with Big-Endian encoding."

### Step 1.2: Start Client 1 - Alice (Computer B)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="<SERVER_IP> 8888 1001 AMO"
```

### Step 1.3: Start Client 2 - Bob (Computer C)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="<SERVER_IP> 8888 1002 AMO"
```

---

## PART 2: Basic Banking Operations (3 minutes)

**Features Demonstrated:**
- ✅ Open Account (with initial balance)
- ✅ Deposit (with currency type)
- ✅ Withdraw (with currency type)
- ✅ Query Balance (idempotent operation)
- ✅ Close Account

### Step 2.1: Open Account (Computer B - Alice)

```
> login alice password123
Logged in as: alice

> open SGD 1000
Account created successfully!
Account Number: 1001
Initial Balance: $1000.00
```

**Explain:**
> "Opening an account requires: name, password, currency type, and initial balance - exactly as specified in the project requirements."

### Step 2.2: Deposit (Computer B - Alice)

```
> deposit SGD 500
Deposit successful!
Deposited: $500.00 SGD
New balance: $1500.00
```

**Explain:**
> "Deposit requires specifying the currency type. The server validates that the currency matches the account's currency."

### Step 2.3: Withdraw (Computer B - Alice)

```
> withdraw SGD 300
Withdrawal successful!
Withdrew: $300.00
New balance: $1200.00
```

### Step 2.4: Query Balance - Idempotent Operation (Computer B - Alice)

```
> balance
Account: 1001
Balance: $1200.00 SGD
```

**Explain:**
> "Query Balance is an IDEMPOTENT operation - executing it multiple times has the same effect. This is one of our two additional operations as required."

### Step 2.5: Close Account Demo (Quick Demo)

```
> close
Account closed successfully!
Final balance returned: $1200.00
```

**Note:** For the rest of the demo, reopen the account:
```
> open SGD 1000
Account Number: 1001
```

---

## PART 3: Transfer Between Accounts (2 minutes)

**Features Demonstrated:**
- ✅ Transfer (non-idempotent operation)
- ✅ Multiple clients accessing the system
- ✅ Account number is integer format

### Step 3.1: Bob Opens Account (Computer C)

```
> login bob password456
Logged in as: bob

> open SGD 500
Account created successfully!
Account Number: 1002
Initial Balance: $500.00
```

### Step 3.2: Alice Transfers to Bob (Computer B)

```
> transfer 1002 300
Transfer successful!
Your new balance: $700.00
```

**Explain:**
> "Transfer is a NON-IDEMPOTENT operation - executing it twice would transfer money twice. This is our second additional operation. The account number 1002 is an integer as required."

### Step 3.3: Verify Transfer (Both Computers)

**Computer B (Alice):**
```
> balance
Balance: $700.00 SGD
```

**Computer C (Bob):**
```
> balance
Balance: $800.00 SGD
```

**Explain:**
> "Alice: 1000 - 300 = 700. Bob: 500 + 300 = 800. The transfer was executed correctly."

---

## PART 4: Callback Notifications (2 minutes)

**Features Demonstrated:**
- ✅ Callback registration for monitoring
- ✅ Monitor interval (TTL)
- ✅ Multiple clients monitoring concurrently

### Step 4.1: Restart Server (Computer A)

Press Ctrl+C to stop, then restart:
```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"
```

### Step 4.2: Start Monitor Client 1 (Computer C - New Terminal)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="<SERVER_IP> 8888 9001 120 120"
```

**Expected Output:**
```
Registering for callbacks...
Successfully registered for callbacks!
Listening for ACCOUNT_UPDATE notifications...
```

### Step 4.3: Start Monitor Client 2 (Computer B - New Terminal, Optional)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="<SERVER_IP> 8888 9002 120 120"
```

**Explain:**
> "We now have TWO monitor clients registered concurrently. Both will receive callback notifications."

### Step 4.4: Perform Operations (Computer B - Original Terminal)

```
> login alice pass
> open SGD 1000
> deposit SGD 500
```

### Step 4.5: Observe Callbacks (Monitor Terminals)

**Both monitors should display:**
```
┌─────────────────────────────────────────────────┐
│ CALLBACK: Account 1001, Balance: $1000.00       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ CALLBACK: Account 1001, Balance: $1500.00       │
└─────────────────────────────────────────────────┘
```

**Explain:**
> "Callbacks are best-effort UDP notifications. Both monitors received updates. The monitor interval is 120 seconds - after that, the client is automatically unregistered."

---

## PART 5: ALO vs AMO Semantics Comparison (4 minutes)

**Features Demonstrated:**
- ✅ At-Least-Once (ALO) semantics
- ✅ At-Most-Once (AMO) semantics
- ✅ Packet loss simulation
- ✅ Client retry with timeout
- ✅ Duplicate request suppression

### Step 5.1: Restart Server WITH Packet Loss (Computer A)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 50"
```

**Note the loss simulation:**
```
║  Loss Simulation: req=0%, rep=50%               ║
```

**Explain:**
> "The server will now drop 50% of replies, simulating network unreliability. This triggers client retries."

### Step 5.2: Test with AMO Semantics (Computer B)

```bash
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="<SERVER_IP> 8888 1001 AMO"
```

```
> login alice pass
> open SGD 1000
```

**Watch the client logs for retries:**
```
[INFO] SEND to ... | op=OPEN_ACCOUNT | attempt=1
[WARN] Timeout (attempt 1/6, timeout=500ms)
[INFO] SEND to ... | op=OPEN_ACCOUNT | attempt=2
[INFO] RECV from ... | status=OK
```

**Watch the server logs for AMO cache:**
```
[INFO] AMO cache hit for clientId=1001, requestId=...
[INFO] REPLY to ... | [FROM AMO CACHE]
```

**Explain:**
> "Notice 'FROM AMO CACHE' in server log - the server detected a duplicate request and returned the cached reply WITHOUT re-executing the operation. This is At-Most-Once semantics."

### Step 5.3: Deposit with AMO (Computer B)

```
> deposit SGD 100
> balance
Balance: $1100.00
```

**Even with retries, balance is correct!**

### Step 5.4: Switch to ALO Semantics (Computer B)

```
> semantics ALO
Semantics set to: ALO

> deposit SGD 100
> balance
```

**Watch the server logs - NO cache hit:**
```
[INFO] Processing DEPOSIT: account=1001 amount=$100.00
[INFO] Processing DEPOSIT: account=1001 amount=$100.00  <-- Executed TWICE!
```

**Check balance - may be higher than expected!**
```
> balance
Balance: $1300.00  (Expected $1200, but got $1300 due to double execution!)
```

**Explain:**
> "With ALO, the deposit was executed TWICE because the reply was lost and client retried. This is why At-Most-Once semantics is CRITICAL for non-idempotent operations like deposit and transfer."

### Step 5.5: Summary Comparison

| Semantics | Cache | Behavior | Result |
|-----------|-------|----------|--------|
| **AMO** | Yes | Duplicate suppressed | Correct balance |
| **ALO** | No | Every request executed | May have wrong balance |

---

## PART 6: Key Technical Points (2 minutes)

### 6.1: Manual Marshalling

**Show code concept:**
```java
// Header.java - 32-byte fixed header
ByteBuffer buffer = ByteBuffer.allocate(32);
buffer.order(ByteOrder.BIG_ENDIAN);
buffer.putShort(magic);      // offset 0
buffer.put(version);         // offset 2
buffer.putShort(opCode);     // offset 6
buffer.putLong(requestId);   // offset 12
// ... all fields manually encoded
```

**Explain:**
> "We implemented marshalling manually using ByteBuffer. No Java serialization, no RPC frameworks. Every field is encoded at the exact byte offset."

### 6.2: Request ID Generation

```java
requestId = (clientId << 32) | seqNo;
// High 32 bits: clientId
// Low 32 bits: sequence number
// Same requestId for all retries of the same request
```

### 6.3: Exponential Backoff Retry

```
Timeout sequence: 500ms → 1000ms → 2000ms → 4000ms → 8000ms → 16000ms
Maximum retries: 5
```

### 6.4: AMO Cache Key

```java
CacheKey = (clientId, requestId)
// Uniquely identifies a request
// Same request's retries hit the cache
```

---

## Troubleshooting

### No retries visible
- Packet loss is probabilistic
- Increase loss rate to 70%: `-Dexec.args="8888 0 70"`
- Try multiple operations

### Clients can't connect
- Check server IP address
- Check firewall allows UDP port 8888
- Try using `localhost` if on same machine

### Balance is correct even with ALO
- You got lucky - no packet loss occurred
- Increase loss rate or try more operations

### ALREADY_EXISTS error
- This is EXPECTED with ALO when creating accounts
- Shows that the operation was executed twice

---

## Quick Command Reference

```bash
# Compile (once)
mvn compile

# Start server (no loss)
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"

# Start server (50% reply loss)
mvn exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 50"

# Start client (AMO)
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="<SERVER_IP> 8888 1001 AMO"

# Start client (ALO)
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="<SERVER_IP> 8888 1001 ALO"

# Start monitor
mvn exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="<SERVER_IP> 8888 9001 120 120"
```

## Client Commands Quick Reference

| Command | Example | Description |
|---------|---------|-------------|
| `login` | `login alice pass` | Set credentials |
| `open` | `open SGD 1000` | Open account with initial balance |
| `deposit` | `deposit SGD 500` | Deposit (requires currency) |
| `withdraw` | `withdraw SGD 200` | Withdraw (requires currency) |
| `balance` | `balance` | Query balance (idempotent) |
| `transfer` | `transfer 1002 100` | Transfer (non-idempotent) |
| `close` | `close` | Close account |
| `register` | `register 60` | Register for callbacks |
| `semantics` | `semantics ALO` | Switch semantics |
| `quit` | `quit` | Exit client |

---

## Project Requirements Checklist

| Requirement | Demo Section | Status |
|-------------|--------------|--------|
| UDP transport | All sections | ✅ |
| Open Account | Part 2, Step 2.1 | ✅ |
| Close Account | Part 2, Step 2.5 | ✅ |
| Deposit | Part 2, Step 2.2 | ✅ |
| Withdraw | Part 2, Step 2.3 | ✅ |
| Callback Monitoring | Part 4 | ✅ |
| Multiple clients monitoring | Part 4, Step 4.3 | ✅ |
| Idempotent operation (Query) | Part 2, Step 2.4 | ✅ |
| Non-idempotent operation (Transfer) | Part 3, Step 3.2 | ✅ |
| Manual Marshalling | Part 6 | ✅ |
| At-Least-Once semantics | Part 5, Step 5.4 | ✅ |
| At-Most-Once semantics | Part 5, Step 5.2 | ✅ |
| Timeout & Retry | Part 5 | ✅ |
| Packet loss simulation | Part 5, Step 5.1 | ✅ |
| ALO vs AMO experiment | Part 5 | ✅ |
