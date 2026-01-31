# Distributed Banking System

A UDP-based distributed banking system implementing Protocol v1.1 for the Distributed Systems course project.

## Overview

This project demonstrates key distributed systems concepts:
- **Manual byte-level marshalling/unmarshalling** using ByteBuffer (Big-Endian)
- **UDP transport** with DatagramSocket/DatagramPacket
- **At-Least-Once (ALO) vs At-Most-Once (AMO) invocation semantics**
- **Packet loss simulation** to demonstrate semantic differences
- **Callback notifications** for monitoring account updates
- **Client retry with exponential backoff**

## Project Structure

```
src/main/java/edu/ntu/ds/
├── protocol/           # Protocol encoding/decoding layer
│   ├── Constants.java      # Protocol constants (magic, offsets, etc.)
│   ├── MessageType.java    # REQ, REP, CBK message types
│   ├── OpCode.java         # Operation codes
│   ├── Semantics.java      # ALO/AMO semantics enum
│   ├── StatusCode.java     # Error/success status codes
│   ├── TlvType.java        # TLV field types
│   ├── Currency.java       # Currency enumeration
│   ├── Header.java         # 32-byte fixed header
│   ├── TlvField.java       # TLV field encoding
│   ├── Payload.java        # TLV collection
│   ├── Message.java        # Complete message
│   ├── ProtocolException.java
│   └── ProtocolTest.java   # Round-trip encoding tests
│
├── network/            # Networking layer
│   ├── UdpServer.java      # UDP server with loss simulation
│   ├── UdpClient.java      # UDP client with retry logic
│   ├── PacketLossSimulator.java
│   └── Logger.java         # Structured logging
│
├── service/            # Business logic layer
│   ├── Account.java        # Account entity
│   ├── AccountStore.java   # In-memory account storage
│   ├── BankingService.java # Banking operations
│   ├── AmoCache.java       # AMO reply cache
│   ├── CallbackRegistry.java
│   └── RequestProcessor.java
│
├── server/
│   └── BankServer.java     # Server main class
│
└── client/
    ├── BankClient.java     # Interactive CLI client
    └── MonitorClient.java  # Callback monitor client
```

## Building

```bash
# Compile
mvn compile

# Run protocol tests
mvn exec:java -Dexec.mainClass="edu.ntu.ds.protocol.ProtocolTest"

# Package JAR
mvn package
```

## Running

### Start the Server

```bash
# Basic server on port 8888
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888"

# Server with packet loss simulation (20% request loss, 20% reply loss)
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 20 20"
```

### Start an Interactive Client

```bash
# Basic client connecting to localhost:8888
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888"

# Client with specific ID and ALO semantics
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1001 ALO"

# Client with AMO semantics (default)
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1002 AMO"
```

### Start a Monitor Client

```bash
# Monitor client to observe callbacks
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="localhost 8888 9999 300 300"
```

## Client Commands

| Command | Description |
|---------|-------------|
| `login <user> <pass>` | Set credentials for operations |
| `logout` | Clear credentials |
| `open <currency>` | Open new account (SGD/USD/EUR/GBP/JPY/CNY) |
| `close` | Close current account |
| `use <accountNo>` | Select an account |
| `deposit <amount>` | Deposit funds |
| `withdraw <amount>` | Withdraw funds |
| `balance` | Query balance |
| `transfer <to> <amount>` | Transfer to another account |
| `register <ttl>` | Register for callback notifications |
| `unregister` | Stop receiving callbacks |
| `semantics <ALO\|AMO>` | Change invocation semantics |
| `status` | Show current session info |
| `help` | Show available commands |
| `quit` | Exit client |

## Protocol Specification (v1.1)

### Fixed Header (32 bytes, Big-Endian)

| Offset | Size | Type | Field | Description |
|--------|------|------|-------|-------------|
| 0 | 2 | u16 | magic | 0xD5D5 |
| 2 | 1 | u8 | version | 1 |
| 3 | 1 | u8 | msgType | 0=REQ, 1=REP, 2=CBK |
| 4 | 2 | u16 | headerLen | 32 |
| 6 | 2 | u16 | opCode | Operation code |
| 8 | 1 | u8 | semantics | 0=ALO, 1=AMO |
| 9 | 1 | u8 | flags | bit0=CRC, bit1=Error |
| 10 | 2 | u16 | status | Status code |
| 12 | 8 | u64 | requestId | (clientId << 32) | seqNo |
| 20 | 4 | u32 | clientId | Client ID |
| 24 | 4 | u32 | seqNo | Sequence number |
| 28 | 4 | u32 | payloadLen | Payload length |

