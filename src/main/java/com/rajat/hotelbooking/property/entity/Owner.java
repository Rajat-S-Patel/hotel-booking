package com.rajat.hotelbooking.property.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.property.entity.enums.OwnerKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Locale;
import java.util.regex.Pattern;

@Entity
@Getter
@Table(name = "owner", uniqueConstraints = @UniqueConstraint(name = "uk_contact_email", columnNames = "contacta_email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Owner extends BaseEntity {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{10,15}");

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OwnerKind kind;

    @Column(nullable = false, length = 200)
    private String contactEmail;
    @Column(nullable = false, length = 16)
    private String contactPhone;

    public Owner(String name, OwnerKind kind, String contactEmail, String contactPhone) {
        this.name = PreConditions.requireText(name, "owner name");
        this.kind = PreConditions.requireNonNull(kind, "owner kind");
        this.contactEmail = normaliseEmail(contactEmail);
        this.contactPhone = normalisePhone(contactPhone);
    }

    private static String normaliseEmail(String email) {
        String trimmed = PreConditions.requireText(email, "contact email").toLowerCase(Locale.ROOT);
        PreConditions.require(EMAIL_PATTERN.matcher(trimmed).matches(), "invalid contact email: " + trimmed);
        return trimmed;
    }

    private static String normalisePhone(String phone) {
        String compact = PreConditions.requireText(phone, "contact phone").replaceAll("[\\s-]", "");
        PreConditions.require(PHONE_PATTERN.matcher(compact).matches(), "invalid contact phone: " + phone);
        return compact;
    }

}
