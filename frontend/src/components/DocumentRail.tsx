import type { DocumentSummary } from "../api/types";
import { flagLabel } from "../lib/review";

interface Props {
  documents: DocumentSummary[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

export function DocumentRail({ documents, selectedId, onSelect }: Props) {
  return (
    <nav className="rail" aria-label="Documents">
      <h2 className="rail-title">
        Documents <span className="rail-count">{documents.length}</span>
      </h2>
      <ul className="rail-list">
        {documents.map((doc) => (
          <li key={doc.id}>
            <button
              type="button"
              className={`rail-item${doc.id === selectedId ? " is-active" : ""}`}
              aria-current={doc.id === selectedId ? "page" : undefined}
              onClick={() => onSelect(doc.id)}
            >
              <span className="rail-item-top">
                <span className="rail-id">{doc.id}</span>
                <span className={`rail-cases${doc.caseCount === 0 ? " is-zero" : ""}`}>
                  {!doc.hasOutput ? "not extracted" : doc.caseCount === 1 ? "1 case" : `${doc.caseCount} cases`}
                </span>
              </span>
              <span className="rail-doc-title">{doc.title}</span>
              {doc.flags.length > 0 ? (
                <span className="rail-flags">
                  {doc.flags.map((flag) => (
                    <span key={flag} className="flag flag-small" title={flag}>
                      {flagLabel(flag)}
                    </span>
                  ))}
                </span>
              ) : null}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  );
}
