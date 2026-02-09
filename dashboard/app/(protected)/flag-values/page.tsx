"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useDebounce } from "use-debounce";
import { Plus, Pencil, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getFlagValues } from "@/lib/api/flag-values";
import { getFlags } from "@/lib/api/flags";
import { getEnvironments } from "@/lib/api/environments";
import type { FlagValue, Flag, Environment, FlagType } from "@/lib/types";

import { DeleteFlagValueDialog } from "@/components/flag-values";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Combobox, type ComboboxOption } from "@/components/ui/combobox";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const TYPE_BADGE_COLORS: Record<FlagType, string> = {
    STRING: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
    BOOLEAN: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
    NUMBER: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300",
};

export default function FlagValuesPage() {
    const router = useRouter();
    const searchParams = useSearchParams();

    const [flagValues, setFlagValues] = useState<FlagValue[]>([]);
    const [flags, setFlags] = useState<Flag[]>([]);
    const [environments, setEnvironments] = useState<Environment[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoadingFilters, setIsLoadingFilters] = useState(true);

    // Get initial filter values from URL
    const [selectedFlagId, setSelectedFlagId] = useState<string | undefined>(
        searchParams.get("flagId") ?? undefined
    );
    const [selectedEnvironmentId, setSelectedEnvironmentId] = useState<
        string | undefined
    >(searchParams.get("environmentId") ?? undefined);

    const [debouncedFlagId] = useDebounce(selectedFlagId, 300);
    const [debouncedEnvironmentId] = useDebounce(selectedEnvironmentId, 300);

    // Fetch filter options
    useEffect(() => {
        async function loadFilters() {
            setIsLoadingFilters(true);
            try {
                const [flagsData, envsData] = await Promise.all([
                    getFlags(),
                    getEnvironments(),
                ]);
                setFlags(flagsData);
                setEnvironments(envsData);
            } catch (error) {
                toast.error("Failed to load filter options");
            } finally {
                setIsLoadingFilters(false);
            }
        }
        loadFilters();
    }, []);

    // Convert to combobox options
    const flagOptions: ComboboxOption[] = flags.map((flag) => ({
        value: flag.id,
        label: flag.name,
        description: flag.key,
    }));

    const environmentOptions: ComboboxOption[] = environments.map((env) => ({
        value: env.id,
        label: env.name,
        description: env.key,
    }));

    // Update URL when filters change
    useEffect(() => {
        const params = new URLSearchParams();
        if (debouncedFlagId) params.set("flagId", debouncedFlagId);
        if (debouncedEnvironmentId)
            params.set("environmentId", debouncedEnvironmentId);

        const queryString = params.toString();
        router.replace(`/flag-values${queryString ? `?${queryString}` : ""}`, {
            scroll: false,
        });
    }, [debouncedFlagId, debouncedEnvironmentId, router]);

    // Fetch flag values
    const fetchFlagValues = useCallback(
        async (flagId?: string, environmentId?: string) => {
            setIsLoading(true);
            try {
                const data = await getFlagValues({ flagId, environmentId });
                setFlagValues(data);
            } catch (error) {
                if (error instanceof ApiError) {
                    toast.error("Failed to load flag values", {
                        description: error.message,
                    });
                } else {
                    toast.error("Failed to load flag values", {
                        description: "An unexpected error occurred.",
                    });
                }
            } finally {
                setIsLoading(false);
            }
        },
        []
    );

    useEffect(() => {
        fetchFlagValues(debouncedFlagId, debouncedEnvironmentId);
    }, [debouncedFlagId, debouncedEnvironmentId, fetchFlagValues]);

    function handleFlagValueDeleted() {
        fetchFlagValues(debouncedFlagId, debouncedEnvironmentId);
    }

    function formatVariants(flagValue: FlagValue): string {
        return flagValue.variants
            .map((v) => `${v.value} (${v.percentage}%)`)
            .join(", ");
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">Flag Values</h1>
                <Link href="/flag-values/new">
                    <Button>
                        <Plus className="mr-2 h-4 w-4" />
                        Add Flag Value
                    </Button>
                </Link>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Flag Values</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="mb-4 grid gap-4 md:grid-cols-2">
                        <div>
                            <label className="text-sm font-medium mb-2 block">
                                Filter by Flag
                            </label>
                            <Combobox
                                options={flagOptions}
                                value={selectedFlagId}
                                onValueChange={setSelectedFlagId}
                                placeholder="All flags"
                                searchPlaceholder="Search flags..."
                                emptyText="No flags found."
                                disabled={isLoadingFilters}
                            />
                        </div>
                        <div>
                            <label className="text-sm font-medium mb-2 block">
                                Filter by Environment
                            </label>
                            <Combobox
                                options={environmentOptions}
                                value={selectedEnvironmentId}
                                onValueChange={setSelectedEnvironmentId}
                                placeholder="All environments"
                                searchPlaceholder="Search environments..."
                                emptyText="No environments found."
                                disabled={isLoadingFilters}
                            />
                        </div>
                    </div>

                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                        </div>
                    ) : flagValues.length === 0 ? (
                        <div className="text-center py-8">
                            <p className="text-muted-foreground">
                                {selectedFlagId || selectedEnvironmentId
                                    ? "No flag values found matching your filters."
                                    : "No flag values yet. Create your first flag value to get started."}
                            </p>
                        </div>
                    ) : (
                        <div className="rounded-md border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="pl-4">Flag</TableHead>
                                        <TableHead>Environment</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead>Variants</TableHead>
                                        <TableHead className="text-right pr-4">
                                            Actions
                                        </TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {flagValues.map((flagValue) => (
                                        <TableRow key={flagValue.id}>
                                            <TableCell className="pl-4">
                                                <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono">
                                                    {flagValue.flagKey}
                                                </code>
                                            </TableCell>
                                            <TableCell>
                                                <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono">
                                                    {flagValue.environmentKey}
                                                </code>
                                            </TableCell>
                                            <TableCell>
                                                <Badge
                                                    variant="secondary"
                                                    className={
                                                        TYPE_BADGE_COLORS[flagValue.flagType]
                                                    }
                                                >
                                                    {flagValue.flagType}
                                                </Badge>
                                            </TableCell>
                                            <TableCell className="font-mono text-sm max-w-[300px] truncate">
                                                {formatVariants(flagValue)}
                                            </TableCell>
                                            <TableCell className="text-right pr-4">
                                                <div className="flex items-center justify-end gap-2">
                                                    <Link
                                                        href={`/flag-values/${flagValue.id}/edit`}
                                                    >
                                                        <Button variant="outline" size="sm">
                                                            <Pencil className="h-4 w-4" />
                                                        </Button>
                                                    </Link>
                                                    <DeleteFlagValueDialog
                                                        flagValue={flagValue}
                                                        onDeleted={handleFlagValueDeleted}
                                                    />
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
