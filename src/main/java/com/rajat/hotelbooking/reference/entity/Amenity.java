package com.rajat.hotelbooking.reference.entity;

import com.rajat.hotelbooking.common.entity.BaseEntity;
import com.rajat.hotelbooking.common.util.PreConditions;
import com.rajat.hotelbooking.reference.entity.enums.AmenityScope;
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

import java.util.regex.Pattern;

@Entity
@Getter
@Table(name = "amenity", uniqueConstraints = @UniqueConstraint(name = "uk_amenity_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Amenity extends BaseEntity {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AmenityScope scope;

    public boolean isRoomFeature() {
        return scope == AmenityScope.ROOM;
    }

    public Amenity(String code, String displayName, AmenityScope scope) {
        String trimmedCode = PreConditions.requireText(code, "amenity code");
        PreConditions.require(CODE_PATTERN.matcher(trimmedCode).matches(), "amenity code must be UPPER_SNAKE_CASE: "+trimmedCode);
        this.code = trimmedCode;
        this.displayName = PreConditions.requireText(displayName, "amenity display name");
        this.scope = PreConditions.requireNonNull(scope, "amenity scope");
    }
}
