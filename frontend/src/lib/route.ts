import { useCallback, useEffect, useState } from "react";

/** The selection lives in the URL hash (#/D04/1) so it survives a reload and can be shared. */
export interface Route {
  documentId: string | null;
  caseIndex: number;
}

export function parseHash(hash: string): Route {
  const [documentId, caseIndex] = hash.replace(/^#\/?/, "").split("/");
  const index = Number.parseInt(caseIndex ?? "", 10);
  return {
    documentId: documentId ? decodeURIComponent(documentId) : null,
    caseIndex: Number.isFinite(index) && index >= 0 ? index : 0,
  };
}

export function formatHash(route: Route): string {
  if (!route.documentId) return "#/";
  return `#/${encodeURIComponent(route.documentId)}${route.caseIndex > 0 ? `/${route.caseIndex}` : ""}`;
}

export function useHashRoute(): [Route, (route: Route) => void] {
  const [route, setRoute] = useState<Route>(() => parseHash(window.location.hash));
  useEffect(() => {
    const onChange = () => setRoute(parseHash(window.location.hash));
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  const navigate = useCallback((next: Route) => {
    const hash = formatHash(next);
    if (window.location.hash !== hash) window.location.hash = hash;
    setRoute(next);
  }, []);
  return [route, navigate];
}
