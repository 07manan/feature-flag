"use client";

import { EnvironmentForm } from "@/components/environments";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function NewEnvironmentPage() {
    return (
        <div className="space-y-6">
            <h1 className="text-xl font-semibold">Create New Environment</h1>
            <Card>
                <CardHeader>
                    <CardTitle>Environment Details</CardTitle>
                </CardHeader>
                <CardContent>
                    <EnvironmentForm mode="create" />
                </CardContent>
            </Card>
        </div>
    );
}
