import { useEffect } from "react";
import { api } from "./api/client";
import type { Catalog, DocumentSummary } from "./api/types";
import { DocumentRail } from "./components/DocumentRail";
import { EmptyState, ErrorState, LoadingState } from "./components/StateViews";
import { Workspace } from "./components/Workspace";
import { useResource } from "./hooks/useResource";
import { useHashRoute } from "./lib/route";

interface AppData {
  catalog: Catalog;
  documents: DocumentSummary[];
}

async function loadAppData(signal: AbortSignal): Promise<AppData> {
  const [catalog, documents] = await Promise.all([api.catalog(signal), api.documents(signal)]);
  return { catalog, documents };
}

export function App() {
  const data = useResource("app", loadAppData);
  const [route, navigate] = useHashRoute();

  const documents = data.state === "ready" ? data.data.documents : [];
  const selectedId = route.documentId && documents.some((d) => d.id === route.documentId) ? route.documentId : null;

  // Open the first document when none (or an unknown one) is selected.
  useEffect(() => {
    const first = documents[0];
    if (data.state === "ready" && !selectedId && first) navigate({ documentId: first.id, caseIndex: 0 });
  }, [data.state, documents, selectedId, navigate]);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">Case review</span>
        </div>
        {data.state === "ready" ? (
          <span className="topbar-meta">Catalog {data.data.catalog.catalog_version} · {data.data.catalog.fields.length} fields</span>
        ) : null}
      </header>

      {data.state === "loading" ? (
        <main className="layout">
          <div className="rail">
            <LoadingState label="Loading documents" lines={8} />
          </div>
          <div className="workspace is-single" />
        </main>
      ) : data.state === "error" ? (
        <main className="layout is-single">
          <ErrorState title="The review service is not available" message={data.message} onRetry={data.reload} />
        </main>
      ) : documents.length === 0 ? (
        <main className="layout is-single">
          <EmptyState icon="○" title="No documents">
            The documents folder is empty. Add .txt reports to <code>documents/</code> and run the pipeline.
          </EmptyState>
        </main>
      ) : (
        <main className="layout">
          <DocumentRail documents={documents} selectedId={selectedId} onSelect={(id) => navigate({ documentId: id, caseIndex: 0 })} />
          {selectedId ? (
            <Workspace
              key={selectedId}
              catalog={data.data.catalog}
              documentId={selectedId}
              caseIndex={route.caseIndex}
              onCase={(index) => navigate({ documentId: selectedId, caseIndex: index })}
            />
          ) : (
            <div className="workspace is-single" />
          )}
        </main>
      )}
    </div>
  );
}
