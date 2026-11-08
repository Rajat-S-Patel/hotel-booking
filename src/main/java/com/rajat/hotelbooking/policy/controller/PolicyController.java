package com.rajat.hotelbooking.policy.controller;

import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.policy.dto.PolicyResponse;
import com.rajat.hotelbooking.policy.service.PolicyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cancellation policies", description = "Platform cancellation policies a property can choose")
@RestController
@RequestMapping(ApiPaths.V1 + "/cancellation-policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyQueryService policyQueryService;

    @Operation(summary = "List platform cancellation policies with their current tiers")
    @GetMapping
    public List<PolicyResponse> listPlatformPolicies() {
        return policyQueryService.listPlatformPolicies();
    }
}
