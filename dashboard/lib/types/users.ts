export type { User } from "./auth";

export type UserRole = "ADMIN" | "GUEST";

export interface UpdateUserData {
    firstName?: string;
    lastName?: string;
    role?: UserRole;
    enabled?: boolean;
}
