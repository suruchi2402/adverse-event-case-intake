import type { ReactNode } from "react";

export function LoadingState({ label, lines = 6 }: { label: string; lines?: number }) {
  return (
    <div className="state state-loading" role="status" aria-live="polite">
      <span className="visually-hidden">{label}</span>
      {Array.from({ length: lines }, (_, i) => (
        <span key={i} className="skeleton" style={{ width: `${92 - ((i * 17) % 40)}%` }} />
      ))}
    </div>
  );
}

export function ErrorState({ title, message, onRetry }: { title: string; message: string; onRetry?: () => void }) {
  return (
    <div className="state state-error" role="alert">
      <div className="state-icon" aria-hidden="true">!</div>
      <h2 className="state-title">{title}</h2>
      <p className="state-body">{message}</p>
      {onRetry ? (
        <button type="button" className="button" onClick={onRetry}>
          Try again
        </button>
      ) : null}
    </div>
  );
}

export function EmptyState({ icon, title, children }: { icon: string; title: string; children?: ReactNode }) {
  return (
    <div className="state state-empty">
      <div className="state-icon" aria-hidden="true">
        {icon}
      </div>
      <h2 className="state-title">{title}</h2>
      {children ? <div className="state-body">{children}</div> : null}
    </div>
  );
}
