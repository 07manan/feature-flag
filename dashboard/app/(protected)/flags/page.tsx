"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function FlagsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Feature Flags</h1>
      <Card>
        <CardHeader>
          <CardTitle>Flags</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">Manage your feature flags here.</p>
        </CardContent>
      </Card>
    </div>
  );
}
