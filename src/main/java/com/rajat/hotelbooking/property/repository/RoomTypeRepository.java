package com.rajat.hotelbooking.property.repository;

import com.rajat.hotelbooking.property.entity.Property;
import com.rajat.hotelbooking.property.entity.RoomType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoomTypeRepository extends Repository<RoomType, UUID> {

    RoomType save(RoomType roomType);

    List<RoomType> findByPropertyOrderByNameAsc(Property property);

    @Query("""
            select distinct rt from RoomType rt
            left join fetch rt.variants
            where rt.property in :properties
            """)
    List<RoomType> findWithVariantsByPropertyIn(@Param("properties") Collection<Property> properties);
}
