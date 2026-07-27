package com.poc.fraud.dto;

/** Another user that shares an entity (card/device/ip/phone) with the target user. */
public record SharedEntity(
        String entityType,
        String entityValue,
        String otherUserId,
        String otherUserStatus
) {
}
