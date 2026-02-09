"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { createEnvironment, updateEnvironment } from "@/lib/api/environments";
import {
    createEnvironmentSchema,
    updateEnvironmentSchema,
    type CreateEnvironmentFormData,
    type UpdateEnvironmentFormData,
} from "@/lib/validations/environments";
import type { Environment } from "@/lib/types";

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

interface EnvironmentFormProps {
    mode: "create" | "edit";
    initialData?: Environment;
}

export function EnvironmentForm({ mode, initialData }: EnvironmentFormProps) {
    const router = useRouter();
    const [isSubmitting, setIsSubmitting] = useState(false);

    const isEditMode = mode === "edit";
    const schema = isEditMode ? updateEnvironmentSchema : createEnvironmentSchema;

    const form = useForm<CreateEnvironmentFormData | UpdateEnvironmentFormData>({
        resolver: zodResolver(schema),
        defaultValues: {
            key: initialData?.key ?? "",
            name: initialData?.name ?? "",
            description: initialData?.description ?? "",
        },
    });

    async function onSubmit(data: CreateEnvironmentFormData | UpdateEnvironmentFormData) {
        setIsSubmitting(true);
        try {
            if (isEditMode && initialData) {
                // Only send editable fields
                await updateEnvironment(initialData.id, {
                    name: data.name,
                    description: data.description || undefined,
                });
                toast.success("Environment updated!", {
                    description: `Environment "${data.name}" has been updated successfully.`,
                });
            } else {
                await createEnvironment(data as CreateEnvironmentFormData);
                toast.success("Environment created!", {
                    description: `Environment "${data.name}" has been created successfully.`,
                });
            }
            router.push("/environments");
            router.refresh();
        } catch (error) {
            if (error instanceof ApiError) {
                if (error.errors) {
                    Object.entries(error.errors).forEach(([field, message]) => {
                        form.setError(field as keyof CreateEnvironmentFormData, { message });
                    });
                }
                toast.error(isEditMode ? "Failed to update environment" : "Failed to create environment", {
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
                                    placeholder="production"
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
                                    placeholder="Production"
                                    {...field}
                                    disabled={isSubmitting}
                                />
                            </FormControl>
                            <FormDescription>
                                Human-readable name for this environment.
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
                                    placeholder="Describe what this environment is used for..."
                                    className="resize-none"
                                    {...field}
                                    disabled={isSubmitting}
                                />
                            </FormControl>
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
                                ? "Update Environment"
                                : "Create Environment"}
                    </Button>
                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => router.push("/environments")}
                        disabled={isSubmitting}
                    >
                        Cancel
                    </Button>
                </div>
            </form>
        </Form>
    );
}
