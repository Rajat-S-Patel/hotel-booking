package com.rajat.hotelbooking.property.controller;

import com.rajat.hotelbooking.common.dto.PageResponse;
import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.property.dto.PropertyResponse;
import com.rajat.hotelbooking.property.dto.PropertySearchRequest;
import com.rajat.hotelbooking.property.dto.PropertySearchResultResponse;
import com.rajat.hotelbooking.property.service.PropertyQueryService;
import com.rajat.hotelbooking.property.service.PropertySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Properties", description = "Search available properties and view property details")
@RestController
@RequestMapping(ApiPaths.V1 + "/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertySearchService searchService;
    private final PropertyQueryService queryService;

    @Operation(summary = "Search properties with availability for the requested stay")
    @GetMapping("/search")
    public PageResponse<PropertySearchResultResponse> search(@Valid @ParameterObject @ModelAttribute PropertySearchRequest request) {
        return searchService.search(request);
    }

    @Operation(summary = "Property details with room types, variants, features and cancellation policy")
    @GetMapping("/{propertyId}")
    public PropertyResponse getProperty(@PathVariable UUID propertyId) {
        return queryService.getProperty(propertyId);
    }
}
