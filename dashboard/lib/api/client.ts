import config from "@/lib/config";
import { getToken } from "@/lib/auth/token";

export class ApiError extends Error {
    status: number;
    errors: Record<string, string> | null;

    constructor(message: string, status: number, errors: Record<string, string> | null = null) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.errors = errors;
    }
}

interface FetchApiOptions {
    method?: string;
    body?: unknown;
    headers?: Record<string, string>;
    auth?: boolean;
}

export async function fetchApi<T = unknown>(endpoint: string, options: FetchApiOptions = {}): Promise<T> {
    const { method = "GET", body, headers = {}, auth = true } = options;

    const url = `${config.apiUrl}${endpoint}`;

    const requestHeaders: Record<string, string> = {
        "Content-Type": "application/json",
        ...headers,
    };

    if (auth) {
        const token = getToken();
        if (token) {
            requestHeaders["Authorization"] = `Bearer ${token}`;
        }
    }

    const fetchOptions: RequestInit = {
        method,
        headers: requestHeaders,
    };

    if (body) {
        fetchOptions.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, fetchOptions);
        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const message = data?.message || data?.error || "An error occurred";
            const errors = data?.errors || null;
            throw new ApiError(message, response.status, errors);
        }

        return data as T;
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }

        throw new ApiError(
            (error as Error).message || "Network error. Please check your connection.",
            0
        );
    }
}

export default fetchApi;
