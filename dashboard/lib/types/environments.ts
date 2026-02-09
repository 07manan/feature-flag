export interface Environment {
    id: string;
    key: string;
    name: string;
    description?: string;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface CreateEnvironmentData {
    key: string;
    name: string;
    description?: string;
}

export interface UpdateEnvironmentData {
    name?: string;
    description?: string;
}
