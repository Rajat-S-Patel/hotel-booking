package com.rajat.hotelbooking.search.controller;

import com.rajat.hotelbooking.common.dto.PageResponse;
import com.rajat.hotelbooking.common.web.ApiPaths;
import com.rajat.hotelbooking.search.dto.PropertySearchRequest;
import com.rajat.hotelbooking.search.dto.PropertySearchResultResponse;
import com.rajat.hotelbooking.search.service.PropertySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "Search available properties for a stay")
@RestController
@RequestMapping(ApiPaths.V1 + "/properties/search")
@RequiredArgsConstructor
public class SearchController {

    private final PropertySearchService searchService;

    @Operation(summary = "Search available properties")
    @GetMapping
    public PageResponse<PropertySearchResultResponse> search(@Valid @ParameterObject @ModelAttribute PropertySearchRequest request) {
        return searchService.search(request);
    }
}
