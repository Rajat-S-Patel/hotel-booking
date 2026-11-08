package com.rajat.hotelbooking.property.controller;

import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.property.dto.CreatePropertyRequest;
import com.rajat.hotelbooking.property.dto.OnboardOwnerRequest;
import com.rajat.hotelbooking.property.dto.OwnerResponse;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.service.PropertyOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Owners", description = "Onboard a standalone property or a multi-property owner")
@RestController
@RequestMapping(ApiPaths.V1 + "/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final PropertyOnboardingService onboardingService;

    @Operation(summary = "Onboard an owner with one or more properties (a standalone hotel is an owner with one property)")
    @PostMapping
    public ResponseEntity<OwnerResponse> onboardOwner(@Valid @RequestBody OnboardOwnerRequest request) {
        OwnerResponse owner = onboardingService.onboardOwner(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{ownerId}").buildAndExpand(owner.id()).toUri();
        return ResponseEntity.created(location).body(owner);
    }

    @Operation(summary = "Add a property to an existing owner")
    @PostMapping("/{ownerId}/properties")
    public ResponseEntity<PropertyResponse> addProperty(@PathVariable UUID ownerId,
                                                        @Valid @RequestBody CreatePropertyRequest request) {
        PropertyResponse property = onboardingService.addProperty(ownerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiPaths.V1 + "/properties/{propertyId}").buildAndExpand(property.id()).toUri();
        return ResponseEntity.created(location).body(property);
    }
}
