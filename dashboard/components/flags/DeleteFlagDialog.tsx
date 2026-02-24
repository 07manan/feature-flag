"use client";

import { useState } from "react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { deleteFlag } from "@/lib/api/flags";
import type { Flag } from "@/lib/types";

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

interface DeleteFlagDialogProps {
    flag: Flag;
    onDeleted: () => void;
    trigger?: React.ReactNode;
}

export function DeleteFlagDialog({ flag, onDeleted, trigger }: DeleteFlagDialogProps) {
    const [open, setOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    async function handleDelete() {
        setIsDeleting(true);
        try {
            await deleteFlag(flag.id);
            toast.success("Flag deleted!", {
                description: `Flag "${flag.name}" has been deleted successfully.`,
            });
            setOpen(false);
            onDeleted();
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to delete flag", {
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
                    <Button variant="outline" size="sm" className="text-destructive hover:text-destructive hover:bg-destructive/10">
                        <Trash2 className="h-4 w-4" />
                    </Button>
                )}
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete Feature Flag</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to delete the flag &quot;{flag.name}&quot; (
                        <code className="text-sm bg-muted/30 border border-border/50 px-1 rounded">{flag.key}</code>)?
                        This action cannot be undone.
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
