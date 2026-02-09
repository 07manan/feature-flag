"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ApiError } from "@/lib/api/client";
import { getEnvironment } from "@/lib/api/environments";
import type { Environment } from "@/lib/types";

import { EnvironmentForm } from "@/components/environments";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function EditEnvironmentPage() {
  const params = useParams();
  const router = useRouter();
  const [environment, setEnvironment] = useState<Environment | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const environmentId = params.id as string;

  useEffect(() => {
    async function fetchEnvironment() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getEnvironment(environmentId);
        setEnvironment(data);
      } catch (err) {
        if (err instanceof ApiError) {
          if (err.status === 404) {
            setError("Environment not found");
          } else {
            setError(err.message);
          }
          toast.error("Failed to load environment", {
            description: err.message,
          });
        } else {
          setError("An unexpected error occurred");
          toast.error("Failed to load environment", {
            description: "An unexpected error occurred.",
          });
        }
      } finally {
        setIsLoading(false);
      }
    }

    if (environmentId) {
      fetchEnvironment();
    }
  }, [environmentId]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !environment) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">Edit Environment</h1>
        <Card>
          <CardContent className="py-8">
            <div className="text-center">
              <p className="text-muted-foreground mb-4">
                {error || "Environment not found"}
              </p>
              <Button onClick={() => router.push("/environments")}>
                Back to Environments
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Edit Environment</h1>
      <Card>
        <CardHeader>
          <CardTitle>
            Environment Details
            <span className="ml-2 font-mono text-sm font-normal text-muted-foreground">
              ({environment.key})
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <EnvironmentForm mode="edit" initialData={environment} />
        </CardContent>
      </Card>
    </div>
  );
}
