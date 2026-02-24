"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Plus, Trash2 } from "lucide-react";

import { ApiError } from "@/lib/api/client";
import { getFlags } from "@/lib/api/flags";
import { getEnvironments } from "@/lib/api/environments";
import { createFlagValue, updateFlagValue } from "@/lib/api/flag-values";
import {
    createFlagValueSchema,
    updateFlagValueSchema,
    type CreateFlagValueFormData,
    type UpdateFlagValueFormData,
} from "@/lib/validations/flag-values";
import type { Flag, Environment, FlagValue, FlagType } from "@/lib/types";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Combobox, type ComboboxOption } from "@/components/ui/combobox";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface FlagValueFormProps {
    mode: "create" | "edit";
    initialData?: FlagValue;
}

function getDefaultValueForType(type: FlagType): string {
    switch (type) {
        case "BOOLEAN":
            return "true";
        case "NUMBER":
            return "0";
        case "STRING":
        default:
            return "string";
    }
}

export function FlagValueForm({ mode, initialData }: FlagValueFormProps) {
    const router = useRouter();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [flags, setFlags] = useState<Flag[]>([]);
    const [environments, setEnvironments] = useState<Environment[]>([]);
    const [isLoadingFlags, setIsLoadingFlags] = useState(true);
    const [isLoadingEnvironments, setIsLoadingEnvironments] = useState(true);

    const isEditMode = mode === "edit";
    const schema = isEditMode ? updateFlagValueSchema : createFlagValueSchema;

    const form = useForm<CreateFlagValueFormData | UpdateFlagValueFormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            flagId: initialData?.flagId ?? "",
            environmentId: initialData?.environmentId ?? "",
            flagType: initialData?.flagType,
            variants: initialData?.variants?.map((v) => ({
                value: v.value,
                percentage: v.percentage,
            })) ?? [{ value: "true", percentage: 100 }],
        },
    });

    const { fields, append, remove } = useFieldArray({
        control: form.control,
        name: "variants",
    });

    const watchedFlagId = form.watch("flagId");
    const selectedFlag = flags.find((f) => f.id === watchedFlagId);
    const flagType = selectedFlag?.type ?? initialData?.flagType;

    // Update flagType in form when flag changes
    useEffect(() => {
        if (flagType) {
            form.setValue("flagType", flagType);
        }
    }, [flagType, form]);

    // Fetch flags and environments
    const fetchFlags = useCallback(async () => {
        setIsLoadingFlags(true);
        try {
            const data = await getFlags();
            setFlags(data);
        } catch (error) {
            toast.error("Failed to load flags");
        } finally {
            setIsLoadingFlags(false);
        }
    }, []);

    const fetchEnvironments = useCallback(async () => {
        setIsLoadingEnvironments(true);
        try {
            const data = await getEnvironments();
            setEnvironments(data);
        } catch (error) {
            toast.error("Failed to load environments");
        } finally {
            setIsLoadingEnvironments(false);
        }
    }, []);

    useEffect(() => {
        fetchFlags();
        fetchEnvironments();
    }, [fetchFlags, fetchEnvironments]);

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

    // Handle adding a new variant
    function handleAddVariant() {
        const defaultValue = flagType ? getDefaultValueForType(flagType) : "string";
        append({ value: defaultValue, percentage: 0 });
    }

    // Handle flag change - reset variants with default values
    function handleFlagChange(flagId: string | undefined) {
        form.setValue("flagId", flagId ?? "");
        if (flagId && !isEditMode) {
            const flag = flags.find((f) => f.id === flagId);
            if (flag) {
                const defaultValue = getDefaultValueForType(flag.type);
                form.setValue("variants", [{ value: defaultValue, percentage: 100 }]);
            }
        }
    }

    async function onSubmit(data: CreateFlagValueFormData | UpdateFlagValueFormData) {
        setIsSubmitting(true);
        try {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { flagType: _, ...submitData } = data;

            if (isEditMode && initialData) {
                await updateFlagValue(initialData.id, submitData);
                toast.success("Flag value updated!", {
                    description: "The flag value has been updated successfully.",
                });
            } else {
                await createFlagValue(submitData);
                toast.success("Flag value created!", {
                    description: "The flag value has been created successfully.",
                });
            }
            router.push("/flag-values");
            router.refresh();
        } catch (error) {
            if (error instanceof ApiError) {
                if (error.errors) {
                    Object.entries(error.errors).forEach(([field, message]) => {
                        form.setError(field as keyof CreateFlagValueFormData, { message });
                    });
                }
                toast.error(
                    isEditMode ? "Failed to update flag value" : "Failed to create flag value",
                    {
                        description: error.message,
                    }
                );
            } else {
                toast.error("An unexpected error occurred", {
                    description: "Please try again later.",
                });
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    // Render value input based on flag type
    function renderValueInput(
        field: { value: string; onChange: (value: string) => void },
        index: number
    ) {
        switch (flagType) {
            case "BOOLEAN":
                return (
                    <Select
                        onValueChange={field.onChange}
                        value={field.value}
                        disabled={isSubmitting}
                    >
                        <SelectTrigger>
                            <SelectValue placeholder="Select value" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="true">true</SelectItem>
                            <SelectItem value="false">false</SelectItem>
                        </SelectContent>
                    </Select>
                );
            case "NUMBER":
                return (
                    <Input
                        type="number"
                        placeholder="0"
                        value={field.value}
                        onChange={(e) => field.onChange(e.target.value)}
                        disabled={isSubmitting}
                    />
                );
            case "STRING":
            default:
                return (
                    <Input
                        type="text"
                        placeholder="Enter value"
                        value={field.value}
                        onChange={(e) => field.onChange(e.target.value)}
                        disabled={isSubmitting}
                    />
                );
        }
    }

    const percentageSum = form
        .watch("variants")
        .reduce((sum, v) => sum + (v.percentage || 0), 0);

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                <div className="grid gap-6 md:grid-cols-2">
                    <FormField
                        control={form.control}
                        name="flagId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Flag</FormLabel>
                                <FormControl>
                                    <Combobox
                                        options={flagOptions}
                                        value={field.value}
                                        onValueChange={handleFlagChange}
                                        placeholder="Select a flag..."
                                        searchPlaceholder="Search flags..."
                                        emptyText="No flags found."
                                        disabled={isEditMode || isSubmitting || isLoadingFlags}
                                    />
                                </FormControl>
                                <FormDescription>
                                    The feature flag to configure.
                                </FormDescription>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="environmentId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Environment</FormLabel>
                                <FormControl>
                                    <Combobox
                                        options={environmentOptions}
                                        value={field.value}
                                        onValueChange={(value) =>
                                            field.onChange(value ?? "")
                                        }
                                        placeholder="Select an environment..."
                                        searchPlaceholder="Search environments..."
                                        emptyText="No environments found."
                                        disabled={
                                            isEditMode || isSubmitting || isLoadingEnvironments
                                        }
                                    />
                                </FormControl>
                                <FormDescription>
                                    The target environment for this configuration.
                                </FormDescription>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>

                {selectedFlag && (
                    <div className="text-sm text-muted-foreground">
                        Flag type: <span className="font-medium">{selectedFlag.type}</span>
                    </div>
                )}

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
                        <div>
                            <CardTitle className="text-base">Variants</CardTitle>
                            <p className="text-sm text-muted-foreground mt-1">
                                Define the values and their distribution percentages.
                            </p>
                        </div>
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={handleAddVariant}
                            disabled={isSubmitting || !watchedFlagId}
                        >
                            <Plus className="mr-2 h-4 w-4" />
                            Add Variant
                        </Button>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {fields.length === 0 && (
                            <p className="text-sm text-muted-foreground text-center py-4">
                                No variants. Add at least one variant.
                            </p>
                        )}

                        {fields.map((field, index) => (
                            <div
                                key={field.id}
                                className="flex items-start gap-4 p-4 border rounded-lg"
                            >
                                <div className="flex-1 grid gap-4 md:grid-cols-2">
                                    <FormField
                                        control={form.control}
                                        name={`variants.${index}.value`}
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>Value</FormLabel>
                                                <FormControl>
                                                    {renderValueInput(field, index)}
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name={`variants.${index}.percentage`}
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>Percentage</FormLabel>
                                                <FormControl>
                                                    <div className="relative">
                                                        <Input
                                                            type="number"
                                                            min={0}
                                                            max={100}
                                                            placeholder="0"
                                                            value={field.value}
                                                            onChange={(e) =>
                                                                field.onChange(
                                                                    parseInt(e.target.value) || 0
                                                                )
                                                            }
                                                            disabled={isSubmitting}
                                                            className="pr-8"
                                                        />
                                                        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                                                            %
                                                        </span>
                                                    </div>
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                </div>

                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    onClick={() => remove(index)}
                                    disabled={isSubmitting || fields.length <= 1}
                                    className="mt-8"
                                >
                                    <Trash2 className="h-4 w-4 text-destructive" />
                                </Button>
                            </div>
                        ))}

                        <div
                            className={`text-sm font-medium ${
                                percentageSum === 100
                                    ? "text-primary"
                                    : "text-destructive"
                            }`}
                        >
                            Total: {percentageSum}%{" "}
                            {percentageSum !== 100 && "(must equal 100%)"}
                        </div>
                    </CardContent>
                </Card>

                <FormField
                    control={form.control}
                    name="variants"
                    render={() => (
                        <FormItem>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex gap-4">
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting
                            ? isEditMode
                                ? "Saving..."
                                : "Creating..."
                            : isEditMode
                              ? "Save Changes"
                              : "Create Flag Value"}
                    </Button>
                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => router.back()}
                        disabled={isSubmitting}
                    >
                        Cancel
                    </Button>
                </div>
            </form>
        </Form>
    );
}
