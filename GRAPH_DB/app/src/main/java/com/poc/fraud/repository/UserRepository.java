package com.poc.fraud.repository;

import com.poc.fraud.domain.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends Neo4jRepository<User, Long> {

    @Query("MATCH (u:User {id: $id}) RETURN u")
    Optional<User> findByBusinessId(@Param("id") String id);
}
