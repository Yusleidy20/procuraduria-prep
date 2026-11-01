import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { ApiError, getErrorMessage } from "./api";
import * as api from "./api";
import type { StudyChatResponse, StudyChatMessage, StudyConversationSummary } from "./types";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return {
    ...actual,
    explainTopic: vi.fn(),
    listStudyConversations: vi.fn(),
    createStudyConversation: vi.fn(),
    getStudyConversation: vi.fn(),
    renameStudyConversation: vi.fn(),
    deleteStudyConversation: vi.fn(),
    sendStudyMessage: vi.fn(),
    getCurrentUser: vi.fn(),
    getDocuments: vi.fn(),
    getOverallProgress: vi.fn(),
    getDueReviews: vi.fn(),
    getStudyModules: vi.fn(),
    getModuleTopics: vi.fn(),
    getTopicProgress: vi.fn(),
    getQuizzes: vi.fn(),
    uploadDocument: vi.fn(),
  };
});

const chatSummary: StudyConversationSummary = {
  id: "conversation-1",
  title: "Nueva conversación",
  createdAt: "2026-10-31T12:00:00Z",
  updatedAt: "2026-10-31T12:00:00Z",
  messageCount: 0,
};
let messageSequence = 0;

function chatMessage(
  id: string,
  role: StudyChatMessage["role"],
  content: string,
  sources: StudyChatMessage["sources"] = [],
  keyPoints: string | null = null,
): StudyChatMessage {
  return { id: `${id}-${messageSequence++}`, role, content, keyPoints, sources, createdAt: "2026-10-31T12:00:00Z" };
}

function chatResponse(
  question: string,
  explanation: string,
  sources: StudyChatMessage["sources"] = [],
  keyPoints: string | null = null,
): StudyChatResponse {
  return {
    conversation: { ...chatSummary, title: question, messageCount: 2 },
    userMessage: chatMessage("user-message", "USER", question),
    assistantMessage: chatMessage("assistant-message", "ASSISTANT", explanation, sources, keyPoints),
  };
}

