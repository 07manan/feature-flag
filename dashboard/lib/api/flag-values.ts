import { fetchApi } from "./client";
import type {
    FlagValue,
    CreateFlagValueData,
    UpdateFlagValueData,
} from "@/lib/types";

export interface GetFlagValuesOptions {
    flagId?: string;
    environmentId?: string;
    search?: string;
}

export async function getFlagValues(
    options?: GetFlagValuesOptions
): Promise<FlagValue[]> {
    const params = new URLSearchParams();
    if (options?.flagId) params.append("flagId", options.flagId);
    if (options?.environmentId)
        params.append("environmentId", options.environmentId);
    if (options?.search) params.append("search", options.search);

    const query = params.toString();
    return fetchApi<FlagValue[]>(`/flag-values${query ? `?${query}` : ""}`);
}

export async function getFlagValue(id: string): Promise<FlagValue> {
    return fetchApi<FlagValue>(`/flag-values/${id}`);
}

export async function createFlagValue(
    data: CreateFlagValueData
): Promise<FlagValue> {
    return fetchApi<FlagValue>("/flag-values", {
        method: "POST",
        body: data,
    });
}

export async function updateFlagValue(
    id: string,
    data: UpdateFlagValueData
): Promise<FlagValue> {
    return fetchApi<FlagValue>(`/flag-values/${id}`, {
        method: "PUT",
        body: data,
    });
}

export async function deleteFlagValue(id: string): Promise<void> {
    return fetchApi<void>(`/flag-values/${id}`, {
        method: "DELETE",
    });
}
