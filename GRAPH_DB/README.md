# Fraud Ring Detection — Graph DB POC (Neo4j + Spring Boot)

A production-grade, dockerized Proof of Concept showing why a **graph database** beats
recursive SQL joins for **fraud ring detection**. Fraudsters reuse shared entities
(IP addresses, devices, phone numbers, cards). Finding a new user's hidden connection to a
**banned** account is a multi-hop traversal — trivial in Cypher, painful in SQL.

## Stack

- **Neo4j 5 Community** (Cypher, Bolt, Neo4j Browser)
- **Spring Boot 3.4.1** (Java 21) + **Spring Data Neo4j**
- **Docker Compose** for one-command startup
- **Testcontainers** for integration tests

## Graph model

```
(:User {id, name, status, riskFlag})
   -[:OWNS]->        (:CreditCard {number})
   -[:USED_FROM]->   (:IPAddress {addr})
   -[:USED_DEVICE]-> (:Device {id})
   -[:HAS_PHONE]->   (:PhoneNumber {number})
```

Two users that share an entity node are indirectly connected. A variable-length path from a
new user to a `banned` user reveals the ring.

Seeded ring (4 hops):

```
[user_new] --USED_FROM--> (192.168.1.100) <--USED_FROM-- [user_mule]
                                                              |
                                                        USED_DEVICE
                                                              v
[user_banned] --USED_DEVICE--> (device-AA11) <---------------+
```

## Run

```bash
cd GRAPH_DB
cp .env.example .env          # optional: change credentials
docker compose up --build
```

- API: http://localhost:8080
- Neo4j Browser: http://localhost:7474  (user `neo4j`, password `password123`)
- Health: http://localhost:8080/actuator/health

Sample data seeds automatically on first startup (controlled by `APP_SEED_ENABLED`).

## REST API

Evaluate a transaction (the POC trigger):

```bash
curl -s -X POST http://localhost:8080/transactions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user_new","amount":250.00}'
```

Expected: `flagged: true`, a `HIGH`/`MEDIUM` risk level, the `pathLength`, the
`connectedBannedUserId`, and the offending `path`.

Compare with an isolated user (not flagged):

```bash
curl -s -X POST http://localhost:8080/transactions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user_bob","amount":250.00}'
```

Other endpoints:

```bash
curl http://localhost:8080/users/user_new/fraud-ring
curl http://localhost:8080/users/user_new/shared-entities
```

Override traversal depth with `?maxHops=N` (1–6).

## Visualize in Neo4j Browser

Open http://localhost:7474 and run:

```cypher
// The whole ring
MATCH p = (u:User {id:'user_new'})-[*1..4]-(b:User {status:'banned'})
RETURN p;

// Everything
MATCH (n) RETURN n;
```

## Test

```bash
cd app
mvn test        # spins up an ephemeral Neo4j via Testcontainers (Docker required)
```

## Configuration

| Env var            | Default                 | Purpose                          |
|--------------------|-------------------------|----------------------------------|
| `NEO4J_URI`        | `bolt://localhost:7687` | Bolt connection                  |
| `NEO4J_USER`       | `neo4j`                 | Username                         |
| `NEO4J_PASSWORD`   | `password123`           | Password                         |
| `APP_SEED_ENABLED` | `true`                  | Load sample data on startup      |
| `SERVER_PORT`      | `8080`                  | API port                         |
