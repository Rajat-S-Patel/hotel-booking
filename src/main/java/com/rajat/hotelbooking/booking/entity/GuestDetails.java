package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.common.util.ContactDetails;
import com.rajat.hotelbooking.common.util.PreConditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GuestDetails(
        @Column(name = "guest_name", length = 150) String name,
        @Column(name = "guest_phone", length = 16) String phone) {

    public GuestDetails {
        name = PreConditions.requireText(name, "guest name");
        phone = ContactDetails.normalisePhone(phone, "guest phone");
    }
}
