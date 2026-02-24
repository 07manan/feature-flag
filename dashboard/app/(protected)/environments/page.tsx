"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useDebounce } from "use-debounce";
import { Plus, Pencil, Search, Loader2, Settings } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getEnvironments } from "@/lib/api/environments";
import type { Environment } from "@/lib/types";

import { DeleteEnvironmentDialog, ApiKeyDisplay } from "@/components/environments";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function EnvironmentsPage() {
    const [environments, setEnvironments] = useState<Environment[]>([]);
    const [search, setSearch] = useState("");
    const [debouncedSearch] = useDebounce(search, 300);
    const [isLoading, setIsLoading] = useState(true);

    const fetchEnvironments = useCallback(async (searchQuery?: string) => {
        setIsLoading(true);
        try {
            const data = await getEnvironments(searchQuery);
            setEnvironments(data);
        } catch (error) {
            if (error instanceof ApiError) {
                toast.error("Failed to load environments", {
                    description: error.message,
                });
            } else {
                toast.error("Failed to load environments", {
                    description: "An unexpected error occurred.",
                });
            }
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchEnvironments(debouncedSearch || undefined);
    }, [debouncedSearch, fetchEnvironments]);

    function handleEnvironmentDeleted() {
        fetchEnvironments(debouncedSearch || undefined);
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-xl font-semibold">Environments</h1>
                <Link href="/environments/new">
                    <Button>
                        <Plus className="mr-2 h-4 w-4" />
                        Add Environment
                    </Button>
                </Link>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Environments</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="mb-4">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                                placeholder="Search environments by name or description..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                className="pl-10"
                            />
                        </div>
                    </div>

                    {isLoading ? (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 className="h-8 w-8 animate-spin text-primary/50" />
                        </div>
                    ) : environments.length === 0 ? (
                        <div className="text-center py-8">
                            <p className="text-muted-foreground">
                                {debouncedSearch
                                    ? "No environments found matching your search."
                                    : "No environments yet. Create your first environment to get started."}
                            </p>
                        </div>
                    ) : (
                        <div className="rounded-md border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="pl-4">Name</TableHead>
                                        <TableHead>Key</TableHead>
                                        <TableHead>API Key</TableHead>
                                        <TableHead className="text-right pr-4">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {environments.map((environment) => (
                                        <TableRow key={environment.id}>
                                            <TableCell className="pl-4">{environment.name}</TableCell>
                                            <TableCell>
                                                <code className="bg-muted/30 px-1.5 py-0.5 rounded border border-border/50 text-sm font-mono">
                                                    {environment.key}
                                                </code>
                                            </TableCell>
                                            <TableCell>
                                                <ApiKeyDisplay apiKey={environment.apiKey} />
                                            </TableCell>
                                            <TableCell className="text-right pr-4">
                                                <div className="flex items-center justify-end gap-2">
                                                    <Link href={`/flag-values?environmentId=${environment.id}`}>
                                                        <Button variant="outline" size="sm" title="Configure Values">
                                                            <Settings className="h-4 w-4" />
                                                        </Button>
                                                    </Link>
                                                    <Link href={`/environments/${environment.id}/edit`}>
                                                        <Button variant="outline" size="sm">
                                                            <Pencil className="h-4 w-4" />
                                                        </Button>
                                                    </Link>
                                                    <DeleteEnvironmentDialog
                                                        environment={environment}
                                                        onDeleted={handleEnvironmentDeleted}
                                                    />
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
