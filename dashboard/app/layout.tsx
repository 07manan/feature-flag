import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/context/ThemeProvider";
import { AuthProvider } from "@/context/AuthContext";
import { GoogleAuthProvider } from "@/context/GoogleAuthProvider";
import { Toaster } from "@/components/ui/sonner";

const inter = Inter({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const jetbrainsMono = JetBrains_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

export const metadata: Metadata = {
    title: "Feature Flags Dashboard",
    description: "Manage your feature flags",
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en" suppressHydrationWarning>
            <body
                className={`${inter.variable} ${jetbrainsMono.variable} antialiased`}
            >
                <ThemeProvider>
                    <GoogleAuthProvider>
                        <AuthProvider>
                            {children}
                            <Toaster position="top-right" />
                        </AuthProvider>
                    </GoogleAuthProvider>
                </ThemeProvider>
            </body>
        </html>
    );
}
