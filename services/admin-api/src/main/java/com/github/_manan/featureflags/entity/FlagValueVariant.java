package com.github._manan.featureflags.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagValueVariant {

    @Column(name = "id", nullable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "value", nullable = false, length = 500)
    private String value;

    @Column(name = "percentage", nullable = false)
    private Integer percentage;
}
