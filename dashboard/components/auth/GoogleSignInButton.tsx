"use client";

import { useState } from "react";
import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/api/client";

export function GoogleSignInButton() {
  const router = useRouter();
  const { oauthLogin } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

  async function handleSuccess(credentialResponse: CredentialResponse) {
    if (!credentialResponse.credential) {
      toast.error("Google Sign-In failed", {
        description: "No credential received from Google.",
      });
      return;
    }

    setIsLoading(true);

    try {
      await oauthLogin("google", credentialResponse.credential);
      toast.success("Welcome!", {
        description: "You have successfully signed in with Google.",
      });
      router.push("/dashboard");
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error("Google Sign-In failed", {
          description: error.message,
        });
      } else {
        toast.error("Google Sign-In failed", {
          description: "An unexpected error occurred. Please try again.",
        });
      }
    } finally {
      setIsLoading(false);
    }
  }

  function handleError() {
    toast.error("Google Sign-In failed", {
      description: "Could not connect to Google. Please try again.",
    });
  }

  return (
    <div className="w-full flex justify-center">
      <GoogleLogin
        onSuccess={handleSuccess}
        onError={handleError}
        useOneTap={false}
        theme="outline"
        size="large"
        width="100%"
        text="continue_with"
        shape="rectangular"
      />
    </div>
  );
}

export default GoogleSignInButton;
