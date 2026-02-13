"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useDebounce } from "use-debounce";
import { Pencil, Search, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getUsers } from "@/lib/api/users";
import type { User } from "@/lib/types";
import { useAuth } from "@/context/AuthContext";

import { DeleteUserDialog } from "@/components/users";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const ROLE_BADGE_COLORS: Record<string, string> = {
    ADMIN: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
    GUEST: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300",
};

export default function UsersPage() {
    const { user: currentUser } = useAuth();
    const [users, setUsers] = useState<User[]>([]);
    const [search, setSearch] = useState("");
    const [debouncedSearch] = useDebounce(search, 300);
    const [isLoading, setIsLoading] = useState(true);

    const fetchUsers = useCallback(async (searchQuery?: string) => {
        setIsLoading(true);
        try {
            const data = await getUsers(searchQuery);
            setUsers(data);
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to load users", {
                    description: error.message,
                });
            } else {
                toast.error("Failed to load users", {
                    description: "An unexpected error occurred.",
                });
            }
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchUsers(debouncedSearch || undefined);
    }, [debouncedSearch, fetchUsers]);

    function handleUserDeleted() {
        fetchUsers(debouncedSearch || undefined);
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">User Management</h1>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Users</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="mb-4">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                                placeholder="Search users by name or email..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                className="pl-10"
                            />
                        </div>
                    </div>

                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                        </div>
                    ) : users.length === 0 ? (
                        <div className="text-center py-8">
                            <p className="text-muted-foreground">
                                {debouncedSearch
                                    ? "No users found matching your search."
                                    : "No users found."}
                            </p>
                        </div>
                    ) : (
                        <div className="rounded-md border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="pl-4">Name</TableHead>
                                        <TableHead>Email</TableHead>
                                        <TableHead>Role</TableHead>
                                        <TableHead>Status</TableHead>
                                        <TableHead className="text-right pr-4">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {users.map((user) => (
                                        <TableRow key={user.id}>
                                            <TableCell className="pl-4">
                                                {user.firstName} {user.lastName}
                                            </TableCell>
                                            <TableCell>{user.email}</TableCell>
                                            <TableCell>
                                                <Badge
                                                    variant="secondary"
                                                    className={ROLE_BADGE_COLORS[user.role]}
                                                >
                                                    {user.role}
                                                </Badge>
                                            </TableCell>
                                            <TableCell>
                                                <Badge
                                                    variant="secondary"
                                                    className={
                                                        user.enabled
                                                            ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300"
                                                            : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300"
                                                    }
                                                >
                                                    {user.enabled ? "Enabled" : "Disabled"}
                                                </Badge>
                                            </TableCell>
                                            <TableCell className="text-right pr-4">
                                                <div className="flex items-center justify-end gap-2">
                                                    <Link href={`/users/${user.id}/edit`}>
                                                        <Button variant="outline" size="sm">
                                                            <Pencil className="h-4 w-4" />
                                                        </Button>
                                                    </Link>
                                                    {currentUser?.id !== user.id && (
                                                        <DeleteUserDialog
                                                            user={user}
                                                            onDeleted={handleUserDeleted}
                                                        />
                                                    )}
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
