import { fetchApi } from "./client";
import type { AuthResponse, LoginCredentials, RegisterData } from "@/lib/types";

export async function register(data: RegisterData): Promise<AuthResponse> {
  return fetchApi<AuthResponse>("/auth/register", {
    method: "POST",
    body: data,
    auth: false,
  });
}

export async function login(data: LoginCredentials): Promise<AuthResponse> {
  return fetchApi<AuthResponse>("/auth/login", {
    method: "POST",
    body: data,
    auth: false,
  });
}

export async function oauthLogin(provider: string, token: string): Promise<AuthResponse> {
  return fetchApi<AuthResponse>(`/auth/oauth2/${provider}`, {
    method: "POST",
    body: { token },
    auth: false,
  });
}
