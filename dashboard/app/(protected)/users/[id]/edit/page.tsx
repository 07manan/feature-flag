"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getUser } from "@/lib/api/users";
import type { User } from "@/lib/types";

import { UserForm } from "@/components/users";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function EditUserPage() {
    const params = useParams();
    const router = useRouter();
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const userId = params.id as string;

    useEffect(() => {
        async function fetchUser() {
            setIsLoading(true);
            setError(null);
            try {
                const data = await getUser(userId);
                setUser(data);
            } catch (err) {
                if (err instanceof ApiError) {
                    if (err.status === 404) {
                        setError("User not found");
                    } else {
                        setError(err.message);
                    }
                    toast.error("Failed to load user", {
                        description: err.message,
                    });
                } else {
                    setError("An unexpected error occurred");
                    toast.error("Failed to load user", {
                        description: "An unexpected error occurred.",
                    });
                }
            } finally {
                setIsLoading(false);
            }
        }

        if (userId) {
            fetchUser();
        }
    }, [userId]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary/50" />
            </div>
        );
    }

    if (error || !user) {
        return (
            <div className="space-y-6">
                <h1 className="text-xl font-semibold">Edit User</h1>
                <Card>
                    <CardContent className="py-8">
                        <div className="text-center">
                            <p className="text-muted-foreground mb-4">
                                {error || "User not found"}
                            </p>
                            <Button onClick={() => router.push("/users")}>
                                Back to Users
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <h1 className="text-xl font-semibold">Edit User</h1>
            <Card>
                <CardHeader>
                    <CardTitle>
                        User Details
                        <span className="ml-2 text-sm font-normal text-muted-foreground">
                            ({user.email})
                        </span>
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <UserForm initialData={user} />
                </CardContent>
            </Card>
        </div>
    );
}
