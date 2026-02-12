package com.github._manan.featureflags.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EvaluationResult {
    private final String flagKey;
    private final Object value;
    private final FlagType type;
    private final boolean isDefault;
    private final String variantId;

    @JsonCreator
    public EvaluationResult(
            @JsonProperty("flagKey") String flagKey,
            @JsonProperty("value") Object value,
            @JsonProperty("type") FlagType type,
            @JsonProperty("isDefault") boolean isDefault,
            @JsonProperty("variantId") String variantId) {
        this.flagKey = flagKey;
        this.value = value;
        this.type = type;
        this.isDefault = isDefault;
        this.variantId = variantId;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public Object getValue() {
        return value;
    }

    public FlagType getType() {
        return type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getVariantId() {
        return variantId;
    }

    /**
     * Converts the value to a boolean.
     * 
     * @return the boolean value
     * @throws ClassCastException if the value cannot be converted to boolean
     */
    public boolean getBooleanValue() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new ClassCastException("Value is not a boolean: " + value);
    }

    public String getStringValue() {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Converts the value to an integer.
     * 
     * @return the integer value
     * @throws ClassCastException if the value cannot be converted to integer
     */
    public int getIntValue() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new ClassCastException("Value is not a number: " + value);
    }

    /**
     * Converts the value to a double.
     * 
     * @return the double value
     * @throws ClassCastException if the value cannot be converted to double
     */
    public double getDoubleValue() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new ClassCastException("Value is not a number: " + value);
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "flagKey='" + flagKey + '\'' +
                ", value=" + value +
                ", type=" + type +
                ", isDefault=" + isDefault +
                ", variantId='" + variantId + '\'' +
                '}';
    }
}
