package com.rajat.hotelbooking.property.entity;

import com.rajat.hotelbooking.common.entity.Money;
import com.rajat.hotelbooking.common.entity.VersionedEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.reference.entity.Amenity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "variant",
        uniqueConstraints = @UniqueConstraint(name = "uk_variant_room_type_name", columnNames = {"room_type_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Variant extends VersionedEntity {

    public static final String STANDARD_NAME = "Standard";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean standard;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "surcharge_amount", nullable = false, precision = 12, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "surcharge_currency", nullable = false, length = 3))
    private Money surcharge;

    @Column(nullable = false)
    private int roomCount;

    @Getter(AccessLevel.NONE)
    @ManyToMany
    @JoinTable(name = "variant_feature",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> extraFeatures = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    private Variant(RoomType roomType, String name, boolean standard, Money surcharge, int roomCount) {
        this.roomType = PreConditions.requireNonNull(roomType, "room type");
        this.name = PreConditions.requireText(name, "variant name");
        this.standard = standard;
        this.surcharge = PreConditions.requireNonNull(surcharge, "surcharge");
        PreConditions.require(surcharge.currency().equals(roomType.getBasePrice().currency()),
                "surcharge currency must match base price currency");
        changeRoomCount(roomCount);
        this.active = true;
    }

    static Variant standard(RoomType roomType, int roomCount) {
        return new Variant(roomType, STANDARD_NAME, true,
                Money.zero(roomType.getBasePrice().currency()), roomCount);
    }

    static Variant extra(RoomType roomType, String name, Money surcharge, int roomCount) {
        PreConditions.require(!STANDARD_NAME.equalsIgnoreCase(name.trim()),
                "only the automatic variant may be named " + STANDARD_NAME);
        return new Variant(roomType, name, false, surcharge, roomCount);
    }

    public Money pricePerNight() {
        return roomType.getBasePrice().plus(surcharge);
    }

    public Set<Amenity> allFeatures() {
        Set<Amenity> all = new HashSet<>(roomType.getBaseFeatures());
        all.addAll(extraFeatures);
        return Collections.unmodifiableSet(all);
    }

    public void addExtraFeature(Amenity feature) {
        PreConditions.requireNonNull(feature, "feature");
        PreConditions.require(!standard, "the Standard variant has no extra features");
        PreConditions.require(feature.isRoomFeature(),
                "property amenity cannot be a room feature: " + feature.getCode());
        extraFeatures.add(feature);
    }

    public void changeRoomCount(int newRoomCount) {
        PreConditions.require(newRoomCount >= 1, "room count must be at least 1: " + newRoomCount);
        this.roomCount = newRoomCount;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
