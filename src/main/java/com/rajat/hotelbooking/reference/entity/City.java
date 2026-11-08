package com.rajat.hotelbooking.reference.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "city", uniqueConstraints = @UniqueConstraint(name = "uk_city_name", columnNames = "name"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class City extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    public City(String name) {
        this.name = PreConditions.requireText(name, "city name");
    }
}
