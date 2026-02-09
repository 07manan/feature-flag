interface Config {
    apiUrl: string | undefined;
    googleClientId: string | undefined;
}

const config: Config = {
    apiUrl: process.env.NEXT_PUBLIC_API_URL,
    googleClientId: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID,
};

if (!config.apiUrl) {
    console.warn(
        "Warning: NEXT_PUBLIC_API_URL is not set. API calls will fail."
    );
}

if (!config.googleClientId) {
    console.warn(
        "Warning: NEXT_PUBLIC_GOOGLE_CLIENT_ID is not set. Google Sign-In will not work."
    );
}

export default config;
