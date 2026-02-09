package com.github._manan.featureflags.dto;

import com.github._manan.featureflags.entity.FlagType;
import com.github._manan.featureflags.entity.FlagValue;
import com.github._manan.featureflags.validation.ValidPercentageDistribution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagValueDto {

    private UUID id;

    @NotNull(message = "Flag ID is required")
    private UUID flagId;

    private String flagKey;

    private FlagType flagType;

    @NotNull(message = "Environment ID is required")
    private UUID environmentId;

    private String environmentKey;

    @NotNull(message = "Variants are required")
    @Valid
    @ValidPercentageDistribution
    private List<FlagValueVariantDto> variants;

    private Boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;

    public static FlagValueDto from(FlagValue flagValue) {
        return FlagValueDto.builder()
                .id(flagValue.getId())
                .flagId(flagValue.getFlag().getId())
                .flagKey(flagValue.getFlag().getKey())
                .flagType(flagValue.getFlag().getType())
                .environmentId(flagValue.getEnvironment().getId())
                .environmentKey(flagValue.getEnvironment().getKey())
                .variants(flagValue.getVariants().stream()
                        .map(FlagValueVariantDto::from)
                        .toList())
                .isActive(flagValue.getIsActive())
                .createdAt(flagValue.getCreatedAt())
                .updatedAt(flagValue.getUpdatedAt())
                .build();
    }
}
