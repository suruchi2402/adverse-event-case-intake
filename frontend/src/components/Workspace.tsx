import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import type { Catalog, DocumentDetail } from "../api/types";
import { useResource } from "../hooks/useResource";
import { flagLabel, groupBySection, matchesFilter, type FieldFilter } from "../lib/review";
import { DocumentPane } from "./DocumentPane";
import { FieldPanel } from "./FieldPanel";
import { EmptyState, ErrorState, LoadingState } from "./StateViews";

const UNSUPPORTED = "unsupported_format";
const NO_EVENT = "no_adverse_event";
const EMBEDDED = "embedded_instructions";

interface Props {
  catalog: Catalog;
  documentId: string;
  caseIndex: number;
  onCase: (index: number) => void;
}

export function Workspace({ catalog, documentId, caseIndex, onCase }: Props) {
  const doc = useResource(documentId, (signal) => api.document(documentId, signal));

  if (doc.state === "loading") {
    return (
      <div className="workspace">
        <div className="document-pane">
          <LoadingState label="Loading document" lines={14} />
        </div>
        <div className="side-pane">
          <LoadingState label="Loading fields" lines={8} />
        </div>
      </div>
    );
  }
  if (doc.state === "error") {
    return (
      <div className="workspace is-single">
        <ErrorState title={`Could not open ${documentId}`} message={doc.message} onRetry={doc.reload} />
      </div>
    );
  }
  return <LoadedWorkspace catalog={catalog} detail={doc.data} caseIndex={caseIndex} onCase={onCase} />;
}

function LoadedWorkspace({ catalog, detail, caseIndex, onCase }: { catalog: Catalog; detail: DocumentDetail; caseIndex: number; onCase: (i: number) => void }) {
  const cases = detail.extraction?.cases ?? [];
  const flags = detail.extraction?.flags ?? [];
  const activeIndex = Math.min(caseIndex, Math.max(cases.length - 1, 0));
  const kase = cases[activeIndex];

  const [filter, setFilter] = useState<FieldFilter>("all");
  const [selectedFieldId, setSelectedFieldId] = useState<string | null>(null);

  const groups = useMemo(() => (kase ? groupBySection(catalog, kase) : []), [catalog, kase]);
  const entries = useMemo(() => groups.flatMap((g) => g.entries), [groups]);
  const visibleIds = useMemo(
    () => entries.filter((e) => matchesFilter(e.risk, filter)).map((e) => e.field.id),
    [entries, filter],
  );

  const changeCase = useCallback(
    (index: number) => {
      setSelectedFieldId(null);
      onCase(index);
    },
    [onCase],
  );

  useKeyboard({
    next: () => setSelectedFieldId((id) => step(visibleIds, id, 1)),
    previous: () => setSelectedFieldId((id) => step(visibleIds, id, -1)),
    nextCase: () => cases.length > 1 && changeCase((activeIndex + 1) % cases.length),
    previousCase: () => cases.length > 1 && changeCase((activeIndex - 1 + cases.length) % cases.length),
    clear: () => setSelectedFieldId(null),
  });

  return (
    <div className="workspace">
      <div className="document-pane">
        <header className="pane-header">
          <div className="pane-heading">
            <span className="doc-id">{detail.id}</span>
            <h1 className="doc-title">{detail.title}</h1>
          </div>
          <div className="doc-meta">
            {flags.map((flag) => (
              <span key={flag} className={`flag${flag === EMBEDDED ? " flag-warn" : ""}`} title={flag}>
                {flagLabel(flag)}
              </span>
            ))}
            {detail.extraction?.model ? <span className="meta">{detail.extraction.model}</span> : null}
          </div>
        </header>
        {detail.text !== null ? (
          <DocumentPane text={detail.text} entries={entries} selectedFieldId={selectedFieldId} onSelect={setSelectedFieldId} />
        ) : (
          <EmptyState icon="▦" title="No text layer">
            This document exists only as an image. The pipeline reads text files, so it was flagged as an unsupported
            format and has no cases. Open the PDF to review it by hand.
          </EmptyState>
        )}
      </div>

      <aside className="side-pane">
        {kase ? (
          <FieldPanel
            cases={cases}
            caseIndex={activeIndex}
            onCase={changeCase}
            groups={groups}
            filter={filter}
            onFilter={setFilter}
            selectedFieldId={selectedFieldId}
            onSelect={setSelectedFieldId}
          />
        ) : (
          <NoCase detail={detail} flags={flags} />
        )}
        {kase ? (
        <footer className="key-hints" aria-label="Keyboard shortcuts">
          <span><kbd>J</kbd><kbd>K</kbd> next / previous field</span>
          {cases.length > 1 ? <span><kbd>[</kbd><kbd>]</kbd> switch case</span> : null}
          <span><kbd>Esc</kbd> clear</span>
        </footer>
        ) : null}
      </aside>
    </div>
  );
}

function NoCase({ detail, flags }: { detail: DocumentDetail; flags: string[] }) {
  if (detail.extraction === null) {
    return (
      <EmptyState icon="○" title="Not extracted yet">
        There is no output for this document. Run <code>./record.sh</code> with a key, or <code>./replay.sh</code> if it
        has a recording.
      </EmptyState>
    );
  }
  if (flags.includes(UNSUPPORTED)) {
    return (
      <EmptyState icon="▦" title="Unsupported format">
        The pipeline could not read this document, so it holds no cases.
      </EmptyState>
    );
  }
  const reasons = flags.filter((f) => f !== NO_EVENT);
  return (
    <EmptyState icon="✓" title="No case in this document">
      <p>
        {flags.includes(NO_EVENT)
          ? "The document reports no adverse event in a patient, so there is nothing to review here."
          : "No patient in this document meets all four minimum criteria: an identifiable patient, a reporter, a suspect drug and an adverse event."}
      </p>
      {reasons.length > 0 ? (
        <ul className="reason-list">
          {reasons.map((r) => (
            <li key={r}>{flagLabel(r)}</li>
          ))}
        </ul>
      ) : null}
    </EmptyState>
  );
}

function step(ids: string[], current: string | null, delta: 1 | -1): string | null {
  if (ids.length === 0) return null;
  const index = current === null ? -1 : ids.indexOf(current);
  if (index < 0) return delta === 1 ? ids[0]! : ids[ids.length - 1]!;
  return ids[Math.min(Math.max(index + delta, 0), ids.length - 1)]!;
}

function useKeyboard(actions: Record<"next" | "previous" | "nextCase" | "previousCase" | "clear", () => unknown>) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      if (target && (target.isContentEditable || (target.tagName === "INPUT" && !(target as HTMLInputElement).readOnly) || target.tagName === "TEXTAREA")) return;
      const action =
        event.key === "j" || event.key === "ArrowDown" ? actions.next
        : event.key === "k" || event.key === "ArrowUp" ? actions.previous
        : event.key === "]" ? actions.nextCase
        : event.key === "[" ? actions.previousCase
        : event.key === "Escape" ? actions.clear
        : null;
      if (action) {
        event.preventDefault();
        action();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [actions]);
}
