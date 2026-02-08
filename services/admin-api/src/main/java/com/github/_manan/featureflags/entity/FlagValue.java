package com.github._manan.featureflags.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "flag_values",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_flag_values_flag_env_active",
            columnNames = {"flag_id", "environment_id", "is_active"}
        )
    },
    indexes = {
        @Index(name = "idx_flag_values_flag_active", columnList = "flag_id, is_active"),
        @Index(name = "idx_flag_values_env_active", columnList = "environment_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagValue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_id", nullable = false)
    private Flag flag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @ElementCollection
    @CollectionTable(
        name = "flag_value_variants",
        joinColumns = @JoinColumn(name = "flag_value_id")
    )
    @OrderColumn(name = "variant_order")
    @Builder.Default
    private List<FlagValueVariant> variants = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}
