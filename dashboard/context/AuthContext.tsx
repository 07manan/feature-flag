"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import * as authApi from "@/lib/api/auth";
import { getUser as fetchCurrentUser } from "@/lib/api/users";
import {
    setToken,
    clearToken,
    initializeToken,
} from "@/lib/auth/token";
import type { User, LoginCredentials, RegisterData } from "@/lib/types";

interface AuthContextValue {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (credentials: LoginCredentials) => Promise<User>;
    register: (data: RegisterData) => Promise<User>;
    oauthLogin: (provider: string, token: string) => Promise<User>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface AuthProviderProps {
    children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
    const router = useRouter();
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        async function validateUser() {
            const { token, user: storedUser } = initializeToken();
            if (token && storedUser) {
                setUser(storedUser);
                try {
                    const freshUser = await fetchCurrentUser(storedUser.id);
                    setUser(freshUser);
                    setToken(token, freshUser);
                } catch {
                    clearToken();
                    setUser(null);
                }
            }
            setIsLoading(false);
        }

        validateUser();
    }, []);

    const login = useCallback(async (credentials: LoginCredentials) => {
        const response = await authApi.login(credentials);
        setToken(response.token, response.user);
        setUser(response.user);
        return response.user;
    }, []);

    const register = useCallback(async (data: RegisterData) => {
        const response = await authApi.register(data);
        setToken(response.token, response.user);
        setUser(response.user);
        return response.user;
    }, []);

    const oauthLogin = useCallback(async (provider: string, token: string) => {
        const response = await authApi.oauthLogin(provider, token);
        setToken(response.token, response.user);
        setUser(response.user);
        return response.user;
    }, []);

    const logout = useCallback(() => {
        clearToken();
        setUser(null);
        router.push("/login");
    }, [router]);

    const value: AuthContextValue = {
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        oauthLogin,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}

export default AuthContext;
