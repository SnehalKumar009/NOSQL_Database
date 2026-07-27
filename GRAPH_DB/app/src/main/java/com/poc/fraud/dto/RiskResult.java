package com.poc.fraud.dto;

import java.util.List;

/**
 * Result of evaluating a user's fraud risk based on graph proximity to a banned user.
 */
public record RiskResult(
        String userId,
        boolean flagged,
        int riskScore,
        String riskLevel,
        Integer pathLength,
        String connectedBannedUserId,
        List<String> path
) {
}
