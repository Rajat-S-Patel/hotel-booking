package com.rajat.hotelbooking.inventory.service;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.inventory.entity.LedgerEntry;
import com.rajat.hotelbooking.inventory.repository.LedgerEntryRepository;
import com.rajat.hotelbooking.property.entity.Variant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerRowCreator ledgerRowCreator;

    // must run before the booking transaction opens
    public void ensureLedgerRows(UUID variantId, LocalDate checkIn, LocalDate checkOut) {
        Set<LocalDate> existing = ledgerEntryRepository.findForStay(variantId, checkIn, checkOut).stream()
                .map(LedgerEntry::getNight)
                .collect(Collectors.toSet());
        for (LocalDate night : checkIn.datesUntil(checkOut).toList()) {
            if (existing.contains(night)) {
                continue;
            }
            try {
                ledgerRowCreator.insert(variantId, night);
            } catch (DataIntegrityViolationException alreadyCreated) {
                log.debug("ledger row for variant {} on {} created concurrently", variantId, night);
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // rows are locked in night order so two bookings over the same nights can't deadlock
    public void reserve(Variant variant, LocalDate checkIn, LocalDate checkOut, int rooms) {
        List<LedgerEntry> entries = lockStay(variant, checkIn, checkOut);
        for (LedgerEntry entry : entries) {
            if (!entry.canReserve(rooms)) {
                throw new ConflictException(variant.getRoomType().getName() + " / " + variant.getName()
                        + " is sold out on " + entry.getNight() + ": " + entry.available()
                        + " room(s) left, requested " + rooms);
            }
        }
        entries.forEach(entry -> entry.reserve(rooms));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Variant variant, LocalDate checkIn, LocalDate checkOut, int rooms) {
        lockStay(variant, checkIn, checkOut).forEach(entry -> entry.release(rooms));
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> availableRooms(Collection<Variant> variants, LocalDate checkIn, LocalDate checkOut) {
        Map<UUID, Integer> available = new HashMap<>();
        if (variants.isEmpty()) {
            return available;
        }
        long nights = checkIn.datesUntil(checkOut).count();
        Map<UUID, Variant> byId = variants.stream().collect(Collectors.toMap(Variant::getId, v -> v, (a, b) -> a));
        Map<UUID, List<LedgerEntry>> entriesByVariant = ledgerEntryRepository
                .findForVariantsAndStay(byId.keySet(), checkIn, checkOut).stream()
                .collect(Collectors.groupingBy(entry -> entry.getVariant().getId()));
        for (Variant variant : byId.values()) {
            if (!variant.isActive()) {
                available.put(variant.getId(), 0);
                continue;
            }
            List<LedgerEntry> entries = entriesByVariant.getOrDefault(variant.getId(), List.of());
            int free = entries.size() < nights ? variant.getRoomCount() : Integer.MAX_VALUE;
            for (LedgerEntry entry : entries) {
                free = Math.min(free, Math.max(0, variant.getRoomCount() - entry.getReservedCount()));
            }
            available.put(variant.getId(), free);
        }
        return available;
    }

    private List<LedgerEntry> lockStay(Variant variant, LocalDate checkIn, LocalDate checkOut) {
        List<LedgerEntry> entries = ledgerEntryRepository.lockForStay(variant.getId(), checkIn, checkOut);
        long nights = checkIn.datesUntil(checkOut).count();
        if (entries.size() != nights) {
            throw new IllegalStateException("ledger rows missing for variant " + variant.getId() + ": expected "
                    + nights + ", found " + entries.size() + "; call ensureLedgerRows first");
        }
        return entries;
    }
}
