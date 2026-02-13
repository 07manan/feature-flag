import { fetchApi } from "./client";
import type { User, UpdateUserData } from "@/lib/types";

export async function getUsers(search?: string): Promise<User[]> {
    const params = search ? `?search=${encodeURIComponent(search)}` : "";
    return fetchApi<User[]>(`/users${params}`);
}

export async function getUser(id: string): Promise<User> {
    return fetchApi<User>(`/users/${id}`);
}

export async function updateUser(id: string, data: UpdateUserData): Promise<User> {
    return fetchApi<User>(`/users/${id}`, {
        method: "PATCH",
        body: data,
    });
}

export async function deleteUser(id: string): Promise<void> {
    return fetchApi<void>(`/users/${id}`, {
        method: "DELETE",
    });
}
