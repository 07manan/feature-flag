"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getFlag } from "@/lib/api/flags";
import type { Flag } from "@/lib/types";

import { FlagForm } from "@/components/flags";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function EditFlagPage() {
    const params = useParams();
    const router = useRouter();
    const [flag, setFlag] = useState<Flag | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const flagId = params.id as string;

    useEffect(() => {
        async function fetchFlag() {
            setIsLoading(true);
            setError(null);
            try {
                const data = await getFlag(flagId);
                setFlag(data);
            } catch (err) {
                if (err instanceof ApiError) {
                    if (err.status === 404) {
                        setError("Flag not found");
                    } else {
                        setError(err.message);
                    }
                    toast.error("Failed to load flag", {
                        description: err.message,
                    });
                } else {
                    setError("An unexpected error occurred");
                    toast.error("Failed to load flag", {
                        description: "An unexpected error occurred.",
                    });
                }
            } finally {
                setIsLoading(false);
            }
        }

        if (flagId) {
            fetchFlag();
        }
    }, [flagId]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary/50" />
            </div>
        );
    }

    if (error || !flag) {
        return (
            <div className="space-y-6">
                <h1 className="text-xl font-semibold">Edit Flag</h1>
                <Card>
                    <CardContent className="py-8">
                        <div className="text-center">
                            <p className="text-muted-foreground mb-4">
                                {error || "Flag not found"}
                            </p>
                            <Button onClick={() => router.push("/flags")}>
                                Back to Flags
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <h1 className="text-xl font-semibold">Edit Flag</h1>
            <Card>
                <CardHeader>
                    <CardTitle>
                        Flag Details
                        <span className="ml-2 font-mono text-sm font-normal text-muted-foreground">
                            ({flag.key})
                        </span>
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <FlagForm mode="edit" initialData={flag} />
                </CardContent>
            </Card>
        </div>
    );
}
