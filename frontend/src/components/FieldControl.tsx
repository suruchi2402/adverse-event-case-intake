import type { ReactElement } from "react";
import type { CatalogField, FieldResult, FieldType, FieldValue } from "../api/types";

/**
 * The control for a field, chosen by its catalog type. Controls are read-only: they show the model's value in the
 * form a reviewer would edit it (all options of a coded field, a date picker for a date, a multi-select for
 * coded_multi). Nothing here depends on a particular field.
 */
interface ControlProps {
  field: CatalogField;
  value: FieldValue | undefined;
  empty: boolean;
  labelledBy: string;
}

function TextControl({ value, empty, labelledBy }: ControlProps) {
  return (
    <input
      className="control control-text"
      type="text"
      readOnly
      aria-labelledby={labelledBy}
      value={empty ? "" : String(value ?? "")}
      placeholder={empty ? "Not stated" : undefined}
    />
  );
}

function NumberControl({ field, value, empty, labelledBy }: ControlProps) {
  return (
    <input
      className="control control-number"
      type="number"
      readOnly
      aria-labelledby={labelledBy}
      min={field.validation?.min}
      max={field.validation?.max}
      step={field.type === "integer" ? 1 : "any"}
      value={empty || value === null || value === undefined ? "" : String(value)}
      placeholder={empty ? "—" : undefined}
    />
  );
}

function DateControl({ value, empty, labelledBy }: ControlProps) {
  return (
    <input
      className="control control-date"
      type="date"
      readOnly
      aria-labelledby={labelledBy}
      value={empty || typeof value !== "string" ? "" : value}
    />
  );
}

function OptionsControl({ field, value, empty, labelledBy, multiple }: ControlProps & { multiple: boolean }) {
  const selected = new Set(empty ? [] : Array.isArray(value) ? value : value == null ? [] : [String(value)]);
  return (
    <div
      className={`control control-options${multiple ? " is-multi" : ""}`}
      role="listbox"
      aria-labelledby={labelledBy}
      aria-multiselectable={multiple}
      aria-readonly="true"
    >
      {(field.options ?? []).map((option) => {
        const isSelected = selected.has(option.code);
        return (
          <span
            key={option.code}
            role="option"
            aria-selected={isSelected}
            className={`option${isSelected ? " is-selected" : ""}`}
          >
            {multiple ? <span className="option-check" aria-hidden="true" /> : null}
            {option.label}
          </span>
        );
      })}
    </div>
  );
}

function ListControl({ value, empty, labelledBy }: ControlProps) {
  const items = !empty && Array.isArray(value) ? value : [];
  return (
    <ul className="control control-list" aria-labelledby={labelledBy}>
      {empty ? <li className="tag tag-placeholder">Not stated</li> : null}
      {!empty && items.length === 0 ? <li className="tag tag-none">None, as stated</li> : null}
      {items.map((item) => (
        <li key={item} className="tag">
          {item}
        </li>
      ))}
    </ul>
  );
}

const CONTROLS: Record<FieldType, (props: ControlProps) => ReactElement> = {
  string: TextControl,
  integer: NumberControl,
  number: NumberControl,
  date: DateControl,
  coded: (props) => <OptionsControl {...props} multiple={false} />,
  coded_multi: (props) => <OptionsControl {...props} multiple />,
  string_list: ListControl,
};

export function FieldControl({ field, result, labelledBy }: { field: CatalogField; result: FieldResult; labelledBy: string }) {
  const Control = CONTROLS[field.type];
  if (!Control) {
    return <span className="control-unsupported">Unsupported field type “{field.type}”</span>;
  }
  return (
    <Control
      field={field}
      value={result.value}
      empty={result.status !== "extracted"}
      labelledBy={labelledBy}
    />
  );
}
