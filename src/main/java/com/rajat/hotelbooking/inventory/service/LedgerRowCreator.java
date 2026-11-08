package com.rajat.hotelbooking.inventory.service;

import com.rajat.hotelbooking.common.exception.NotFoundException;
import com.rajat.hotelbooking.inventory.entity.LedgerEntry;
import com.rajat.hotelbooking.inventory.repository.LedgerEntryRepository;
import com.rajat.hotelbooking.property.entity.Variant;
import com.rajat.hotelbooking.property.repository.VariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

// separate bean so REQUIRES_NEW applies; a duplicate insert from a parallel booking
// only rolls back this small transaction
@Component
@RequiredArgsConstructor
class LedgerRowCreator {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final VariantRepository variantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(UUID variantId, LocalDate night) {
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> NotFoundException.of("variant", variantId));
        ledgerEntryRepository.saveAndFlush(new LedgerEntry(variant, night));
    }
}
