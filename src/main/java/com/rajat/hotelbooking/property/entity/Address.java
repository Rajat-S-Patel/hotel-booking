package com.rajat.hotelbooking.property.entity;

import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.reference.entity.Locality;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.regex.Pattern;

@Embeddable
public record Address(
        @Column(name = "address_line", nullable = false, length = 255) String line,
        @Column(name = "pincode", nullable = false, length = 6) String pincode,
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "locality_id", nullable = false) Locality locality) {

    private static final Pattern PINCODE_PATTERN = Pattern.compile("[1-9][0-9]{5}");

    public Address {
        line = PreConditions.requireText(line, "address line");
        pincode = PreConditions.requireText(pincode, "pincode");
        PreConditions.require(PINCODE_PATTERN.matcher(pincode).matches(),
                "pincode must be 6 digits and not start with 0: " + pincode);
        PreConditions.requireNonNull(locality, "locality");
    }
}