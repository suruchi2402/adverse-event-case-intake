import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { CatalogField, FieldResult } from "../api/types";
import { fieldOfType } from "../test/data";
import { FieldControl } from "./FieldControl";

function renderControl(field: CatalogField, result: FieldResult) {
  return render(
    <div>
      <span id="label">{field.label}</span>
      <FieldControl field={field} result={result} labelledBy="label" />
    </div>,
  );
}

describe("FieldControl renders the control the catalog type asks for", () => {
  it("coded: every option, the chosen one selected", () => {
    const field = fieldOfType("coded");
    const chosen = field.options![1]!;
    renderControl(field, { status: "extracted", value: chosen.code, confidence: "high" });
    const listbox = screen.getByRole("listbox", { name: field.label });
    expect(listbox).toHaveAttribute("aria-multiselectable", "false");
    const options = within(listbox).getAllByRole("option");
    expect(options.map((o) => o.textContent)).toEqual(field.options!.map((o) => o.label));
    expect(within(listbox).getByRole("option", { selected: true })).toHaveTextContent(chosen.label);
  });

  it("coded_multi: a multi-select with several selected", () => {
    const field = fieldOfType("coded_multi");
    const codes = field.options!.slice(0, 2).map((o) => o.code);
    renderControl(field, { status: "extracted", value: codes, confidence: "medium" });
    const listbox = screen.getByRole("listbox", { name: field.label });
    expect(listbox).toHaveAttribute("aria-multiselectable", "true");
    expect(within(listbox).getAllByRole("option", { selected: true })).toHaveLength(2);
  });

  it("date: a date input holding the ISO value", () => {
    const field = fieldOfType("date");
    const { container } = renderControl(field, { status: "extracted", value: "2026-03-04", confidence: "high" });
    const input = container.querySelector("input")!;
    expect(input).toHaveAttribute("type", "date");
    expect(input).toHaveValue("2026-03-04");
  });

  it("integer: a number input with the catalog range", () => {
    const field = fieldOfType("integer");
    renderControl(field, { status: "extracted", value: 54, confidence: "high" });
    const input = screen.getByRole("spinbutton", { name: field.label });
    expect(input).toHaveValue(54);
    if (field.validation?.max !== undefined) expect(input).toHaveAttribute("max", String(field.validation.max));
  });

  it("string_list: tags, and an explicit empty list reads as none", () => {
    const field = fieldOfType("string_list");
    const { rerender } = renderControl(field, { status: "extracted", value: ["rash", "angioedema"], confidence: "high" });
    expect(screen.getAllByRole("listitem").map((li) => li.textContent)).toEqual(["rash", "angioedema"]);
    rerender(<FieldControl field={field} result={{ status: "extracted", value: [], confidence: "high" }} labelledBy="label" />);
    expect(screen.getByText("None, as stated")).toBeInTheDocument();
  });

  it("unknown fields show an empty control, never a value", () => {
    const field = fieldOfType("string");
    renderControl(field, { status: "unknown", value: "should not show" });
    expect(screen.getByRole("textbox", { name: field.label })).toHaveValue("");
    expect(screen.getByPlaceholderText("Not stated")).toBeInTheDocument();
  });
});
