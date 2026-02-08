package com.github._manan.featureflags.validation;

import com.github._manan.featureflags.dto.FlagValueVariantDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class PercentageDistributionValidator 
        implements ConstraintValidator<ValidPercentageDistribution, List<FlagValueVariantDto>> {

    @Override
    public boolean isValid(List<FlagValueVariantDto> variants, ConstraintValidatorContext context) {
        if (variants == null || variants.isEmpty()) {
            setMessage(context, "At least one variant is required");
            return false;
        }

        int totalPercentage = 0;
        for (int i = 0; i < variants.size(); i++) {
            FlagValueVariantDto variant = variants.get(i);
            
            if (variant.getValue() == null || variant.getValue().isBlank()) {
                setMessage(context, "Variant at index " + i + " has blank value");
                return false;
            }

            if (variant.getPercentage() == null) {
                setMessage(context, "Variant at index " + i + " has null percentage");
                return false;
            }

            if (variant.getPercentage() < 0 || variant.getPercentage() > 100) {
                setMessage(context, "Variant at index " + i + " has invalid percentage: " 
                        + variant.getPercentage() + " (must be 0-100)");
                return false;
            }

            totalPercentage += variant.getPercentage();
        }

        if (totalPercentage != 100) {
            setMessage(context, "Percentages must sum to 100, got: " + totalPercentage);
            return false;
        }

        return true;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
