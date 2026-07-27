package com.poc.fraud.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates uniqueness constraints (which also back lookups with indexes) on startup.
 * Runs before the data seeder.
 */
@Component
@Order(1)
public class SchemaInitializer {

    private static final List<String> CONSTRAINTS = List.of(
            "CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE",
            "CREATE CONSTRAINT card_number IF NOT EXISTS FOR (c:CreditCard) REQUIRE c.number IS UNIQUE",
            "CREATE CONSTRAINT ip_addr IF NOT EXISTS FOR (i:IPAddress) REQUIRE i.addr IS UNIQUE",
            "CREATE CONSTRAINT device_id IF NOT EXISTS FOR (d:Device) REQUIRE d.id IS UNIQUE",
            "CREATE CONSTRAINT phone_number IF NOT EXISTS FOR (p:PhoneNumber) REQUIRE p.number IS UNIQUE"
    );

    private final Neo4jClient client;

    public SchemaInitializer(Neo4jClient client) {
        this.client = client;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void applyConstraints() {
        CONSTRAINTS.forEach(ddl -> client.query(ddl).run());
    }
}
