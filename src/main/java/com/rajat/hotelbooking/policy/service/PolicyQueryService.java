package com.rajat.hotelbooking.policy.service;

import com.rajat.hotelbooking.policy.dto.PolicyResponse;
import com.rajat.hotelbooking.policy.mapper.PolicyMapper;
import com.rajat.hotelbooking.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyQueryService {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;

    public List<PolicyResponse> listPlatformPolicies() {
        return policyRepository.findByOwnerIdIsNullOrderByNameAsc().stream()
                .map(policyMapper::toResponse)
                .toList();
    }
}
