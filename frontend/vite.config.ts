import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// The UI talks to the backend only through /api, proxied to the Spring Boot server in development.
const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { "/api": backend },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
