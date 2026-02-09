"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";

import { useAuth } from "@/context/AuthContext";
import { registerSchema, type RegisterFormData } from "@/lib/validations/auth";
import { ApiError } from "@/lib/api/client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form";
import { GoogleSignInButton } from "./GoogleSignInButton";

type RegisterFormFields = "email" | "password" | "firstName" | "lastName";

export function RegisterForm() {
    const router = useRouter();
    const { register: registerUser } = useAuth();
    const [isSubmitting, setIsSubmitting] = useState(false);

    const form = useForm<RegisterFormData>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            email: "",
            password: "",
            firstName: "",
            lastName: "",
        },
    });

    async function onSubmit(data: RegisterFormData) {
        setIsSubmitting(true);

        try {
            await registerUser(data);
            toast.success("Account created!", {
                description: "Welcome to Feature Flags Dashboard.",
            });
            router.push("/dashboard");
        } catch (error) {
            if (error instanceof ApiError) {
                if (error.errors) {
                    Object.entries(error.errors).forEach(([field, message]) => {
                        form.setError(field as RegisterFormFields, { message });
                    });
                }
                toast.error("Registration failed", {
                    description: error.message,
                });
            } else {
                toast.error("Registration failed", {
                    description: "An unexpected error occurred. Please try again.",
                });
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <Card className="w-full max-w-md">
            <CardHeader className="space-y-1">
                <CardTitle className="text-2xl font-bold">Create an account</CardTitle>
                <CardDescription>
                    Enter your details to get started
                </CardDescription>
            </CardHeader>
            <CardContent>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="firstName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>First name</FormLabel>
                                        <FormControl>
                                            <Input
                                                placeholder="John"
                                                autoComplete="given-name"
                                                {...field}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="lastName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Last name</FormLabel>
                                        <FormControl>
                                            <Input
                                                placeholder="Doe"
                                                autoComplete="family-name"
                                                {...field}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                        <FormField
                            control={form.control}
                            name="email"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Email</FormLabel>
                                    <FormControl>
                                        <Input
                                            type="email"
                                            placeholder="you@example.com"
                                            autoComplete="email"
                                            {...field}
                                        />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="password"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Password</FormLabel>
                                    <FormControl>
                                        <Input
                                            type="password"
                                            placeholder="••••••••"
                                            autoComplete="new-password"
                                            {...field}
                                        />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <Button type="submit" className="w-full" disabled={isSubmitting}>
                            {isSubmitting ? "Creating account..." : "Create account"}
                        </Button>
                    </form>
                </Form>

                <div className="relative my-6">
                    <div className="absolute inset-0 flex items-center">
                        <span className="w-full border-t" />
                    </div>
                    <div className="relative flex justify-center text-xs uppercase">
                        <span className="bg-card px-2 text-muted-foreground">Or continue with</span>
                    </div>
                </div>

                <GoogleSignInButton />
            </CardContent>
            <CardFooter className="flex justify-center">
                <p className="text-sm text-muted-foreground">
                    Already have an account?{" "}
                    <Link
                        href="/login"
                        className="font-medium text-primary underline-offset-4 hover:underline"
                    >
                        Sign in
                    </Link>
                </p>
            </CardFooter>
        </Card>
    );
}

export default RegisterForm;
