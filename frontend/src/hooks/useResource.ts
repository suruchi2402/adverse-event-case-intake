import { useCallback, useEffect, useState } from "react";

export type Resource<T> =
  | { state: "loading" }
  | { state: "error"; message: string }
  | { state: "ready"; data: T };

/** Loads data for a key, cancels the request when the key changes, and exposes a retry. */
export function useResource<T>(
  key: string | null,
  load: (signal: AbortSignal) => Promise<T>,
): Resource<T> & { reload: () => void } {
  const [resource, setResource] = useState<Resource<T>>({ state: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (key === null) return;
    const controller = new AbortController();
    setResource({ state: "loading" });
    load(controller.signal)
      .then((data) => setResource({ state: "ready", data }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setResource({ state: "error", message: error instanceof Error ? error.message : String(error) });
      });
    return () => controller.abort();
    // `load` is expected to be stable for a given key.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  return { ...resource, reload };
}
