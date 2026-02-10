import { fetchApi } from "./client";
import type { Environment, CreateEnvironmentData, UpdateEnvironmentData } from "@/lib/types";

export async function getEnvironments(search?: string): Promise<Environment[]> {
    const params = search ? `?search=${encodeURIComponent(search)}` : "";
    return fetchApi<Environment[]>(`/environments${params}`);
}

export async function getEnvironment(id: string): Promise<Environment> {
    return fetchApi<Environment>(`/environments/${id}`);
}

export async function createEnvironment(data: CreateEnvironmentData): Promise<Environment> {
    return fetchApi<Environment>("/environments", {
        method: "POST",
        body: data,
    });
}

export async function updateEnvironment(id: string, data: UpdateEnvironmentData): Promise<Environment> {
    return fetchApi<Environment>(`/environments/${id}`, {
        method: "PATCH",
        body: data,
    });
}

export async function deleteEnvironment(id: string): Promise<void> {
    return fetchApi<void>(`/environments/${id}`, {
        method: "DELETE",
    });
}

export async function regenerateApiKey(id: string): Promise<Environment> {
    return fetchApi<Environment>(`/environments/${id}/api-key`, {
        method: "POST",
    });
}
