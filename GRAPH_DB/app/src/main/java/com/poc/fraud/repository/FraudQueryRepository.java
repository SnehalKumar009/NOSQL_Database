package com.poc.fraud.repository;

import com.poc.fraud.dto.RingGraph;
import com.poc.fraud.dto.SharedEntity;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Graph traversal queries backed by {@link Neo4jClient}. Variable-length paths use an
 * integer hop bound that Cypher cannot parameterize, so it is validated and interpolated
 * safely (see {@link #safeHops(int)}).
 */
@Repository
public class FraudQueryRepository {

    private static final int MAX_ALLOWED_HOPS = 6;

    private final Neo4jClient client;

    public FraudQueryRepository(Neo4jClient client) {
        this.client = client;
    }

    public boolean userExists(String userId) {
        return client.query("MATCH (u:User {id: $id}) RETURN count(u) > 0 AS present")
                .bind(userId).to("id")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    /**
     * Finds the shortest path (up to {@code maxHops}) from the given user to any banned user.
     * Returns empty when no such path exists.
     */
    public Optional<PathHit> shortestPathToBanned(String userId, int maxHops) {
        int hops = safeHops(maxHops);
        String cypher = """
                MATCH (u:User {id: $id})
                MATCH p = shortestPath((u)-[*1..%d]-(b:User {status: 'banned'}))
                WHERE b.id <> $id
                RETURN length(p) AS pathLength,
                       b.id AS bannedUserId,
                       [n IN nodes(p) | labels(n)[0] + ':' + coalesce(n.id, n.number, n.addr, n.deviceId)] AS pathNodes
                ORDER BY pathLength ASC
                LIMIT 1
                """.formatted(hops);

        return client.query(cypher)
                .bind(userId).to("id")
                .fetch()
                .one()
                .map(row -> new PathHit(
                        ((Number) row.get("pathLength")).intValue(),
                        (String) row.get("bannedUserId"),
                        castList(row.get("pathNodes"))
                ));
    }

    /** Returns the connected subgraph (nodes + edges) around a user for visualization. */
    public RingGraph subgraph(String userId, int maxHops) {
        int hops = safeHops(maxHops);

        String nodeCypher = """
                MATCH path = (u:User {id: $id})-[*0..%d]-(n)
                UNWIND nodes(path) AS x
                RETURN DISTINCT labels(x)[0] AS label,
                       coalesce(x.id, x.number, x.addr, x.deviceId) AS key,
                       x.status AS status
                """.formatted(hops);

        List<RingGraph.GraphNode> nodes = client.query(nodeCypher)
                .bind(userId).to("id")
                .fetch()
                .all()
                .stream()
                .map(row -> new RingGraph.GraphNode(
                        (String) row.get("label"),
                        (String) row.get("key"),
                        (String) row.get("status")))
                .toList();

        String edgeCypher = """
                MATCH path = (u:User {id: $id})-[*1..%d]-(n)
                UNWIND relationships(path) AS r
                RETURN DISTINCT labels(startNode(r))[0] AS sourceLabel,
                       coalesce(startNode(r).id, startNode(r).number, startNode(r).addr, startNode(r).deviceId) AS source,
                       type(r) AS type,
                       labels(endNode(r))[0] AS targetLabel,
                       coalesce(endNode(r).id, endNode(r).number, endNode(r).addr, endNode(r).deviceId) AS target
                """.formatted(hops);

        List<RingGraph.GraphEdge> edges = client.query(edgeCypher)
                .bind(userId).to("id")
                .fetch()
                .all()
                .stream()
                .map(row -> new RingGraph.GraphEdge(
                        (String) row.get("sourceLabel"),
                        (String) row.get("source"),
                        (String) row.get("type"),
                        (String) row.get("targetLabel"),
                        (String) row.get("target")))
                .toList();

        return new RingGraph(userId, nodes, edges);
    }

    /** Other users that share a card/device/ip/phone entity with the given user (1 hop out). */
    public List<SharedEntity> sharedEntities(String userId) {
        String cypher = """
                MATCH (u:User {id: $id})-[]->(e)<-[]-(other:User)
                WHERE other.id <> $id
                RETURN labels(e)[0] AS entityType,
                       coalesce(e.id, e.number, e.addr, e.deviceId) AS entityValue,
                       other.id AS otherUserId,
                       other.status AS otherUserStatus
                ORDER BY otherUserStatus, otherUserId
                """;

        return client.query(cypher)
                .bind(userId).to("id")
                .fetch()
                .all()
                .stream()
                .map(row -> new SharedEntity(
                        (String) row.get("entityType"),
                        (String) row.get("entityValue"),
                        (String) row.get("otherUserId"),
                        (String) row.get("otherUserStatus")))
                .toList();
    }

    private static int safeHops(int maxHops) {
        if (maxHops < 1) {
            return 1;
        }
        return Math.min(maxHops, MAX_ALLOWED_HOPS);
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return value == null ? List.of() : (List<String>) value;
    }

    /** Shortest-path lookup result. */
    public record PathHit(int pathLength, String bannedUserId, List<String> path) {
    }
}
