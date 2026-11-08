package com.rajat.hotelbooking.reference.controller;

import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.reference.dto.AmenityResponse;
import com.rajat.hotelbooking.reference.dto.CityResponse;
import com.rajat.hotelbooking.reference.dto.LocalityResponse;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
import com.rajat.hotelbooking.reference.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reference data", description = "Cities, localities and amenities used to build requests")
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @Operation(summary = "List cities")
    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return referenceDataService.listCities();
    }

    @Operation(summary = "List localities of a city")
    @GetMapping("/cities/{cityId}/localities")
    public List<LocalityResponse> listLocalities(@PathVariable UUID cityId) {
        return referenceDataService.listLocalities(cityId);
    }

    @Operation(summary = "List amenities, optionally filtered by scope (PROPERTY or ROOM)")
    @GetMapping("/amenities")
    public List<AmenityResponse> listAmenities(@RequestParam(required = false) AmenityScope scope) {
        return referenceDataService.listAmenities(scope);
    }
}
