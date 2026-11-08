package com.rajat.hotelbooking.reference.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "locality", uniqueConstraints = @UniqueConstraint(name = "uk_locality_city_name", columnNames = {"city_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locality extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 100)
    private String name;

    public Locality(City city, String name) {
        this.city = PreConditions.requireNonNull(city, "city");
        this.name = PreConditions.requireText(name, "locality name");
    }
}
