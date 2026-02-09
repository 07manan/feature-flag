export type FlagType = "STRING" | "BOOLEAN" | "NUMBER";

export interface Flag {
    id: string;
    key: string;
    name: string;
    description?: string;
    type: FlagType;
    defaultValue: string;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface CreateFlagData {
    key: string;
    name: string;
    description?: string;
    type: FlagType;
    defaultValue: string;
}

export interface UpdateFlagData {
    name?: string;
    description?: string;
    defaultValue?: string;
}
