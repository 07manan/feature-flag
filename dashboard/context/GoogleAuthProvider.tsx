"use client";

import { GoogleOAuthProvider } from "@react-oauth/google";
import config from "@/lib/config";
import type { ReactNode } from "react";

interface GoogleAuthProviderProps {
    children: ReactNode;
}

export function GoogleAuthProvider({ children }: GoogleAuthProviderProps) {
    if (!config.googleClientId) {
        return <>{children}</>;
    }

    return (
        <GoogleOAuthProvider clientId={config.googleClientId}>
            {children}
        </GoogleOAuthProvider>
    );
}

export default GoogleAuthProvider;
