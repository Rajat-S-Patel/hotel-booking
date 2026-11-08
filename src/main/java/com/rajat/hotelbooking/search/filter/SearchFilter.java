package com.rajat.hotelbooking.search.filter;

import com.rajat.hotelbooking.search.dto.PropertySearchRequest;

public interface SearchFilter {

    SearchCandidate apply(SearchCandidate candidate, PropertySearchRequest request);
}
