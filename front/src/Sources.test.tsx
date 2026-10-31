import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Sources } from "./Sources";
import * as api from "./api";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, getDocumentText: vi.fn() };
});

const auth = { email: "student@example.com", password: "pass" };

function getSourcesSummary(container: HTMLElement) {
  const panel = container.querySelector("details.sources-panel");
  expect(panel).not.toBeNull();
  return within(panel as HTMLElement).getByText("Fuentes consultadas");
}

describe("Sources", () => {
  beforeEach(() => {
    vi.mocked(api.getDocumentText).mockReset();
  });

  it("shows a clear empty state when an answer has no sources", () => {
    render(<Sources sources={[]} auth={auth} />);
    expect(screen.getByText(/no incluye referencias documentales/i)).toBeInTheDocument();
  });

  it("shows the document and page from the backend and expands/collapses its sources", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <Sources
        auth={auth}
        sources={[{
          documentId: "doc-1",
          documentName: "Constitución.pdf",
          pageNumber: 14,
          chunkIndex: 2,
        }]}
      />,
    );

    const section = getSourcesSummary(container);
    const sourcesPanel = container.querySelector("details.sources-panel");
    await user.click(section);
    expect(sourcesPanel).toHaveAttribute("open");
    expect(screen.getByText("Constitución.pdf")).toBeInTheDocument();
    expect(screen.getByText(/Página 14/)).toBeInTheDocument();
    const sourceCard = screen.getByText("Constitución.pdf").closest("details");
    expect(sourceCard).not.toBeNull();
    expect(within(sourceCard as HTMLElement).getByText("Referencia: página 14 · Fragmento 2")).toBeInTheDocument();

    await user.click(section);
    expect(sourcesPanel).not.toHaveAttribute("open");
  });

  it("does not invent a page when the backend returns null", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <Sources
        auth={auth}
        sources={[{
          documentId: "doc-2",
          documentName: "Ley 1952.txt",
          pageNumber: null,
          chunkIndex: 0,
        }]}
      />,
    );
    await user.click(getSourcesSummary(container));

    expect(screen.getByText(/Sin número de página/)).toBeInTheDocument();
    expect(screen.getByText(/página no disponible/)).toBeInTheDocument();
  });

  it("consults document text using its existing API endpoint", async () => {
    const user = userEvent.setup();
    vi.mocked(api.getDocumentText).mockResolvedValue({
      documentId: "doc-3",
      originalName: "Manual.pdf",
      text: "Contenido extraído",
    });
    const { container } = render(
      <Sources
        auth={auth}
        sources={[{
          documentId: "doc-3",
          documentName: "Manual.pdf",
          pageNumber: 3,
          chunkIndex: 4,
        }]}
      />,
    );
    await user.click(getSourcesSummary(container));
    await user.click(screen.getByText("Manual.pdf"));
    await user.click(screen.getByRole("button", { name: "Consultar texto del documento" }));

    expect(api.getDocumentText).toHaveBeenCalledWith(auth, "doc-3");
    expect(await screen.findByText("Contenido extraído")).toBeInTheDocument();
    expect(screen.getByText(/no proporciona el extracto exacto/i)).toBeInTheDocument();
  });
});
