package com.rajat.hotelbooking.inventory.repository;

import com.rajat.hotelbooking.inventory.entity.LedgerEntry;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends Repository<LedgerEntry, UUID> {

    LedgerEntry saveAndFlush(LedgerEntry entry);

    @Query("""
            select l from LedgerEntry l
            where l.variant.id = :variantId and l.night >= :from and l.night < :to
            order by l.night asc
            """)
    List<LedgerEntry> findForStay(@Param("variantId") UUID variantId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
            select l from LedgerEntry l
            where l.variant.id = :variantId and l.night >= :from and l.night < :to
            order by l.night asc
            """)
    List<LedgerEntry> lockForStay(@Param("variantId") UUID variantId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Query("""
            select l from LedgerEntry l
            where l.variant.id in :variantIds and l.night >= :from and l.night < :to
            """)
    List<LedgerEntry> findForVariantsAndStay(@Param("variantIds") Collection<UUID> variantIds,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
