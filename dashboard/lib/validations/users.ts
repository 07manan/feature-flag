import { z } from "zod";

export const USER_ROLES = ["ADMIN", "GUEST"] as const;

export const updateUserSchema = z.object({
    firstName: z
        .string()
        .min(1, "First name is required")
        .max(100, "First name must be less than 100 characters"),
    lastName: z
        .string()
        .min(1, "Last name is required")
        .max(100, "Last name must be less than 100 characters"),
    role: z.enum(USER_ROLES, {
        message: "Role is required",
    }),
    enabled: z.boolean(),
});

export type UpdateUserFormData = z.infer<typeof updateUserSchema>;
