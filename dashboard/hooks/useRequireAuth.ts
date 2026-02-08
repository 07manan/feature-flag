"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import type { User } from "@/lib/types";

interface UseRequireAuthReturn {
  isLoading: boolean;
  isAuthenticated: boolean;
  user: User | null;
}

export function useRequireAuth(redirectTo: string = "/login"): UseRequireAuthReturn {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace(redirectTo);
    }
  }, [isLoading, isAuthenticated, router, redirectTo]);

  return { isLoading, isAuthenticated, user };
}

export default useRequireAuth;
