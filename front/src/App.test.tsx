import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { ApiError, getErrorMessage } from "./api";
import * as api from "./api";
import type { TopicExplanationResponse } from "./types";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return {
    ...actual,
    explainTopic: vi.fn(),
    getCurrentUser: vi.fn(),
    getDocuments: vi.fn(),
    getOverallProgress: vi.fn(),
    getDueReviews: vi.fn(),
    getStudyModules: vi.fn(),
    getModuleTopics: vi.fn(),
    getTopicProgress: vi.fn(),
    getQuizzes: vi.fn(),
  };
});

describe("document consultation", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.mocked(api.getCurrentUser).mockReset().mockResolvedValue({
      id: "user-1",
      name: "Estudiante",
      email: "student@example.com",
    });
    vi.mocked(api.getDocuments).mockReset().mockResolvedValue({
      content: [{
        id: "doc-1",
        originalName: "Guía de estudio.pdf",
        contentType: "application/pdf",
        storageReference: "internal",
        status: "COMPLETED",
        processingError: null,
        uploadedAt: "2026-10-31T12:00:00Z",
      }],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
    });
    vi.mocked(api.getOverallProgress).mockReset().mockResolvedValue({
      topicCount: 0,
      completedQuizAttempts: 0,
      quizAnswers: 0,
      correctQuizAnswers: 0,
      flashcardCount: 0,
      flashcardReviews: 0,
      rememberedReviews: 0,
      masteryPercentage: null,
    });
    vi.mocked(api.getDueReviews).mockReset().mockResolvedValue([]);
    vi.mocked(api.getStudyModules).mockReset().mockResolvedValue([]);
    vi.mocked(api.getModuleTopics).mockReset().mockResolvedValue([]);
    vi.mocked(api.getTopicProgress).mockReset();
    vi.mocked(api.getQuizzes).mockReset().mockResolvedValue([]);
    vi.mocked(api.explainTopic).mockReset();
  });

  async function connect(user = userEvent.setup()) {
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await screen.findByRole("heading", { name: "Hola, Estudiante" });
    await user.click(screen.getByRole("button", { name: "Estudiar con IA" }));
    await screen.findByRole("heading", { name: "Estudiar con IA" });
    return user;
  }

  it("shows loading state, disables repeated submission, and then renders an answer with sources", async () => {
    let resolveExplanation!: (value: TopicExplanationResponse) => void;
    vi.mocked(api.explainTopic).mockImplementation(
      () => new Promise((resolve) => { resolveExplanation = resolve; }),
    );
    const user = await connect();
    await user.type(screen.getByLabelText("Tema o pregunta"), "Acción de tutela");
    await user.selectOptions(screen.getByLabelText("Documento"), "doc-1");

    await user.click(screen.getByRole("button", { name: /consultar/i }));
    expect(screen.getByRole("status")).toHaveTextContent("Buscando evidencia");
    expect(screen.getByRole("button", { name: /consultando documentos/i })).toBeDisabled();
    expect(api.explainTopic).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      { topicName: "Acción de tutela", learnerLevel: "Inicial", sourceDocumentId: "doc-1" },
    );

    await act(async () => {
      resolveExplanation({
        explanation: "La tutela protege derechos fundamentales.\n\n- Es preferente\n- Es subsidiaria",
        keyPoints: "Protección inmediata",
        sources: [{
          documentId: "doc-1",
          documentName: "Guía de estudio.pdf",
          pageNumber: 12,
          chunkIndex: 3,
        }],
      });
    });
    expect(await screen.findByText("La tutela protege derechos fundamentales.")).toBeInTheDocument();
    expect(screen.getByRole("list").children).toHaveLength(2);
    const answer = screen.getByRole("heading", { name: "Acción de tutela" }).closest("article");
    expect(answer).not.toBeNull();
    const answerQueries = within(answer as HTMLElement);
    await user.click(answerQueries.getByText("Fuentes consultadas"));
    expect(answerQueries.getByText("Guía de estudio.pdf")).toBeInTheDocument();
    expect(answerQueries.getByText(/Página 12/)).toBeInTheDocument();
  });

  it("renders a valid explanation without sources", async () => {
    vi.mocked(api.explainTopic).mockResolvedValue({
      explanation: "Una explicación general.",
      keyPoints: "",
      sources: [],
    });
    const user = await connect();
    await user.type(screen.getByLabelText("Tema o pregunta"), "Debido proceso");
    await user.click(screen.getByRole("button", { name: /consultar/i }));
    expect(await screen.findByText("Una explicación general.")).toBeInTheDocument();
    expect(screen.getByText(/no incluye referencias documentales/i)).toBeInTheDocument();
    expect(screen.getByText("Sin fuentes asociadas")).toBeInTheDocument();
  });

  it("treats the backend insufficient-context explanation as a valid response", async () => {
    vi.mocked(api.explainTopic).mockResolvedValue({
      explanation: "No encontré información suficiente en los documentos disponibles para explicar este tema.",
      keyPoints: "Adjunta o selecciona un documento que contenga información sobre el tema.",
      sources: [],
    });
    const user = await connect();
    await user.type(screen.getByLabelText("Tema o pregunta"), "Tema sin material");
    await user.click(screen.getByRole("button", { name: /consultar/i }));
    expect(await screen.findByRole("status")).toHaveTextContent("No encontré información suficiente");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("maps HTTP and network errors to safe user-facing messages", async () => {
    vi.mocked(api.explainTopic).mockRejectedValue(new ApiError("http", 503));
    const user = await connect();
    await user.type(screen.getByLabelText("Tema o pregunta"), "Tema");
    await user.click(screen.getByRole("button", { name: /consultar/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("no está disponible en este momento");
    expect(screen.queryByText(/Exception|stack trace|API key/i)).not.toBeInTheDocument();

    vi.mocked(api.explainTopic).mockRejectedValue(new ApiError("network"));
    await user.click(screen.getByRole("button", { name: /consultar/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("No fue posible conectar");
  });

  it("handles authentication failure without revealing backend details", async () => {
    vi.mocked(api.getCurrentUser).mockRejectedValue(new ApiError("http", 401));
    render(<App />);
    await userEvent.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await userEvent.type(screen.getByLabelText("Contraseña"), "wrong");
    fireEvent.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Verifica tu correo y contraseña"));
    expect(screen.queryByText(/stack trace|internal server/i)).not.toBeInTheDocument();
  });

  it("shows a clear timeout message", () => {
    expect(getErrorMessage(new ApiError("timeout"))).toMatch(/tardó demasiado/i);
    expect(getErrorMessage(new ApiError("http", 422))).toMatch(/información suficiente/i);
  });

  it("opens the dashboard after login and navigates to the convocatoria syllabus", async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));

    expect(await screen.findByRole("heading", { name: "Hola, Estudiante" })).toBeInTheDocument();
    expect(screen.getByText("0% de sesiones sugeridas marcadas en este dispositivo")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Temario" }));
    expect(screen.getByRole("heading", { name: "Temario" })).toBeInTheDocument();
    expect(screen.getByText(/Empleo Técnico Investigador.*código 4TI-15/i)).toBeInTheDocument();
    expect(screen.getByText("Código General Disciplinario")).toBeInTheDocument();
    expect(screen.getByText(/PDF “mi concurso\(2\)\.pdf” no está disponible/i)).toBeInTheDocument();
  });

  it("persists only locally marked study sessions and renders real backend topic statistics", async () => {
    vi.mocked(api.getStudyModules).mockResolvedValue([{
      id: "module-1",
      name: "Área jurídica",
      description: null,
      displayOrder: 1,
      status: "ACTIVE",
      createdAt: "2026-10-31T12:00:00Z",
    }]);
    vi.mocked(api.getModuleTopics).mockResolvedValue([{
      id: "topic-1",
      name: "Derecho de prueba",
      description: null,
      moduleId: "module-1",
      moduleName: "Área jurídica",
      difficulty: "INTERMEDIATE",
      createdAt: "2026-10-31T12:00:00Z",
    }]);
    vi.mocked(api.getTopicProgress).mockResolvedValue({
      topicId: "topic-1",
      topicName: "Derecho de prueba",
      completedQuizAttempts: 2,
      quizAnswers: 10,
      correctQuizAnswers: 7,
      flashcardCount: 0,
      flashcardReviews: 0,
      rememberedReviews: 0,
      masteryPercentage: 70,
    });

    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await screen.findByRole("heading", { name: "Hola, Estudiante" });
    await user.click(screen.getByRole("button", { name: "Mi plan de estudio" }));
    await user.click(screen.getAllByRole("button", { name: "Marcar completada" })[0]);
    expect(window.localStorage.getItem("procuraduria-prep.study-plan.v1")).toContain("gestion-publica");

    await user.click(screen.getByRole("button", { name: "Mi progreso" }));
    expect(screen.getByText("Derecho de prueba")).toBeInTheDocument();
    const topicStats = document.querySelector(".actual-topics-panel");
    expect(topicStats).not.toBeNull();
    expect(within(topicStats as HTMLElement).getByText("70%")).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.textContent?.replace(/\s/g, "") === "1/14")).toBeInTheDocument();
    expect(screen.getByText("No registrado por el backend")).toBeInTheDocument();
  });
});