describe("document consultation", () => {
  beforeEach(() => {
    window.localStorage.clear();
    messageSequence = 0;
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
    vi.mocked(api.listStudyConversations).mockReset().mockResolvedValue([]);
    vi.mocked(api.createStudyConversation).mockReset().mockResolvedValue(chatSummary);
    vi.mocked(api.getStudyConversation).mockReset().mockResolvedValue({
      conversation: chatSummary,
      messages: [],
    });
    vi.mocked(api.renameStudyConversation).mockReset().mockImplementation(
      async (_auth, conversationId, title) => ({ ...chatSummary, id: conversationId, title }),
    );
    vi.mocked(api.deleteStudyConversation).mockReset().mockResolvedValue(undefined);
    vi.mocked(api.sendStudyMessage).mockReset().mockImplementation(async (_auth, _id, request) =>
      chatResponse(request.content, "Respuesta de estudio."));
    vi.mocked(api.uploadDocument).mockReset().mockResolvedValue({
      id: "doc-uploaded",
      originalName: "Ley_2094_de_2021.pdf",
      contentType: "application/pdf",
      storageReference: "internal",
      status: "COMPLETED",
      processingError: null,
      uploadedAt: "2026-10-31T12:00:00Z",
    });
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

  it("saves a chat message, disables repeated submission, and renders its retrieved sources", async () => {
    let resolveMessage!: (value: StudyChatResponse) => void;
    vi.mocked(api.sendStudyMessage).mockImplementation(
      () => new Promise((resolve) => { resolveMessage = resolve; }),
    );
    const user = await connect();
    await user.type(screen.getByLabelText("Tu pregunta"), "Acción de tutela");
    await user.click(screen.getByText("Toda mi biblioteca"));
    await user.selectOptions(screen.getByLabelText("Limitar búsqueda a un documento (opcional)"), "doc-1");

    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(screen.getByRole("status")).toHaveTextContent("Buscando en tu biblioteca");
    expect(screen.getByRole("button", { name: /buscando/i })).toBeDisabled();
    expect(api.sendStudyMessage).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      "conversation-1",
      { content: "Acción de tutela", learnerLevel: "Inicial", sourceDocumentId: "doc-1" },
    );

    await act(async () => {
      resolveMessage(chatResponse(
        "Acción de tutela",
        "La tutela protege derechos fundamentales.\n\n- Es preferente\n- Es subsidiaria",
        [{
          documentId: "doc-1",
          documentName: "Guía de estudio.pdf",
          pageNumber: 12,
          chunkIndex: 3,
        }],
        "Protección inmediata",
      ));
    });
    expect(await screen.findByText("La tutela protege derechos fundamentales.")).toBeInTheDocument();
    expect(screen.getByRole("list").children).toHaveLength(2);
    const answer = screen.getByRole("heading", { name: "Respuesta" }).closest("article");
    expect(answer).not.toBeNull();
    const answerQueries = within(answer as HTMLElement);
    await user.click(answerQueries.getByText("Fuentes consultadas"));
    expect(answerQueries.getByText("Guía de estudio.pdf")).toBeInTheDocument();
    expect(answerQueries.getByText(/Página 12/)).toBeInTheDocument();
  });

  it("renders a saved answer without sources", async () => {
    vi.mocked(api.sendStudyMessage).mockImplementation(async (_auth, _id, request) =>
      chatResponse(request.content, "Una explicación general."));
    const user = await connect();
    await user.type(screen.getByLabelText("Tu pregunta"), "Debido proceso");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(await screen.findByText("Una explicación general.")).toBeInTheDocument();
    expect(screen.getByText(/no incluye referencias documentales/i)).toBeInTheDocument();
    expect(screen.getByText("Sin fuentes asociadas")).toBeInTheDocument();
  });

  it("continues the same persisted conversation and renders each answer's own sources", async () => {
    vi.mocked(api.sendStudyMessage)
      .mockImplementationOnce(async (_auth, _id, request) => chatResponse(
        request.content,
        "La falta disciplinaria se define en el material consultado.",
        [{
          documentId: "doc-1",
          documentName: "Guía de estudio.pdf",
          pageNumber: 8,
          chunkIndex: 2,
        }]))
      .mockImplementationOnce(async (_auth, _id, request) => chatResponse(
        request.content,
        "La diferencia depende de los elementos que describe la fuente.",
        [{
          documentId: "doc-1",
          documentName: "Guía de estudio.pdf",
          pageNumber: 9,
          chunkIndex: 3,
        }]));

    const user = await connect();
    await user.type(screen.getByLabelText("Tu pregunta"), "¿Qué es una falta disciplinaria?");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(await screen.findByText("La falta disciplinaria se define en el material consultado.")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Tu pregunta"), "¿Qué diferencias hay con lo anterior?");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(await screen.findByText("La diferencia depende de los elementos que describe la fuente.")).toBeInTheDocument();

    expect(api.sendStudyMessage).toHaveBeenLastCalledWith(
      { email: "student@example.com", password: "safe-password" },
      "conversation-1",
      {
        content: "¿Qué diferencias hay con lo anterior?",
        learnerLevel: "Inicial",
      },
    );
    expect(document.querySelectorAll(".ai-question")).toHaveLength(2);
    expect(screen.getAllByText("Guía de estudio.pdf")).toHaveLength(3);
  });

  it("loads saved history and supports renaming and deleting a conversation", async () => {
    const savedConversation = { ...chatSummary, title: "Ramas del poder público", messageCount: 2 };
    vi.mocked(api.listStudyConversations).mockResolvedValue([savedConversation]);
    vi.mocked(api.getStudyConversation).mockResolvedValue({
      conversation: savedConversation,
      messages: [
        chatMessage("saved-user", "USER", "Explícame las ramas del poder público"),
        chatMessage("saved-assistant", "ASSISTANT", "La biblioteca describe sus funciones."),
      ],
    });

    const user = await connect();
    expect(await screen.findByText("Explícame las ramas del poder público")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Renombrar conversación: Ramas del poder público" }));
    await user.clear(screen.getByLabelText("Nuevo título"));
    await user.type(screen.getByLabelText("Nuevo título"), "Organización del Estado");
    await user.click(screen.getByRole("button", { name: "Guardar título" }));
    expect(await screen.findByRole("button", { name: "Renombrar conversación: Organización del Estado" })).toBeInTheDocument();
    expect(api.renameStudyConversation).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      "conversation-1",
      "Organización del Estado",
    );

    await user.click(screen.getByRole("button", { name: "Eliminar conversación: Organización del Estado" }));
    await waitFor(() => expect(api.deleteStudyConversation).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      "conversation-1",
    ));
    expect(await screen.findByText("¿Qué quieres estudiar hoy?")).toBeInTheDocument();
  });

  it("treats the backend insufficient-context explanation as a valid response", async () => {
    vi.mocked(api.sendStudyMessage).mockImplementation(async (_auth, _id, request) =>
      chatResponse(request.content, "No encontré información suficiente en los documentos disponibles para explicar este tema.", [], "Agrega material relacionado."));
    const user = await connect();
    await user.type(screen.getByLabelText("Tu pregunta"), "Tema sin material");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(await screen.findByRole("status")).toHaveTextContent("No encontré información suficiente");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("maps HTTP and network errors to safe user-facing messages", async () => {
    vi.mocked(api.sendStudyMessage).mockRejectedValue(new ApiError("http", 503));
    const user = await connect();
    await user.type(screen.getByLabelText("Tu pregunta"), "Tema");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("no está disponible en este momento");
    expect(screen.queryByText(/Exception|stack trace|API key/i)).not.toBeInTheDocument();

    vi.mocked(api.sendStudyMessage).mockRejectedValue(new ApiError("network"));
    await user.type(screen.getByLabelText("Tu pregunta"), "Pregunta de seguimiento");
    await user.click(screen.getByRole("button", { name: /enviar/i }));
    const alerts = await screen.findAllByRole("alert");
    expect(alerts[alerts.length - 1]).toHaveTextContent("No fue posible conectar");
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
    expect(document.querySelector(".hero-art svg")).not.toBeNull();
    expect(screen.getByRole("region", { name: "Resumen de preparación" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Agenda de hoy" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Tu progreso" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Accesos rápidos" })).toBeInTheDocument();
    expect(document.querySelectorAll(".dashboard-stat")).toHaveLength(4);
    expect(screen.getByText("0% de sesiones sugeridas marcadas en este dispositivo")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Temario" }));
    expect(screen.getByRole("heading", { name: "Temario" })).toBeInTheDocument();
    expect(screen.getByText(/Empleo Técnico Investigador.*código 4TI-15/i)).toBeInTheDocument();
    expect(screen.getByText("Código General Disciplinario")).toBeInTheDocument();
    expect(screen.getByText(/PDF “mi concurso\(2\)\.pdf” no está disponible/i)).toBeInTheDocument();
  });

  it("searches real topics and documents and opens the corresponding study views", async () => {
    vi.mocked(api.getDocuments).mockResolvedValue({
      content: [
        {
          id: "doc-1",
          originalName: "Guía de estudio.pdf",
          contentType: "application/pdf",
          storageReference: "internal",
          status: "COMPLETED",
          processingError: null,
          uploadedAt: "2026-10-31T12:00:00Z",
        },
        {
          id: "doc-2",
          originalName: "Notas pendientes.txt",
          contentType: "text/plain",
          storageReference: "internal",
          status: "PROCESSING",
          processingError: null,
          uploadedAt: "2026-10-31T12:05:00Z",
        },
      ],
      totalPages: 1,
      totalElements: 2,
      number: 0,
      size: 100,
    });
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await screen.findByRole("heading", { name: "Hola, Estudiante" });

    const search = screen.getByRole("searchbox", { name: "Buscar temas o documentos" });
    await user.type(search, "Gestión pública");
    await user.click(screen.getByRole("button", { name: /Gestión pública y funcionamiento del Estado/ }));
    expect(screen.getByRole("heading", { name: "Estudiar con IA" })).toBeInTheDocument();
    expect(screen.getByLabelText("Tu pregunta")).toHaveValue("Gestión pública y funcionamiento del Estado");

    await user.clear(search);
    await user.type(search, "Guía de estudio");
    await user.click(screen.getByRole("button", { name: /Guía de estudio\.pdf/ }));
    await user.click(screen.getByText("Documento específico"));
    expect(screen.getByLabelText("Limitar búsqueda a un documento (opcional)")).toHaveValue("doc-1");

    await user.click(screen.getByRole("button", { name: "Mis documentos" }));
    expect(screen.getByText("Notas pendientes.txt")).toBeInTheDocument();
    expect(screen.getByText("Procesando")).toBeInTheDocument();
  });

  it("sends a binary PDF without trying to decode it as plain text and rejects empty files locally", async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await screen.findByRole("heading", { name: "Hola, Estudiante" });
    await user.click(screen.getByRole("button", { name: "Mis documentos" }));

    const pdf = new File(["%PDF-1.7\nbinary PDF content"], "Ley_2094_de_2021.pdf", { type: "application/x-pdf" });
    await user.upload(screen.getByLabelText("Seleccionar documentos para subir"), pdf);
    await waitFor(() => expect(api.uploadDocument).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      pdf,
    ));

    const emptyPdf = new File([], "empty.pdf", { type: "application/pdf" });
    await user.upload(screen.getByLabelText("Seleccionar documentos para subir"), emptyPdf);
    expect(await screen.findByRole("alert")).toHaveTextContent("el archivo está vacío");
    expect(api.uploadDocument).toHaveBeenCalledTimes(1);
  });

  it("leaves PDF content validation to the backend PDF parser instead of requiring a local header prefix", async () => {
    vi.mocked(api.uploadDocument).mockRejectedValueOnce(new ApiError(
      "http",
      400,
      "No fue posible leer el PDF; puede estar dañado o protegido con contraseña",
    ));
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText("Correo electrónico"), "student@example.com");
    await user.type(screen.getByLabelText("Contraseña"), "safe-password");
    await user.click(screen.getByRole("button", { name: "Entrar a mi espacio" }));
    await screen.findByRole("heading", { name: "Hola, Estudiante" });
    await user.click(screen.getByRole("button", { name: "Mis documentos" }));

    const fileWithoutHeaderAtByteZero = new File(["document tag%PDF-1.7\n"], "Ley_2094_de_2021.pdf", { type: "application/pdf" });
    await user.upload(screen.getByLabelText("Seleccionar documentos para subir"), fileWithoutHeaderAtByteZero);

    await waitFor(() => expect(api.uploadDocument).toHaveBeenCalledWith(
      { email: "student@example.com", password: "safe-password" },
      fileWithoutHeaderAtByteZero,
    ));
    expect(await screen.findByRole("alert")).toHaveTextContent("No fue posible leer el PDF");
  });

  it("maps backend PDF extraction errors to safe actionable messages", () => {
    expect(getErrorMessage(new ApiError("http", 400, "El PDF no contiene texto extraíble; si es escaneado, requiere OCR")))
      .toMatch(/necesita OCR/i);
    expect(getErrorMessage(new ApiError("http", 400, "No fue posible leer el PDF; puede estar dañado o protegido con contraseña")))
      .toMatch(/dañado o protegido/i);
    expect(getErrorMessage(new ApiError("http", 400, "internal exception at /srv/private")))
      .toMatch(/archivo no es válido/i);
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
