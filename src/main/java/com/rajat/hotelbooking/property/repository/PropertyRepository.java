package com.rajat.hotelbooking.property.repository;

import com.rajat.hotelbooking.property.entity.Owner;
import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends Repository<Property, UUID> {

    Property save(Property property);

    Optional<Property> findById(UUID id);

    List<Property> findByOwnerOrderByNameAsc(Owner owner);

    @Query("""
            select p from Property p
            join fetch p.address.locality l
            join fetch l.city c
            where p.active = true
              and c.id = :cityId
              and (:localityId is null or l.id = :localityId)
              and (:kind is null or p.kind = :kind)
              and (:minStars is null or p.starRating >= :minStars)
            """)
    List<Property> findForSearch(@Param("cityId") UUID cityId,
                                 @Param("localityId") UUID localityId,
                                 @Param("kind") PropertyKind kind,
                                 @Param("minStars") Integer minStars);
}
