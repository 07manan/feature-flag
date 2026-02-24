"use client";

import { useState } from "react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { deleteFlagValue } from "@/lib/api/flag-values";
import type { FlagValue } from "@/lib/types";

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
import { Trash2 } from "lucide-react";

interface DeleteFlagValueDialogProps {
    flagValue: FlagValue;
    onDeleted: () => void;
    trigger?: React.ReactNode;
}

export function DeleteFlagValueDialog({
    flagValue,
    onDeleted,
    trigger,
}: DeleteFlagValueDialogProps) {
    const [open, setOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    async function handleDelete() {
        setIsDeleting(true);
        try {
            await deleteFlagValue(flagValue.id);
            toast.success("Flag value deleted!", {
                description: `Flag value for "${flagValue.flagKey}" in "${flagValue.environmentKey}" has been deleted successfully.`,
            });
            setOpen(false);
            onDeleted();
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to delete flag value", {
                    description: error.message,
                });
            } else {
                toast.error("An unexpected error occurred", {
                    description: "Please try again later.",
                });
            }
        } finally {
            setIsDeleting(false);
        }
    }

    return (
        <AlertDialog open={open} onOpenChange={setOpen}>
            <AlertDialogTrigger asChild>
                {trigger ?? (
                    <Button
                        variant="outline"
                        size="sm"
                        className="text-destructive hover:text-destructive hover:bg-destructive/10"
                    >
                        <Trash2 className="h-4 w-4" />
                    </Button>
                )}
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete Flag Value</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to delete the flag value for{" "}
                        <code className="text-sm bg-muted/30 border border-border/50 px-1 rounded">
                            {flagValue.flagKey}
                        </code>{" "}
                        in environment{" "}
                        <code className="text-sm bg-muted/30 border border-border/50 px-1 rounded">
                            {flagValue.environmentKey}
                        </code>
                        ? This action cannot be undone.
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
                    <AlertDialogAction
                        onClick={handleDelete}
                        disabled={isDeleting}
                        className="bg-destructive/10 text-destructive border border-destructive/20 hover:bg-destructive/20"
                    >
                        {isDeleting ? "Deleting..." : "Delete"}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}
