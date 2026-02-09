"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function EnvironmentsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Environments</h1>
      <Card>
        <CardHeader>
          <CardTitle>Environments</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">Manage your environments here.</p>
        </CardContent>
      </Card>
    </div>
  );
}
