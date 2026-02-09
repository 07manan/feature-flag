"use client";

import { FlagValueForm } from "@/components/flag-values";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function NewFlagValuePage() {
    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-bold">Create Flag Value</h1>

            <Card>
                <CardHeader>
                    <CardTitle>Flag Value Details</CardTitle>
                </CardHeader>
                <CardContent>
                    <FlagValueForm mode="create" />
                </CardContent>
            </Card>
        </div>
    );
}
