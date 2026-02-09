import { fetchApi } from "./client";
import type { Flag, CreateFlagData, UpdateFlagData } from "@/lib/types";

export async function getFlags(search?: string): Promise<Flag[]> {
  const params = search ? `?search=${encodeURIComponent(search)}` : "";
  return fetchApi<Flag[]>(`/flags${params}`);
}

export async function getFlag(id: string): Promise<Flag> {
  return fetchApi<Flag>(`/flags/${id}`);
}

export async function createFlag(data: CreateFlagData): Promise<Flag> {
  return fetchApi<Flag>("/flags", {
    method: "POST",
    body: data,
  });
}

export async function updateFlag(id: string, data: UpdateFlagData): Promise<Flag> {
  return fetchApi<Flag>(`/flags/${id}`, {
    method: "PATCH",
    body: data,
  });
}

export async function deleteFlag(id: string): Promise<void> {
  return fetchApi<void>(`/flags/${id}`, {
    method: "DELETE",
  });
}
