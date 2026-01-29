package com.github._manan.featureflags.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity representing a feature flag definition.
 * A flag defines the metadata and default value for a feature toggle.
 * Environment-specific overrides are managed separately.
 */
@Entity
@Table(
    name = "flags",
    indexes = {
        @Index(name = "idx_flags_key_active", columnList = "key, is_active"),
        @Index(name = "idx_flags_is_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flag extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique programmatic key for SDK usage.
     * Immutable after creation. Must be lowercase with hyphens only.
     * Uniqueness is enforced at the application level among active records only.
     */
    @Column(nullable = false, length = 100)
    private String key;

    /**
     * Human-readable display name for the flag.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Optional description explaining the flag's purpose and usage.
     */
    @Column(length = 1000)
    private String description;

    /**
     * The data type of this flag's value.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlagType type;

    /**
     * The default value for this flag when no environment-specific override exists.
     * Stored as a string and validated against the flag's type.
     */
    @Column(name = "default_value", nullable = false, length = 500)
    private String defaultValue;

    /**
     * Soft delete flag. When false, the flag is considered deleted.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}
