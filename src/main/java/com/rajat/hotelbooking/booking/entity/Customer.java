package com.rajat.hotelbooking.booking.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.ContactDetails;
import com.rajat.hotelbooking.common.util.PreConditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "customer",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 16)
    private String phone;

    public Customer(String name, String email, String phone) {
        this.name = PreConditions.requireText(name, "customer name");
        this.email = ContactDetails.normaliseEmail(email, "customer email");
        this.phone = ContactDetails.normalisePhone(phone, "customer phone");
    }
}
