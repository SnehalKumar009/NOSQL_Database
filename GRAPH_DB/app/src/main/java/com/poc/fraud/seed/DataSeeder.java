package com.poc.fraud.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Loads a crafted fraud-ring dataset on startup when {@code app.seed.enabled=true}.
 *
 * <p>The ring: a clean, newly-registered user is connected — via a shared IP and a shared
 * device through an intermediary "mule" account — to a known banned user, forming a 4-hop
 * path that recursive SQL joins would struggle to surface. Isolated clean users are added
 * as noise.
 */
@Component
@Order(2)
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final Neo4jClient client;
    private final boolean seedEnabled;

    public DataSeeder(Neo4jClient client,
                      org.springframework.core.env.Environment env) {
        this.client = client;
        this.seedEnabled = Boolean.parseBoolean(env.getProperty("app.seed.enabled", "true"));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void seed() {
        if (!seedEnabled) {
            log.info("Seeding disabled (app.seed.enabled=false)");
            return;
        }

        long userCount = client.query("MATCH (u:User) RETURN count(u) AS c")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
        if (userCount > 0) {
            log.info("Data already present ({} users) - skipping seed", userCount);
            return;
        }

        log.info("Seeding fraud-ring sample dataset...");
        client.query(SEED_CYPHER).run();
        log.info("Seed complete.");
    }

    private static final String SEED_CYPHER = """
            // --- The fraud ring ---
            MERGE (clean:User {id: 'user_new'})       SET clean.name = 'Nina New',    clean.status = 'active', clean.riskFlag = false
            MERGE (mule:User  {id: 'user_mule'})      SET mule.name  = 'Manny Mule',  mule.status  = 'active', mule.riskFlag  = false
            MERGE (banned:User {id: 'user_banned'})   SET banned.name = 'Boris Banned', banned.status = 'banned', banned.riskFlag = true

            MERGE (ip:IPAddress {addr: '192.168.1.100'})
            MERGE (device:Device {id: 'device-AA11'})
            MERGE (phone:PhoneNumber {number: '+1-555-0101'})

            // clean --(shared IP)--> mule --(shared device)--> banned  => 4 hops clean..banned
            MERGE (clean)-[:USED_FROM]->(ip)
            MERGE (mule)-[:USED_FROM]->(ip)
            MERGE (mule)-[:USED_DEVICE]->(device)
            MERGE (banned)-[:USED_DEVICE]->(device)

            // clean and mule also share a phone (1-hop shared-entity demo)
            MERGE (clean)-[:HAS_PHONE]->(phone)
            MERGE (mule)-[:HAS_PHONE]->(phone)

            // give each user a distinct card
            MERGE (c1:CreditCard {number: '4111-1111-1111-1111'})
            MERGE (c2:CreditCard {number: '4222-2222-2222-2222'})
            MERGE (c3:CreditCard {number: '4333-3333-3333-3333'})
            MERGE (clean)-[:OWNS]->(c1)
            MERGE (mule)-[:OWNS]->(c2)
            MERGE (banned)-[:OWNS]->(c3)

            // --- Noise: isolated legitimate users, not connected to the ring ---
            MERGE (a:User {id: 'user_alice'}) SET a.name = 'Alice Clean', a.status = 'active', a.riskFlag = false
            MERGE (b:User {id: 'user_bob'})   SET b.name = 'Bob Clean',   b.status = 'active', b.riskFlag = false
            MERGE (ipA:IPAddress {addr: '10.0.0.5'})
            MERGE (ipB:IPAddress {addr: '10.0.0.6'})
            MERGE (devA:Device {id: 'device-ZZ99'})
            MERGE (cA:CreditCard {number: '4999-0000-0000-0001'})
            MERGE (cB:CreditCard {number: '4999-0000-0000-0002'})
            MERGE (a)-[:USED_FROM]->(ipA)
            MERGE (a)-[:USED_DEVICE]->(devA)
            MERGE (a)-[:OWNS]->(cA)
            MERGE (b)-[:USED_FROM]->(ipB)
            MERGE (b)-[:OWNS]->(cB)
            """;
}
