import { z } from "zod";
import { FLAG_TYPES } from "./flags";

const variantSchema = z.object({
    value: z
        .string()
        .min(1, "Value is required")
        .max(500, "Value must be less than 500 characters"),
    percentage: z
        .number({ message: "Percentage must be a number" })
        .int({ message: "Percentage must be a whole number" })
        .min(0, { message: "Percentage must be at least 0" })
        .max(100, { message: "Percentage must be at most 100" }),
});

export const createFlagValueSchema = z
    .object({
        flagId: z.string().min(1, "Please select a flag"),
        environmentId: z.string().min(1, "Please select an environment"),
        flagType: z.enum(FLAG_TYPES).optional(),
        variants: z
            .array(variantSchema)
            .min(1, "At least one variant is required"),
    })
    .superRefine((data, ctx) => {
        // Validate percentages sum to 100
        const sum = data.variants.reduce((s, v) => s + v.percentage, 0);
        if (sum !== 100) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: `Percentages must sum to 100 (currently ${sum})`,
                path: ["variants"],
            });
        }

        // Validate values match flag type
        if (data.flagType) {
            data.variants.forEach((variant, index) => {
                if (data.flagType === "BOOLEAN") {
                    const lowerValue = variant.value.toLowerCase();
                    if (lowerValue !== "true" && lowerValue !== "false") {
                        ctx.addIssue({
                            code: z.ZodIssueCode.custom,
                            message: "Boolean value must be 'true' or 'false'",
                            path: ["variants", index, "value"],
                        });
                    }
                }

                if (data.flagType === "NUMBER") {
                    const numValue = Number(variant.value);
                    if (isNaN(numValue)) {
                        ctx.addIssue({
                            code: z.ZodIssueCode.custom,
                            message: "Value must be a valid number",
                            path: ["variants", index, "value"],
                        });
                    }
                }
            });
        }
    });

export const updateFlagValueSchema = z
    .object({
        flagId: z.string().min(1, "Flag ID is required"),
        environmentId: z.string().min(1, "Environment ID is required"),
        flagType: z.enum(FLAG_TYPES).optional(),
        variants: z
            .array(variantSchema)
            .min(1, "At least one variant is required"),
    })
    .superRefine((data, ctx) => {
        // Validate percentages sum to 100
        const sum = data.variants.reduce((s, v) => s + v.percentage, 0);
        if (sum !== 100) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: `Percentages must sum to 100 (currently ${sum})`,
                path: ["variants"],
            });
        }

        // Validate values match flag type
        if (data.flagType) {
            data.variants.forEach((variant, index) => {
                if (data.flagType === "BOOLEAN") {
                    const lowerValue = variant.value.toLowerCase();
                    if (lowerValue !== "true" && lowerValue !== "false") {
                        ctx.addIssue({
                            code: z.ZodIssueCode.custom,
                            message: "Boolean value must be 'true' or 'false'",
                            path: ["variants", index, "value"],
                        });
                    }
                }

                if (data.flagType === "NUMBER") {
                    const numValue = Number(variant.value);
                    if (isNaN(numValue)) {
                        ctx.addIssue({
                            code: z.ZodIssueCode.custom,
                            message: "Value must be a valid number",
                            path: ["variants", index, "value"],
                        });
                    }
                }
            });
        }
    });

export type CreateFlagValueFormData = z.infer<typeof createFlagValueSchema>;
export type UpdateFlagValueFormData = z.infer<typeof updateFlagValueSchema>;
