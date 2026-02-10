"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { regenerateApiKey } from "@/lib/api/environments";
import type { Environment } from "@/lib/types";

import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

interface RegenerateApiKeyDialogProps {
    environment: Environment;
    onRegenerated: (updatedEnvironment: Environment) => void;
    trigger?: React.ReactNode;
}

export function RegenerateApiKeyDialog({ 
    environment, 
    onRegenerated, 
    trigger 
}: RegenerateApiKeyDialogProps) {
    const [open, setOpen] = useState(false);
    const [isRegenerating, setIsRegenerating] = useState(false);

    async function handleRegenerate() {
        setIsRegenerating(true);
        try {
            const updated = await regenerateApiKey(environment.id);
            toast.success("API key regenerated!", {
                description: "The old API key has been invalidated. Make sure to update your SDKs.",
            });
            setOpen(false);
            onRegenerated(updated);
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to regenerate API key", {
                    description: error.message,
                });
            } else {
                toast.error("An unexpected error occurred", {
                    description: "Please try again later.",
                });
            }
        } finally {
            setIsRegenerating(false);
        }
    }

    return (
        <AlertDialog open={open} onOpenChange={setOpen}>
            <AlertDialogTrigger asChild>
                {trigger ?? (
                    <Button variant="outline" size="sm">
                        <RefreshCw className="mr-2 h-4 w-4" />
                        Regenerate API Key
                    </Button>
                )}
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Regenerate API Key</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to regenerate the API key for &quot;{environment.name}&quot;?
                        The current API key will be <strong>immediately invalidated</strong> and any 
                        SDKs using it will stop working until updated with the new key.
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={isRegenerating}>Cancel</AlertDialogCancel>
                    <AlertDialogAction
                        onClick={handleRegenerate}
                        disabled={isRegenerating}
                    >
                        {isRegenerating ? "Regenerating..." : "Regenerate"}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}
