"use client";

import { useState } from "react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { deleteUser } from "@/lib/api/users";
import type { User } from "@/lib/types";

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

interface DeleteUserDialogProps {
    user: User;
    onDeleted: () => void;
    trigger?: React.ReactNode;
}

export function DeleteUserDialog({ user, onDeleted, trigger }: DeleteUserDialogProps) {
    const [open, setOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    async function handleDelete() {
        setIsDeleting(true);
        try {
            await deleteUser(user.id);
            toast.success("User deleted!", {
                description: `User "${user.firstName} ${user.lastName}" has been deleted.`,
            });
            setOpen(false);
            onDeleted();
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to delete user", {
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
                    <AlertDialogTitle>Delete User</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to permanently delete{" "}
                        <strong>{user.firstName} {user.lastName}</strong> ({user.email})?
                        This action cannot be undone. Consider disabling the account instead.
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
