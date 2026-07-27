package com.poc.fraud.service;

import com.poc.fraud.dto.RingGraph;
import com.poc.fraud.dto.RiskResult;
import com.poc.fraud.dto.SharedEntity;
import com.poc.fraud.repository.FraudQueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FraudService {

    private final FraudQueryRepository queryRepository;
    private final int defaultMaxHops;

    public FraudService(FraudQueryRepository queryRepository,
                        @Value("${app.fraud.default-max-hops:4}") int defaultMaxHops) {
        this.queryRepository = queryRepository;
        this.defaultMaxHops = defaultMaxHops;
    }

    /**
     * Evaluates fraud risk for a user by measuring graph proximity to any banned user.
     * Risk score follows a hop-distance heuristic: the closer the banned user, the higher the score.
     */
    public RiskResult evaluate(String userId, Integer maxHops) {
        int hops = maxHops == null ? defaultMaxHops : maxHops;
        if (!queryRepository.userExists(userId)) {
            throw new UserNotFoundException(userId);
        }

        Optional<FraudQueryRepository.PathHit> hit = queryRepository.shortestPathToBanned(userId, hops);
        if (hit.isEmpty()) {
            return new RiskResult(userId, false, 0, "LOW", null, null, List.of());
        }

        FraudQueryRepository.PathHit path = hit.get();
        int score = scoreFor(path.pathLength());
        String level = levelFor(path.pathLength());
        return new RiskResult(userId, true, score, level, path.pathLength(), path.bannedUserId(), path.path());
    }

    public RingGraph fraudRing(String userId, Integer maxHops) {
        int hops = maxHops == null ? defaultMaxHops : maxHops;
        if (!queryRepository.userExists(userId)) {
            throw new UserNotFoundException(userId);
        }
        return queryRepository.subgraph(userId, hops);
    }

    public List<SharedEntity> sharedEntities(String userId) {
        if (!queryRepository.userExists(userId)) {
            throw new UserNotFoundException(userId);
        }
        return queryRepository.sharedEntities(userId);
    }

    /** Hop-distance heuristic: 1 hop -> 100, each extra hop subtracts 20, floored at 10. */
    private int scoreFor(int pathLength) {
        int score = 100 - (pathLength - 1) * 20;
        return Math.max(score, 10);
    }

    private String levelFor(int pathLength) {
        if (pathLength <= 2) {
            return "HIGH";
        }
        if (pathLength <= 4) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
