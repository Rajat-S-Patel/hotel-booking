package com.rajat.hotelbooking.common.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VersionedEntity extends BaseEntity {
    @Version
    private Long version;
}
