import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
    "inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
    {
        variants: {
            variant: {
                default:
                    "border-primary/20 bg-primary/10 text-primary",
                secondary:
                    "border-border bg-muted/50 text-muted-foreground",
                destructive:
                    "border-destructive/20 bg-destructive/10 text-destructive",
                outline: "text-foreground",
                success:
                    "border-emerald-500/20 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
                info:
                    "border-cyan-500/20 bg-cyan-500/10 text-cyan-600 dark:text-cyan-400",
                warning:
                    "border-amber-500/20 bg-amber-500/10 text-amber-600 dark:text-amber-400",
                purple:
                    "border-violet-500/20 bg-violet-500/10 text-violet-600 dark:text-violet-400",
            },
        },
        defaultVariants: {
            variant: "default",
        },
    }
);

export interface BadgeProps
    extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> { }

function Badge({ className, variant, ...props }: BadgeProps) {
    return (
        <div className={cn(badgeVariants({ variant }), className)} {...props} />
    );
}

export { Badge, badgeVariants };
