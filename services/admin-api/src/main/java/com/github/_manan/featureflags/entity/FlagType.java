package com.github._manan.featureflags.entity;

/**
 * Enumeration of supported flag value types.
 * Determines how the flag's default value and overrides are validated and interpreted.
 */
public enum FlagType {
    /**
     * String flag type - accepts any string value
     */
    STRING,

    /**
     * Boolean flag type - accepts "true" or "false" (case-insensitive)
     */
    BOOLEAN,

    /**
     * Number flag type - accepts valid numeric values (integers or decimals)
     */
    NUMBER
}
