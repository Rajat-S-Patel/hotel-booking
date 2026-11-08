package com.rajat.hotelbooking.property.entity;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.reference.entity.Amenity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Table(name = "room_type",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_type_property_name", columnNames = {"property_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomType extends VersionedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int maxOccupancy;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "base_price_amount", nullable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "base_price_currency", nullable = false, length = 3))
    private Money basePrice;

    @Getter(AccessLevel.NONE)
    @ManyToMany
    @JoinTable(name = "room_type_feature",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> baseFeatures = new HashSet<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "roomType", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Variant> variants = new ArrayList<>();

    public RoomType(Property property, String name, int maxOccupancy, Money basePrice, int standardRoomCount) {
        this.property = PreConditions.requireNonNull(property, "property");
        this.name = PreConditions.requireText(name, "room type name");
        PreConditions.require(maxOccupancy >= 1, "max occupancy must be at least 1: " + maxOccupancy);
        this.maxOccupancy = maxOccupancy;
        this.basePrice = PreConditions.requireNonNull(basePrice, "base price");
        PreConditions.require(!basePrice.isZero(), "base price must be greater than zero");
        variants.add(Variant.standard(this, standardRoomCount));
    }

    public Variant addVariant(String variantName, Money surcharge, int roomCount) {
        String trimmed = PreConditions.requireText(variantName, "variant name");
        PreConditions.require(variants.stream().noneMatch(v -> v.getName().equalsIgnoreCase(trimmed)),
                "variant already exists in room type " + name + ": " + trimmed);
        Variant variant = Variant.extra(this, trimmed, surcharge, roomCount);
        variants.add(variant);
        return variant;
    }

    public Variant standardVariant() {
        return variants.stream()
                .filter(Variant::isStandard)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("room type has no Standard variant: " + name));
    }

    public int totalRooms() {
        return variants.stream().filter(Variant::isActive).mapToInt(Variant::getRoomCount).sum();
    }

    public void addBaseFeature(Amenity feature) {
        PreConditions.requireNonNull(feature, "feature");
        PreConditions.require(feature.isRoomFeature(),
                "property amenity cannot be a room feature: " + feature.getCode());
        baseFeatures.add(feature);
    }

    public void removeBaseFeature(Amenity feature) {
        baseFeatures.remove(feature);
    }

    public void changeBasePrice(Money newBasePrice) {
        PreConditions.requireNonNull(newBasePrice, "base price");
        PreConditions.require(!newBasePrice.isZero(), "base price must be greater than zero");
        PreConditions.require(newBasePrice.currency().equals(basePrice.currency()),
                "base price currency cannot change: " + basePrice.currency());
        this.basePrice = newBasePrice;
    }

    public Set<Amenity> getBaseFeatures() {
        return Collections.unmodifiableSet(baseFeatures);
    }

    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }
}
