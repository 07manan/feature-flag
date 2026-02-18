"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Flag, Globe, Users, ToggleRight, Loader2, ArrowRight } from "lucide-react";
import { toast } from "sonner";

import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/api/client";
import { getFlags } from "@/lib/api/flags";
import { getEnvironments } from "@/lib/api/environments";
import { getUsers } from "@/lib/api/users";
import type { Flag as FlagType } from "@/lib/types";
import type { Environment } from "@/lib/types";
import type { User } from "@/lib/types";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

interface DashboardStats {
    flags: FlagType[];
    environments: Environment[];
    users: User[];
}

const TYPE_BADGE_COLORS: Record<string, string> = {
    STRING: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
    BOOLEAN: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
    NUMBER: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300",
};

export default function DashboardPage() {
    const { user } = useAuth();
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const fetchStats = useCallback(async () => {
        setIsLoading(true);
        try {
            const [flags, environments, users] = await Promise.all([
                getFlags(),
                getEnvironments(),
                getUsers(),
            ]);
            setStats({ flags, environments, users });
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to load dashboard data", {
                    description: error.message,
                });
            } else {
                toast.error("Failed to load dashboard data", {
                    description: "An unexpected error occurred.",
                });
            }
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchStats();
    }, [fetchStats]);

    const recentFlags = [...(stats?.flags ?? [])]
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
        .slice(0, 5);
    const activeEnvironments = stats?.environments.filter((e) => e.isActive) ?? [];
    const adminUsers = stats?.users.filter((u) => u.role === "ADMIN") ?? [];

    if (isLoading) {
        return (
            <div className="container mx-auto px-4 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
                    <p className="text-muted-foreground mt-1">
                        Welcome back, {user?.firstName}!
                    </p>
                </div>
                <div className="flex items-center justify-center py-16">
                    <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
                <p className="text-muted-foreground mt-1">
                    Welcome back, {user?.firstName}!
                </p>
            </div>

            {/* Stats Cards */}
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 mb-8">
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Total Flags</CardTitle>
                        <Flag className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{stats?.flags.length ?? 0}</div>
                        <p className="text-xs text-muted-foreground">
                            Across {stats?.environments.length ?? 0} environments
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Environments</CardTitle>
                        <Globe className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{stats?.environments.length ?? 0}</div>
                        <p className="text-xs text-muted-foreground">
                            {activeEnvironments.length} active
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Team Members</CardTitle>
                        <Users className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{stats?.users.length ?? 0}</div>
                        <p className="text-xs text-muted-foreground">
                            {adminUsers.length} admin · {(stats?.users.length ?? 0) - adminUsers.length} guest
                        </p>
                    </CardContent>
                </Card>

            </div>

            {/* Recent Flags & Quick Links */}
            <div className="grid gap-4 lg:grid-cols-3">
                <Card className="lg:col-span-2">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <CardTitle>Recently Updated Flags</CardTitle>
                            <CardDescription>Latest changes to your feature flags</CardDescription>
                        </div>
                        <Link href="/flags">
                            <Button variant="outline" size="sm">
                                View All
                                <ArrowRight className="ml-2 h-4 w-4" />
                            </Button>
                        </Link>
                    </CardHeader>
                    <CardContent>
                        {recentFlags.length === 0 ? (
                            <p className="text-sm text-muted-foreground text-center py-4">
                                No flags yet. Create your first flag to get started.
                            </p>
                        ) : (
                            <div className="rounded-md border">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead className="pl-4">Name</TableHead>
                                            <TableHead>Key</TableHead>
                                            <TableHead>Type</TableHead>
                                            <TableHead>Default</TableHead>
                                            <TableHead className="pr-4">Updated</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {recentFlags.map((flag) => (
                                            <TableRow key={flag.id}>
                                                <TableCell className="pl-4 font-medium">
                                                    <Link href={`/flags/${flag.id}/edit`} className="hover:underline">
                                                        {flag.name}
                                                    </Link>
                                                </TableCell>
                                                <TableCell>
                                                    <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono">
                                                        {flag.key}
                                                    </code>
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant="secondary" className={TYPE_BADGE_COLORS[flag.type]}>
                                                        {flag.type}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell className="font-mono text-sm max-w-[120px] truncate">
                                                    {flag.defaultValue}
                                                </TableCell>
                                                <TableCell className="pr-4 text-sm text-muted-foreground">
                                                    {new Date(flag.updatedAt).toLocaleDateString()}
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </div>
                        )}
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Quick Actions</CardTitle>
                        <CardDescription>Common tasks</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        <Link href="/flags/new" className="block">
                            <Button variant="outline" className="w-full justify-start">
                                <Flag className="mr-2 h-4 w-4" />
                                Create Flag
                            </Button>
                        </Link>
                        <Link href="/environments/new" className="block">
                            <Button variant="outline" className="w-full justify-start">
                                <Globe className="mr-2 h-4 w-4" />
                                Add Environment
                            </Button>
                        </Link>
                        <Link href="/flag-values" className="block">
                            <Button variant="outline" className="w-full justify-start">
                                <ToggleRight className="mr-2 h-4 w-4" />
                                Configure Flag Values
                            </Button>
                        </Link>
                        <Link href="/users" className="block">
                            <Button variant="outline" className="w-full justify-start">
                                <Users className="mr-2 h-4 w-4" />
                                Manage Users
                            </Button>
                        </Link>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
