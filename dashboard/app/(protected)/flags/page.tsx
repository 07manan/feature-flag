"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useDebounce } from "use-debounce";
import { Plus, Pencil, Search, Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getFlags } from "@/lib/api/flags";
import type { Flag, FlagType } from "@/lib/types";

import { DeleteFlagDialog } from "@/components/flags";
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

const TYPE_BADGE_COLORS: Record<FlagType, string> = {
  STRING: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300",
  BOOLEAN: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300",
  NUMBER: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300",
};

export default function FlagsPage() {
  const [flags, setFlags] = useState<Flag[]>([]);
  const [search, setSearch] = useState("");
  const [debouncedSearch] = useDebounce(search, 300);
  const [isLoading, setIsLoading] = useState(true);

  const fetchFlags = useCallback(async (searchQuery?: string) => {
    setIsLoading(true);
    try {
      const data = await getFlags(searchQuery);
      setFlags(data);
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error("Failed to load flags", {
          description: error.message,
        });
      } else {
        toast.error("Failed to load flags", {
          description: "An unexpected error occurred.",
        });
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFlags(debouncedSearch || undefined);
  }, [debouncedSearch, fetchFlags]);

  function handleFlagDeleted() {
    fetchFlags(debouncedSearch || undefined);
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Feature Flags</h1>
        <Link href="/flags/new">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Add Flag
          </Button>
        </Link>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Flags</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="mb-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search flags by key, name, or description..."
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
          ) : flags.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">
                {debouncedSearch
                  ? "No flags found matching your search."
                  : "No feature flags yet. Create your first flag to get started."}
              </p>
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-4">Name</TableHead>
                    <TableHead>Key</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Default Value</TableHead>
                    <TableHead className="text-right pr-4">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {flags.map((flag) => (
                    <TableRow key={flag.id}>
                      <TableCell className="pl-4">{flag.name}</TableCell>
                      <TableCell>
                        <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono">
                          {flag.key}
                        </code>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="secondary"
                          className={TYPE_BADGE_COLORS[flag.type]}
                        >
                          {flag.type}
                        </Badge>
                      </TableCell>
                      <TableCell className="font-mono text-sm max-w-[200px] truncate">
                        {flag.defaultValue}
                      </TableCell>
                      <TableCell className="text-right pr-4">
                        <div className="flex items-center justify-end gap-2">
                          <Link href={`/flags/${flag.id}/edit`}>
                            <Button variant="outline" size="sm">
                              <Pencil className="h-4 w-4" />
                            </Button>
                          </Link>
                          <DeleteFlagDialog
                            flag={flag}
                            onDeleted={handleFlagDeleted}
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
