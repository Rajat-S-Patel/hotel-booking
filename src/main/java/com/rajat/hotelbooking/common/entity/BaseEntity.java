package com.rajat.hotelbooking.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Override
    public final boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null || effectiveClass(this) != effectiveClass(obj))
            return false;
        UUID id = getId();
        return id != null && id.equals(((BaseEntity) obj).getId());
    }
    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }
    private static Class<?> effectiveClass(Object entity) {
        return entity instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : entity.getClass();
    }
}
