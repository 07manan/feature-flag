import { z } from "zod";

export const createEnvironmentSchema = z.object({
    key: z
        .string()
        .min(1, "Key is required")
        .max(50, "Key must be less than 50 characters")
        .regex(
            /^[a-z0-9-]+$/,
            "Key must contain only lowercase letters, numbers, and hyphens"
        ),
    name: z
        .string()
        .min(1, "Name is required")
        .max(100, "Name must be less than 100 characters"),
    description: z
        .string()
        .max(500, "Description must be less than 500 characters")
        .optional()
        .or(z.literal("")),
});

export const updateEnvironmentSchema = z.object({
    name: z
        .string()
        .min(1, "Name is required")
        .max(100, "Name must be less than 100 characters"),
    description: z
        .string()
        .max(500, "Description must be less than 500 characters")
        .optional()
        .or(z.literal("")),
});

export type CreateEnvironmentFormData = z.infer<typeof createEnvironmentSchema>;
export type UpdateEnvironmentFormData = z.infer<typeof updateEnvironmentSchema>;