### TLV Format

```
Type (u16) | Length (u16) | Value (bytes)
```

### Operation Codes

| Code | Name | Idempotent |
|------|------|------------|
| 0x0001 | OPEN_ACCOUNT | No |
| 0x0002 | CLOSE_ACCOUNT | No |
| 0x0003 | DEPOSIT | No |
| 0x0004 | WITHDRAW | No |
| 0x0005 | REGISTER_CALLBACK | Yes |
| 0x0006 | UNREGISTER_CALLBACK | Yes |
| 0x0101 | QUERY_BALANCE | Yes |
| 0x0102 | TRANSFER | No |
| 0x8001 | ACCOUNT_UPDATE | N/A (Callback) |

### Status Codes

| Code | Name | Description |
|------|------|-------------|
| 0 | OK | Success |
| 1 | BAD_REQUEST | Malformed or missing fields |
| 2 | AUTH_FAIL | Authentication failed |
| 3 | NOT_FOUND | Account not found |
| 4 | INSUFFICIENT_FUNDS | Insufficient balance |
| 5 | CURRENCY_MISMATCH | Currency mismatch |
| 6 | ALREADY_EXISTS | Resource exists |
| 7 | INTERNAL_ERROR | Server error |

## Invocation Semantics

### At-Least-Once (ALO)
- Server executes **every** received request
- No duplicate suppression
- May cause **double execution** if client retries due to lost reply
- Suitable for **idempotent** operations (e.g., QUERY_BALANCE)

### At-Most-Once (AMO)
- Server **caches replies** keyed by (clientId, requestId)
- Duplicate requests return **cached reply** without re-executing
- Ensures **exactly-once execution** even under packet loss
- Required for **non-idempotent** operations (e.g., TRANSFER)

### Demonstrating the Difference

With packet loss enabled:

1. **ALO + TRANSFER**: If reply is lost, client retries, server executes again → **double transfer**
2. **AMO + TRANSFER**: If reply is lost, client retries, server returns cached reply → **correct**

## Demo Scenario

### Terminal 1: Start Server with Packet Loss
```bash
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.server.BankServer" -Dexec.args="8888 0 30"
```
(30% reply loss to trigger retries)

### Terminal 2: Start Monitor Client
```bash
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.MonitorClient" -Dexec.args="localhost 8888 9999 300 300"
```

### Terminal 3: Interactive Client with AMO
```bash
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1001 AMO"
```

Then in client:
```
login alice secret123
open SGD
deposit 1000
balance
transfer ACC-1002 100
```

### Terminal 4: Second Interactive Client (for receiving transfers)
```bash
mvn compile exec:java -Dexec.mainClass="edu.ntu.ds.client.BankClient" -Dexec.args="localhost 8888 1002 AMO"
```

```
login bob pass456
open SGD
balance
```

### Demonstrating ALO vs AMO

1. Create two accounts with $1000 each
2. With **AMO** semantics, perform a transfer - even if retries occur, balance changes correctly
3. Change to **ALO** semantics (`semantics ALO`) and repeat - with packet loss, transfer may execute twice

## Client Retry Policy

Per Protocol v1.1:
- Initial timeout: 500ms
- Maximum retries: 5
- Backoff strategy: Exponential (500ms, 1s, 2s, 4s, 8s, 16s)
- Same requestId used for all retries

## Design Decisions

1. **Thread Safety**: AccountStore and AmoCache use ConcurrentHashMap
2. **Transfer Atomicity**: Both accounts locked in consistent order to prevent deadlocks
3. **Callback Best-Effort**: Callbacks are fire-and-forget (UDP semantics)
4. **AMO Cache Expiry**: Cached replies expire after 5 minutes to bound memory
5. **Monetary Values**: Stored as int64 cents to avoid floating-point issues

## Team Members

(Add your team members here)

## License

Educational use only - Distributed Systems Course Project
