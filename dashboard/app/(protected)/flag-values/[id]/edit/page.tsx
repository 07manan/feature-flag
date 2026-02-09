"use client";

import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getFlagValue } from "@/lib/api/flag-values";
import type { FlagValue } from "@/lib/types";

import { FlagValueForm } from "@/components/flag-values";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface EditFlagValuePageProps {
    params: Promise<{ id: string }>;
}

export default function EditFlagValuePage({ params }: EditFlagValuePageProps) {
    const { id } = use(params);
    const router = useRouter();
    const [flagValue, setFlagValue] = useState<FlagValue | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        async function loadFlagValue() {
            try {
                const data = await getFlagValue(id);
                setFlagValue(data);
            } catch (error) {
                if (error instanceof ApiError) {
                    toast.error("Failed to load flag value", {
                        description: error.message,
                    });
                } else {
                    toast.error("Failed to load flag value", {
                        description: "An unexpected error occurred.",
                    });
                }
                router.push("/flag-values");
            } finally {
                setIsLoading(false);
            }
        }

        loadFlagValue();
    }, [id, router]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (!flagValue) {
        return null;
    }

    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-bold">Edit Flag Value</h1>

            <Card>
                <CardHeader>
                    <CardTitle>
                        <span className="font-mono text-base">
                            {flagValue.flagKey}
                        </span>{" "}
                        in{" "}
                        <span className="font-mono text-base">
                            {flagValue.environmentKey}
                        </span>
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <FlagValueForm mode="edit" initialData={flagValue} />
                </CardContent>
            </Card>
        </div>
    );
}
