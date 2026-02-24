"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Flag, LayoutDashboard, Layers, Users, Sliders, Github, Linkedin } from "lucide-react";

import { cn } from "@/lib/utils";

interface NavItem {
    label: string;
    href: string;
    icon: React.ComponentType<{ className?: string }>;
}

const navItems: NavItem[] = [
    {
        label: "Dashboard",
        href: "/dashboard",
        icon: LayoutDashboard,
    },
    {
        label: "Flags",
        href: "/flags",
        icon: Flag,
    },
    {
        label: "Environments",
        href: "/environments",
        icon: Layers,
    },
    {
        label: "Flag Values",
        href: "/flag-values",
        icon: Sliders,
    },
    {
        label: "Users",
        href: "/users",
        icon: Users,
    },
];

const externalLinks = [
    {
        label: "GitHub",
        href: "https://github.com/07manan/feature-flag",
        icon: Github,
    },
    {
        label: "LinkedIn",
        href: "https://www.linkedin.com/in/mananrpatel/",
        icon: Linkedin,
    },
];

export function Sidebar() {
    const pathname = usePathname();

    return (
        <aside className="w-56 border-r border-sidebar-border bg-sidebar flex flex-col">
            <nav className="flex-1 p-3">
                <ul className="space-y-0.5">
                    {navItems.map((item) => {
                        const isActive = pathname.startsWith(item.href);
                        const Icon = item.icon;

                        return (
                            <li key={item.href}>
                                <Link
                                    href={item.href}
                                    className={cn(
                                        "relative flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-150",
                                        isActive
                                            ? "bg-sidebar-primary/10 text-sidebar-primary before:absolute before:left-0 before:top-1/2 before:-translate-y-1/2 before:h-4 before:w-0.5 before:rounded-full before:bg-sidebar-primary"
                                            : "text-sidebar-foreground/60 hover:text-sidebar-foreground hover:bg-sidebar-accent/50"
                                    )}
                                >
                                    <Icon className="size-4 shrink-0" />
                                    {item.label}
                                </Link>
                            </li>
                        );
                    })}
                </ul>
            </nav>

            <div className="border-t border-sidebar-border p-3">
                <ul className="space-y-0.5">
                    {externalLinks.map((item) => {
                        const Icon = item.icon;
                        return (
                            <li key={item.href}>
                                <a
                                    href={item.href}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-150 text-sidebar-foreground/60 hover:text-sidebar-foreground hover:bg-sidebar-accent/50"
                                >
                                    <Icon className="size-4 shrink-0" />
                                    {item.label}
                                </a>
                            </li>
                        );
                    })}
                </ul>
            </div>
        </aside>
    );
}

export default Sidebar;
