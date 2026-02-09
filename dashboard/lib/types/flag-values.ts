import type { FlagType } from "./flags";

export interface Variant {
    id?: string;
    value: string;
    percentage: number;
}

export interface FlagValue {
    id: string;
    flagId: string;
    flagKey: string;
    flagType: FlagType;
    environmentId: string;
    environmentKey: string;
    variants: Variant[];
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface CreateFlagValueData {
    flagId: string;
    environmentId: string;
    variants: { value: string; percentage: number }[];
}

export interface UpdateFlagValueData {
    flagId: string;
    environmentId: string;
    variants: { value: string; percentage: number }[];
}
