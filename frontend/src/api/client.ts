import type { Catalog, DocumentDetail, DocumentSummary } from "./types";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, { signal, headers: { Accept: "application/json" } });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === "AbortError") throw cause;
    throw new ApiError("The review service cannot be reached. Is the backend running on port 8080?");
  }
  if (!response.ok) {
    let detail = `${response.status} ${response.statusText}`;
    try {
      const body = (await response.json()) as { error?: string };
      if (body.error) detail = body.error;
    } catch {
      // Not a JSON error body; keep the status line.
    }
    throw new ApiError(detail, response.status);
  }
  return (await response.json()) as T;
}

export const api = {
  catalog: (signal?: AbortSignal) => getJson<Catalog>("/api/catalog", signal),
  documents: (signal?: AbortSignal) => getJson<DocumentSummary[]>("/api/documents", signal),
  document: (id: string, signal?: AbortSignal) =>
    getJson<DocumentDetail>(`/api/documents/${encodeURIComponent(id)}`, signal),
};
