import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { catalog, fakeFetch, IDS, json } from "./test/data";

const label = (id: string) => catalog.fields.find((f) => f.id === id)!.label;

beforeEach(() => {
  vi.stubGlobal("fetch", vi.fn(fakeFetch()));
});
afterEach(() => {
  vi.unstubAllGlobals();
});

async function openApp(hash = "") {
  window.location.hash = hash;
  render(<App />);
  await screen.findByRole("navigation", { name: "Documents" });
}

describe("App", () => {
  it("shows a loading state, then the document list and the first document", async () => {
    render(<App />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading documents");
    expect(await screen.findByRole("heading", { level: 1, name: "Two patients" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /D06/ })).toHaveTextContent("No adverse event");
  });

  it("groups fields by catalog section and shows every catalog field", async () => {
    await openApp("#/D01");
    await screen.findByRole("heading", { level: 1 });
    for (const section of catalog.sections) {
      expect(screen.getByRole("heading", { level: 3, name: new RegExp(section.label) })).toBeInTheDocument();
    }
    for (const field of catalog.fields) {
      expect(screen.getByText(field.label, { selector: ".field-label" })).toBeInTheDocument();
    }
  });

  it("highlights the quote of the selected field in the document", async () => {
    const user = userEvent.setup();
    await openApp("#/D01");
    await user.click(await screen.findByRole("button", { name: new RegExp(label(IDS.date)) }));
    const selected = document.querySelector("mark.is-selected");
    expect(selected).toHaveTextContent("Started drug on 2 March 2026");
    expect(selected).toHaveAttribute("data-anchor", "true");
  });

  it("selects a field when its highlighted quote is clicked", async () => {
    const user = userEvent.setup();
    await openApp("#/D01");
    await screen.findByRole("heading", { level: 1 });
    const mark = [...document.querySelectorAll("mark")].find((m) => m.textContent?.startsWith("Rash"))!;
    await user.click(mark);
    expect(screen.getByRole("button", { name: new RegExp(label(IDS.list)), pressed: true })).toBeInTheDocument();
    expect(mark).toHaveClass("is-selected");
  });

  it("filters to the fields that need review", async () => {
    const user = userEvent.setup();
    await openApp("#/D01");
    await user.click(await screen.findByRole("radio", { name: /Needs review/ }));
    const rows = document.querySelectorAll(".field-row");
    expect(rows).toHaveLength(3);
    expect(screen.getByText("54 or 45")).toBeInTheDocument();
  });

  it("moves between fields with the keyboard", async () => {
    const user = userEvent.setup();
    await openApp("#/D01");
    await screen.findByRole("heading", { level: 1 });
    await user.keyboard("j");
    const first = catalog.fields.find((f) => f.section === catalog.sections[0]!.id)!;
    expect(screen.getByRole("button", { name: new RegExp(first.label), pressed: true })).toBeInTheDocument();
    await user.keyboard("{Escape}");
    expect(screen.queryByRole("button", { pressed: true })).not.toBeInTheDocument();
  });

  it("switches between the cases of a document", async () => {
    const user = userEvent.setup();
    await openApp("#/D01");
    const tabs = await screen.findByRole("tablist", { name: "Cases in this document" });
    await user.click(within(tabs).getByRole("tab", { name: /Infant/ }));
    expect(within(tabs).getByRole("tab", { name: /Infant/ })).toHaveAttribute("aria-selected", "true");
    expect(window.location.hash).toBe("#/D01/1");
    expect(document.querySelectorAll("mark")).toHaveLength(0);
  });

  it("presents a document without a case as intentional", async () => {
    await openApp("#/D06");
    expect(await screen.findByRole("heading", { name: "No case in this document" })).toBeInTheDocument();
    expect(screen.getByText(/reports no adverse event/)).toBeInTheDocument();
    expect(screen.getByText("Capsules were discoloured. No patient received them.")).toBeInTheDocument();
  });

  it("explains an image-only document", async () => {
    await openApp("#/S03");
    expect(await screen.findByRole("heading", { name: "No text layer" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Unsupported format" })).toBeInTheDocument();
  });

  it("shows an error with a retry when the backend is down", async () => {
    const user = userEvent.setup();
    const failing = vi.fn(fakeFetch({ "/api/documents": () => json({ error: "boom" }, 500) }));
    vi.stubGlobal("fetch", failing);
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent("boom");
    vi.stubGlobal("fetch", vi.fn(fakeFetch()));
    await user.click(screen.getByRole("button", { name: "Try again" }));
    await waitFor(() => expect(screen.getByRole("navigation", { name: "Documents" })).toBeInTheDocument());
  });

  it("shows an error for a document that fails to load", async () => {
    vi.stubGlobal("fetch", vi.fn(fakeFetch({ "/api/documents/D01": () => json({ error: "disk error" }, 500) })));
    await openApp("#/D01");
    expect(await screen.findByRole("alert")).toHaveTextContent("disk error");
  });
});
