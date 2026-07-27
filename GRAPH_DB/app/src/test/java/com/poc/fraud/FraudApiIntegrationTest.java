package com.poc.fraud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FraudApiIntegrationTest {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5-community"))
            .withAdminPassword("testpassword");

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
        registry.add("app.seed.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void userInRingIsFlaggedAsFraud() throws Exception {
        mockMvc.perform(post("/transactions/evaluate")
                        .contentType("application/json")
                        .content("{\"userId\":\"user_new\",\"amount\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andExpect(jsonPath("$.connectedBannedUserId").value("user_banned"))
                .andExpect(jsonPath("$.pathLength").value(4))
                .andExpect(jsonPath("$.riskScore").value(40));
    }

    @Test
    void isolatedUserIsNotFlagged() throws Exception {
        mockMvc.perform(post("/transactions/evaluate")
                        .contentType("application/json")
                        .content("{\"userId\":\"user_bob\",\"amount\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(false))
                .andExpect(jsonPath("$.riskScore").value(0));
    }

    @Test
    void sharedEntitiesRevealsMuleAccount() throws Exception {
        mockMvc.perform(get("/users/user_new/shared-entities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.otherUserId == 'user_mule')]").exists());
    }

    @Test
    void fraudRingReturnsSubgraph() throws Exception {
        mockMvc.perform(get("/users/user_new/fraud-ring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isNotEmpty())
                .andExpect(jsonPath("$.edges").isNotEmpty());
    }

    @Test
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(post("/transactions/evaluate")
                        .contentType("application/json")
                        .content("{\"userId\":\"does_not_exist\",\"amount\":10.0}"))
                .andExpect(status().isNotFound());
    }
}
