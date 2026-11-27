import { RISK_LABEL, RISK_SHORT, type Risk } from "../lib/review";

/** Status and confidence in one mark: shape and colour differ per risk, so it reads at a glance and without colour. */
export function RiskBadge({ risk, compact = false }: { risk: Risk; compact?: boolean }) {
  return (
    <span className={`risk-badge risk-${risk}`} title={RISK_LABEL[risk]}>
      <span className="risk-glyph" aria-hidden="true" />
      {compact ? null : <span className="risk-text">{RISK_SHORT[risk]}</span>}
      {compact ? <span className="visually-hidden">{RISK_LABEL[risk]}</span> : null}
    </span>
  );
}
