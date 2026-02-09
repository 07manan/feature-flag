"use client";

import { FlagForm } from "@/components/flags";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function NewFlagPage() {
    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-bold">Create New Flag</h1>
            <Card>
                <CardHeader>
                    <CardTitle>Flag Details</CardTitle>
                </CardHeader>
                <CardContent>
                    <FlagForm mode="create" />
                </CardContent>
            </Card>
        </div>
    );
}
