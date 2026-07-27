package com.poc.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Incoming transaction to be evaluated (the POC trigger). */
public record TransactionRequest(
        @NotBlank String userId,
        @Positive double amount
) {
}
