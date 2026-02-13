"use client";

import { ShieldAlert } from "lucide-react";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Sidebar } from "@/components/navigation";

export default function ProtectedLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const { isLoading, isAuthenticated } = useRequireAuth();
    const { user, logout } = useAuth();

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-pulse text-muted-foreground">Loading...</div>
            </div>
        );
    }

    if (!isAuthenticated) {
        return null;
    }

    if (user?.role !== "ADMIN") {
        return (
            <div className="min-h-screen flex flex-col">
                <header className="border-b bg-card">
                    <div className="container mx-auto px-4 py-3 flex items-center justify-between">
                        <h1 className="text-lg font-semibold">Feature Flags</h1>
                        <div className="flex items-center gap-4">
                            <span className="text-sm text-muted-foreground">
                                {user?.firstName} {user?.lastName}
                            </span>
                            <Button variant="outline" size="sm" onClick={logout}>
                                Sign out
                            </Button>
                        </div>
                    </div>
                </header>
                <div className="flex flex-1 items-center justify-center p-6">
                    <Card className="max-w-md w-full">
                        <CardContent className="pt-6 text-center space-y-4">
                            <ShieldAlert className="h-12 w-12 mx-auto text-muted-foreground" />
                            <h2 className="text-xl font-semibold">Admin Access Required</h2>
                            <p className="text-muted-foreground">
                                You don&apos;t have permission to access this area. Please contact an
                                administrator to grant you the ADMIN role.
                            </p>
                            <Button variant="outline" onClick={logout}>
                                Sign out
                            </Button>
                        </CardContent>
                    </Card>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col">
            <header className="border-b bg-card">
                <div className="container mx-auto px-4 py-3 flex items-center justify-between">
                    <h1 className="text-lg font-semibold">Feature Flags</h1>
                    <div className="flex items-center gap-4">
                        <span className="text-sm text-muted-foreground">
                            {user?.firstName} {user?.lastName}
                        </span>
                        <Button variant="outline" size="sm" onClick={logout}>
                            Sign out
                        </Button>
                    </div>
                </div>
            </header>

            <div className="flex flex-1">
                <Sidebar />
                <main className="flex-1 p-6">
                    {children}
                </main>
            </div>
        </div>
    );
}
