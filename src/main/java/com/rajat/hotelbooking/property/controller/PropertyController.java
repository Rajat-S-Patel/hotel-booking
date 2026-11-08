package com.rajat.hotelbooking.property.controller;

import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.service.PropertyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Properties", description = "Property details")
@RestController
@RequestMapping(ApiPaths.V1 + "/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyQueryService queryService;

    @Operation(summary = "Get property details")
    @GetMapping("/{propertyId}")
    public PropertyResponse getProperty(@PathVariable UUID propertyId) {
        return queryService.getProperty(propertyId);
    }
}
