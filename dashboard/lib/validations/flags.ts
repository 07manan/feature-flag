import { z } from "zod";

export const FLAG_TYPES = ["STRING", "BOOLEAN", "NUMBER"] as const;

export const createFlagSchema = z
  .object({
    key: z
      .string()
      .min(1, "Key is required")
      .max(100, "Key must be less than 100 characters")
      .regex(
        /^[a-z0-9-]+$/,
        "Key must contain only lowercase letters, numbers, and hyphens"
      ),
    name: z
      .string()
      .min(1, "Name is required")
      .max(200, "Name must be less than 200 characters"),
    description: z
      .string()
      .max(1000, "Description must be less than 1000 characters")
      .optional()
      .or(z.literal("")),
    type: z.enum(FLAG_TYPES, {
      message: "Type is required",
    }),
    defaultValue: z
      .string()
      .min(1, "Default value is required")
      .max(500, "Default value must be less than 500 characters"),
  })
  .superRefine((data, ctx) => {
    const { type, defaultValue } = data;

    if (type === "BOOLEAN") {
      const lowerValue = defaultValue.toLowerCase();
      if (lowerValue !== "true" && lowerValue !== "false") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Boolean value must be 'true' or 'false'",
          path: ["defaultValue"],
        });
      }
    }

    if (type === "NUMBER") {
      const numValue = Number(defaultValue);
      if (isNaN(numValue)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Default value must be a valid number",
          path: ["defaultValue"],
        });
      }
    }
  });

export const updateFlagSchema = z
  .object({
    name: z
      .string()
      .min(1, "Name is required")
      .max(200, "Name must be less than 200 characters"),
    description: z
      .string()
      .max(1000, "Description must be less than 1000 characters")
      .optional()
      .or(z.literal("")),
    defaultValue: z
      .string()
      .min(1, "Default value is required")
      .max(500, "Default value must be less than 500 characters"),
    // Type is included for validation purposes but won't be sent to API
    type: z.enum(FLAG_TYPES),
  })
  .superRefine((data, ctx) => {
    const { type, defaultValue } = data;

    if (type === "BOOLEAN") {
      const lowerValue = defaultValue.toLowerCase();
      if (lowerValue !== "true" && lowerValue !== "false") {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Boolean value must be 'true' or 'false'",
          path: ["defaultValue"],
        });
      }
    }

    if (type === "NUMBER") {
      const numValue = Number(defaultValue);
      if (isNaN(numValue)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Default value must be a valid number",
          path: ["defaultValue"],
        });
      }
    }
  });

export type CreateFlagFormData = z.infer<typeof createFlagSchema>;
export type UpdateFlagFormData = z.infer<typeof updateFlagSchema>;
