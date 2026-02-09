"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { createFlag, updateFlag } from "@/lib/api/flags";
import {
    createFlagSchema,
    updateFlagSchema,
    FLAG_TYPES,
    type CreateFlagFormData,
    type UpdateFlagFormData,
} from "@/lib/validations/flags";
import type { Flag, FlagType } from "@/lib/types";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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

interface FlagFormProps {
    mode: "create" | "edit";
    initialData?: Flag;
}

export function FlagForm({ mode, initialData }: FlagFormProps) {
    const router = useRouter();
    const [isSubmitting, setIsSubmitting] = useState(false);

    const isEditMode = mode === "edit";
    const schema = isEditMode ? updateFlagSchema : createFlagSchema;

    const form = useForm<CreateFlagFormData | UpdateFlagFormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            key: initialData?.key ?? "",
            name: initialData?.name ?? "",
            description: initialData?.description ?? "",
            type: initialData?.type ?? "STRING",
            defaultValue: initialData?.defaultValue ?? "",
        },
    });

    const watchedType = form.watch("type") as FlagType;

    async function onSubmit(data: CreateFlagFormData | UpdateFlagFormData) {
        setIsSubmitting(true);
        try {
            if (isEditMode && initialData) {
                // Only send editable fields
                await updateFlag(initialData.id, {
                    name: data.name,
                    description: data.description || undefined,
                    defaultValue: data.defaultValue,
                });
                toast.success("Flag updated!", {
                    description: `Flag "${data.name}" has been updated successfully.`,
                });
            } else {
                await createFlag(data as CreateFlagFormData);
                toast.success("Flag created!", {
                    description: `Flag "${data.name}" has been created successfully.`,
                });
            }
            router.push("/flags");
            router.refresh();
        } catch (error) {
            if (error instanceof ApiError) {
                if (error.errors) {
                    Object.entries(error.errors).forEach(([field, message]) => {
                        form.setError(field as keyof CreateFlagFormData, { message });
                    });
                }
                toast.error(isEditMode ? "Failed to update flag" : "Failed to create flag", {
                    description: error.message,
                });
            } else {
                toast.error("An unexpected error occurred", {
                    description: "Please try again later.",
                });
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    // Render appropriate input based on flag type
    function renderDefaultValueInput(field: any) {
        switch (watchedType) {
            case "BOOLEAN":
                return (
                    <Select
                        onValueChange={field.onChange}
                        value={field.value}
                        disabled={isSubmitting}
                    >
                        <SelectTrigger>
                            <SelectValue placeholder="Select a value" />
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
                        placeholder="Enter a number"
                        {...field}
                        disabled={isSubmitting}
                    />
                );
            case "STRING":
            default:
                return (
                    <Input
                        type="text"
                        placeholder="Enter a value"
                        {...field}
                        disabled={isSubmitting}
                    />
                );
        }
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                <FormField
                    control={form.control}
                    name="key"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Key</FormLabel>
                            <FormControl>
                                <Input
                                    placeholder="my-feature-flag"
                                    {...field}
                                    disabled={isEditMode || isSubmitting}
                                />
                            </FormControl>
                            <FormDescription>
                                Unique identifier used by SDKs. Only lowercase letters, numbers, and
                                hyphens allowed.
                            </FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>
                            <FormControl>
                                <Input
                                    placeholder="My Feature Flag"
                                    {...field}
                                    disabled={isSubmitting}
                                />
                            </FormControl>
                            <FormDescription>
                                Human-readable name for this flag.
                            </FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Description (optional)</FormLabel>
                            <FormControl>
                                <Textarea
                                    placeholder="Describe what this feature flag controls..."
                                    className="resize-none"
                                    {...field}
                                    disabled={isSubmitting}
                                />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="type"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Type</FormLabel>
                            <Select
                                onValueChange={(value) => {
                                    field.onChange(value);
                                    // Reset default value when type changes
                                    if (value === "BOOLEAN") {
                                        form.setValue("defaultValue", "false");
                                    } else {
                                        form.setValue("defaultValue", "");
                                    }
                                }}
                                value={field.value}
                                disabled={isEditMode || isSubmitting}
                            >
                                <FormControl>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a type" />
                                    </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                    {FLAG_TYPES.map((type) => (
                                        <SelectItem key={type} value={type}>
                                            {type}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <FormDescription>
                                The data type of the flag&apos;s value. Cannot be changed after creation.
                            </FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name="defaultValue"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Default Value</FormLabel>
                            <FormControl>{renderDefaultValueInput(field)}</FormControl>
                            <FormDescription>
                                The value returned when no environment-specific override exists.
                            </FormDescription>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex gap-4">
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting
                            ? isEditMode
                                ? "Updating..."
                                : "Creating..."
                            : isEditMode
                                ? "Update Flag"
                                : "Create Flag"}
                    </Button>
                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => router.push("/flags")}
                        disabled={isSubmitting}
                    >
                        Cancel
                    </Button>
                </div>
            </form>
        </Form>
    );
}
