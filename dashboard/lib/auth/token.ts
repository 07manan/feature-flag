import type { User } from "@/lib/types";

const TOKEN_KEY = "ff_auth_token";
const USER_KEY = "ff_auth_user";

let memoryToken: string | null = null;
let memoryUser: User | null = null;

const isBrowser = typeof window !== "undefined";

export function getToken(): string | null {
    return memoryToken;
}

export function getUser(): User | null {
    return memoryUser;
}

export function setToken(token: string, user: User): void {
    memoryToken = token;
    memoryUser = user;

    if (isBrowser) {
        try {
            localStorage.setItem(TOKEN_KEY, token);
            localStorage.setItem(USER_KEY, JSON.stringify(user));
        } catch (error) {
            console.warn("Failed to persist auth to localStorage:", error);
        }
    }
}

export function clearToken(): void {
    memoryToken = null;
    memoryUser = null;

    if (isBrowser) {
        try {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
        } catch (error) {
            console.warn("Failed to clear auth from localStorage:", error);
        }
    }
}

export function initializeToken(): { token: string | null; user: User | null } {
    if (!isBrowser) {
        return { token: null, user: null };
    }

    try {
        const token = localStorage.getItem(TOKEN_KEY);
        const userStr = localStorage.getItem(USER_KEY);
        const user = userStr ? (JSON.parse(userStr) as User) : null;

        if (token && user) {
            memoryToken = token;
            memoryUser = user;
            return { token, user };
        }
    } catch (error) {
        console.warn("Failed to restore auth from localStorage:", error);
        clearToken();
    }

    return { token: null, user: null };
}

export function isAuthenticated(): boolean {
    return !!memoryToken;
}
