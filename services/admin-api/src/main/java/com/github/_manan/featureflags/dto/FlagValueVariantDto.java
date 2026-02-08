package com.github._manan.featureflags.dto;

import com.github._manan.featureflags.entity.FlagValueVariant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagValueVariantDto {

    private UUID id;

    @NotBlank(message = "Variant value is required")
    private String value;

    @NotNull(message = "Percentage is required")
    @Min(value = 0, message = "Percentage must be at least 0")
    @Max(value = 100, message = "Percentage must be at most 100")
    private Integer percentage;

    public static FlagValueVariantDto from(FlagValueVariant variant) {
        return FlagValueVariantDto.builder()
                .id(variant.getId())
                .value(variant.getValue())
                .percentage(variant.getPercentage())
                .build();
    }

    public FlagValueVariant toEntity() {
        return FlagValueVariant.builder()
                .value(this.value)
                .percentage(this.percentage)
                .build();
    }
}
