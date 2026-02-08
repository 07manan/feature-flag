package com.github._manan.featureflags.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PercentageDistributionValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPercentageDistribution {

    String message() default "Invalid percentage distribution";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
