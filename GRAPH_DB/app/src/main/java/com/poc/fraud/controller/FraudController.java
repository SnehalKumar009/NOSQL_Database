package com.poc.fraud.controller;

import com.poc.fraud.dto.RingGraph;
import com.poc.fraud.dto.RiskResult;
import com.poc.fraud.dto.SharedEntity;
import com.poc.fraud.dto.TransactionRequest;
import com.poc.fraud.service.FraudService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    /** POC trigger: evaluate an incoming transaction against the fraud graph. */
    @PostMapping("/transactions/evaluate")
    public RiskResult evaluateTransaction(@Valid @RequestBody TransactionRequest request,
                                          @RequestParam(name = "maxHops", required = false) Integer maxHops) {
        return fraudService.evaluate(request.userId(), maxHops);
    }

    @GetMapping("/users/{id}/fraud-ring")
    public RingGraph fraudRing(@PathVariable("id") String userId,
                               @RequestParam(name = "maxHops", required = false) Integer maxHops) {
        return fraudService.fraudRing(userId, maxHops);
    }

    @GetMapping("/users/{id}/shared-entities")
    public List<SharedEntity> sharedEntities(@PathVariable("id") String userId) {
        return fraudService.sharedEntities(userId);
    }
}
