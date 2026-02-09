"use client";

import { useAuth } from "@/context/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function DashboardPage() {
    const { user } = useAuth();

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
                <p className="text-muted-foreground mt-1">
                    Welcome back, {user?.firstName}!
                </p>
            </div>

            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                <Card>
                    <CardHeader>
                        <CardTitle>Feature Flags</CardTitle>
                        <CardDescription>
                            Manage your feature flags
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold">—</p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Environments</CardTitle>
                        <CardDescription>
                            Configure environments
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold">—</p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Users</CardTitle>
                        <CardDescription>
                            Manage team members
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold">—</p>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
