# Demo Guide — Fraud Ring Detection (Neo4j Graph DB POC)

A step-by-step script to demonstrate the POC end to end. Run all commands from the
`GRAPH_DB/` directory unless noted. On Linux/Mac use `curl`; on Windows PowerShell,
`curl` maps to `Invoke-WebRequest` — use `curl.exe` or the PowerShell snippets provided.

---

## 0. Prerequisites

- Docker + Docker Compose installed and running
- Ports free: `7474`, `7687` (Neo4j), `8080` (API)

```bash
cd GRAPH_DB
cp .env.example .env    # optional — defaults: neo4j / password123
```

---

## 1. Start the stack & verify containers are healthy

Build and launch both services:

```bash
docker compose up --build -d
```

Check the containers are up and `fraud-neo4j` reports **healthy**:

```bash
docker compose ps
```

Expected — two services, `fraud-neo4j` shows `(healthy)`:

```
NAME           IMAGE               STATUS                   PORTS
fraud-neo4j    neo4j:5-community   Up (healthy)             0.0.0.0:7474->7474, 0.0.0.0:7687->7687
fraud-api      fraud-graph-poc     Up                       0.0.0.0:8080->8080
```

The API waits for Neo4j's healthcheck before starting (`depends_on: condition: service_healthy`).

Confirm the API is healthy:

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP"}
```

> Tip: watch startup logs with `docker compose logs -f api`.

---

## 2. Seed the data

Seeding is **automatic** on first startup (`APP_SEED_ENABLED=true`). The app loads a crafted
fraud ring plus unrelated "noise" users. Confirm it ran:

```bash
docker compose logs api | grep -i seed
```

Expected:

```
Seeding fraud-ring sample dataset...
Seed complete.
```

Verify the node counts directly in Neo4j:

```bash
docker exec -it fraud-neo4j cypher-shell -u neo4j -p password123 \
  "MATCH (n) RETURN labels(n)[0] AS type, count(*) AS count ORDER BY type;"
```

Expected (5 users, plus entity nodes):

```
type          count
CreditCard    5
Device        2
IPAddress     3
PhoneNumber   1
User          5
```

**Re-seed from scratch** (wipe volumes and restart):

```bash
docker compose down -v
docker compose up --build -d
```

The seeded fraud ring:

```
[user_new] --USED_FROM--> (192.168.1.100) <--USED_FROM-- [user_mule]
                                                              |
                                                        USED_DEVICE
                                                              v
[user_banned] --USED_DEVICE--> (device-AA11) <---------------+
```

`user_new` is a brand-new, clean account — but sits **4 hops** from a banned user through
shared infrastructure.

---

## 3. Show the POC working

### 3a. The trigger — flag a hidden fraud connection

A new user makes a transaction. The graph traverses outward to find any banned account:

```bash
curl -s -X POST http://localhost:8080/transactions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user_new","amount":250.0}'
```

Expected — **flagged**, connected to `user_banned` at 4 hops:

```json
{
  "userId": "user_new",
  "flagged": true,
  "riskScore": 40,
  "riskLevel": "MEDIUM",
  "pathLength": 4,
  "connectedBannedUserId": "user_banned",
  "path": [
    "User:user_new",
    "IPAddress:192.168.1.100",
    "User:user_mule",
    "Device:device-AA11",
    "User:user_banned"
  ]
}
```

### 3b. The contrast — a clean, isolated user is NOT flagged

```bash
curl -s -X POST http://localhost:8080/transactions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user_bob","amount":250.0}'
```

Expected:

```json
{"userId":"user_bob","flagged":false,"riskScore":0,"riskLevel":"LOW","pathLength":null,"connectedBannedUserId":null,"path":[]}
```

### 3c. Shared entities — who else touched the same infra?

```bash
curl -s http://localhost:8080/users/user_new/shared-entities
```

Reveals `user_new` shares an IP and phone with `user_mule` (the intermediary).

### 3d. The subgraph — data behind the decision

```bash
curl -s http://localhost:8080/users/user_new/fraud-ring
```

Returns the nodes and edges around `user_new` (useful for feeding a UI graph view).

> Depth is tunable on any call with `?maxHops=N` (1–6), e.g.
> `.../transactions/evaluate?maxHops=2` — at 2 hops `user_new` is NOT reachable to the
> banned node, demonstrating how ring detection depends on traversal depth.

---

## 4. Show HOW the graph DB works (visual proof)

Open **Neo4j Browser**: http://localhost:7474
Login: `neo4j` / `password123`

**Visualize the entire fraud ring** (single query, no joins):

```cypher
MATCH p = (u:User {id:'user_new'})-[*1..4]-(b:User {status:'banned'})
RETURN p;
```

You'll see the clean user visually linked to the banned user through the shared IP, mule
account, and shared device.

**See the whole graph:**

```cypher
MATCH (n) RETURN n;
```

**The core insight — why this beats SQL:** the same answer in a relational DB needs recursive
self-joins across user↔ip↔user↔device↔user bridge tables. In Cypher it's one
variable-length pattern:

```cypher
MATCH (new:User {id:'user_new'})
MATCH path = shortestPath((new)-[*1..4]-(bad:User {status:'banned'}))
RETURN [n IN nodes(path) | labels(n)[0] + ':' + coalesce(n.id,n.number,n.addr,n.deviceId)] AS chain,
       length(path) AS hops;
```

---

## 5. Talking points (why Graph DB)

- **Multi-hop is native**: 4th/5th-degree connections are a single pattern, not N recursive joins.
- **Performance**: traversal cost scales with the ring size touched, not total table size.
- **Model = domain**: entities and relationships map 1:1 to fraud investigators' mental model.
- **Explainable**: the returned `path` is the literal evidence chain for an analyst.

---

## 6. Teardown

```bash
docker compose down        # stop, keep data
docker compose down -v     # stop and wipe Neo4j volumes
```
