# Distributed Auction Platform with Java RMI

An academic client-server auction platform exploring remote method calls, replicated state and failover. Clients connect to a front-end service, which routes operations to a primary back-end replica.

## Features

- Email-based registration and an interactive command-line client.
- Creating auctions, listing items, placing bids and closing auctions.
- Three back-end replica processes with primary selection by the front end.
- State transfer for items, users, auction owners and highest bidders after selected updates.
- Availability checks and retry handling for selected remote operations.
- Validation of auction ownership, duplicate closure and bid values.
- Console status output and exception traces for startup and replica failures.

## Architecture

| Component | Role |
| --- | --- |
| `AuctionClient` | Interactive client using the front-end RMI binding. |
| `FrontEndServer` | Primary selection, routing and selected-operation failover. |
| `Replica` | Auction state, operations and replication. |
| `Auction`, `ReplicaInterface` | Remote method contracts. |
| `AuctionItem`, `AuctionSaleItem`, `AuctionResult` | Data exchanged by operations. |
| `AESKeyGenerator`, `keys/` | Coursework key-generation/loading assets. |
| `server.sh` | Starts the registry, replicas and front end. |

## Requirements and running

Use JDK 17 or newer: the client uses arrow-style switch cases. Bash is required for the startup script. Run commands from the repository root so key paths resolve.

```bash
git clone https://github.com/shaj9054/SCC-311.git\ncd SCC-311\njavac *.java\nbash server.sh
```

After the front end reports it is ready, open another terminal in the same directory:

```bash
java AuctionClient
```

Enter an email, then use the menu to create, list, bid on or close auctions. The code uses `localhost` RMI bindings and the registry's default port.

The startup script launches the registry and replica processes in the background. Stopping its foreground front end does not automatically stop every background process; stop those specific processes when finishing the demonstration.

## Keys and security scope

The repository contains coursework test keys. If `keys/testKey.aes` is absent, `java AESKeyGenerator` creates it; the replicas load or generate an RSA key pair. Key loading is present, but the current auction calls do not implement an authenticated, encrypted RMI transport. Treat the keys as demonstration assets, not deployment credentials.

## Implementation limitations

State is in memory. Replication does not transfer every field, including ID counters and closed-auction tracking, so failover can leave inconsistent state. Registration alone does not trigger state synchronisation. The front end is a single point of failure, retry coverage differs by operation, and reserve prices are not enforced by the current replica logic.

This coursework demonstrates distributed-system mechanisms; it is not a production auction service.

## Project context

**Module:** SCC-311, Lancaster University. **Language:** Java. **Author:** Mohammed Shajalal Sarwar.
