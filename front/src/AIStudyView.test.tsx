import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AIStudyView } from "./AIStudyView";
import * as api from "./api";
import type { StudyChatResponse, StudyConversationSummary } from "./types";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return {
    ...actual,
    createStudyConversation: vi.fn(),
    deleteStudyConversation: vi.fn(),
    getStudyConversation: vi.fn(),
    listStudyConversations: vi.fn(),
    renameStudyConversation: vi.fn(),
    sendStudyMessage: vi.fn(),
  };
});

const auth = { email: "student@example.com", password: "safe-password" };
const conversation: StudyConversationSummary = {
  id: "conversation-1",
  title: "Nueva conversación",
  createdAt: "2026-10-31T12:00:00Z",
  updatedAt: "2026-10-31T12:00:00Z",
  messageCount: 0,
};

function response(question: string): StudyChatResponse {
  return {
    conversation: { ...conversation, title: question, messageCount: 2 },
    userMessage: {
      id: "user-message",
      role: "USER",
      content: question,
      keyPoints: null,
      sources: [],
      createdAt: "2026-10-31T12:00:00Z",
    },
    assistantMessage: {
      id: "assistant-message",
      role: "ASSISTANT",
      content: "La fuente describe el tema consultado.",
      keyPoints: null,
      sources: [{
        documentId: "doc-1",
        documentName: "Guía de estudio.pdf",
        pageNumber: 8,
        chunkIndex: 2,
      }],
      createdAt: "2026-10-31T12:00:00Z",
    },
  };
}

describe("AIStudyView", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.listStudyConversations).mockResolvedValue([]);
    vi.mocked(api.createStudyConversation).mockResolvedValue(conversation);
    vi.mocked(api.getStudyConversation).mockResolvedValue({ conversation, messages: [] });
    vi.mocked(api.sendStudyMessage).mockImplementation(async (_auth, _id, request) =>
      response(request.content));
  });

  it("creates a conversation, searches the full library and shows sources attached to the answer", async () => {
    const user = userEvent.setup();
    render(
      <AIStudyView auth={auth} documents={[]} initialTopic="" initialSourceDocumentId="" />,
    );

    await user.type(screen.getByLabelText("Tu pregunta"), "Explícame la organización del Estado");
    await user.click(screen.getByRole("button", { name: /enviar/i }));

    expect(await screen.findByText("La fuente describe el tema consultado.")).toBeInTheDocument();
    expect(api.createStudyConversation).toHaveBeenCalledWith(auth);
    expect(api.sendStudyMessage).toHaveBeenCalledWith(auth, conversation.id, {
      content: "Explícame la organización del Estado",
      learnerLevel: "Inicial",
    });

    await user.click(screen.getByText("Fuentes consultadas"));
    expect(await screen.findByText("Guía de estudio.pdf")).toBeInTheDocument();
    expect(screen.getByText(/Página 8/)).toBeInTheDocument();
  });

  it("continues an existing conversation with a follow-up question", async () => {
    const previousMessage = {
      id: "previous-message",
      role: "ASSISTANT" as const,
      content: "Las ramas del poder público cumplen funciones distintas.",
      keyPoints: null,
      sources: [],
      createdAt: "2026-10-31T12:00:00Z",
    };
    const savedConversation = { ...conversation, title: "Ramas del poder público", messageCount: 1 };
    vi.mocked(api.listStudyConversations).mockResolvedValue([savedConversation]);
    vi.mocked(api.getStudyConversation).mockResolvedValue({
      conversation: savedConversation,
      messages: [previousMessage],
    });

    const user = userEvent.setup();
    render(<AIStudyView auth={auth} documents={[]} initialTopic="" initialSourceDocumentId="" />);
    expect(await screen.findByText(previousMessage.content)).toBeInTheDocument();

    await user.type(screen.getByLabelText("Tu pregunta"), "¿Me das un ejemplo?");
    await user.click(screen.getByRole("button", { name: /enviar/i }));

    expect(await screen.findByText("La fuente describe el tema consultado.")).toBeInTheDocument();
    expect(api.sendStudyMessage).toHaveBeenCalledWith(auth, savedConversation.id, {
      content: "¿Me das un ejemplo?",
      learnerLevel: "Inicial",
    });
  });
});
