package com.poc.fraud.dto;

import java.util.List;

/** A subgraph around a user, suitable for visualization. */
public record RingGraph(
        String userId,
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {
    public record GraphNode(String label, String key, String status) {
    }

    public record GraphEdge(String sourceLabel, String source, String type, String targetLabel, String target) {
    }
}
