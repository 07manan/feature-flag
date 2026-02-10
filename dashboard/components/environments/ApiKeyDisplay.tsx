"use client";

import { CopyButton } from "@/components/ui/copy-button";

interface ApiKeyDisplayProps {
    apiKey: string;
}

function maskApiKey(apiKey: string): string {
    const parts = apiKey.split("_");
    if (parts.length < 3) {
        return apiKey.slice(0, 15) + "...";
    }
    
    const prefix = parts[0];
    const envKey = parts[1];
    const randomPart = parts.slice(2).join("_");
    
    const visibleRandom = randomPart.slice(0, 4);
    
    return `${prefix}_${envKey}_${visibleRandom}...`;
}

export function ApiKeyDisplay({ apiKey }: ApiKeyDisplayProps) {
    return (
        <div className="inline-flex items-center gap-1">
            <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono">
                {maskApiKey(apiKey)}
            </code>
            <CopyButton value={apiKey} />
        </div>
    );
}
