import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  createStudyConversation,
  deleteStudyConversation,
  explainTopic,
  getErrorMessage,
  getStudyConversation,
  listStudyConversations,
  renameStudyConversation,
  sendStudyMessage,
  uploadDocument,
} from "./api";

const auth = { email: "student@example.com", password: "pass" };

describe("document upload diagnostics", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("document tutor query diagnostics", () => {
    afterEach(() => {
      vi.unstubAllGlobals();
    });

    it("uses the persistent study-chat endpoints for listing, reading, renaming, deleting, and sending", async () => {
      const summary = {
        id: "conversation-1",
        title: "Estado colombiano",
        createdAt: "2026-10-31T12:00:00Z",
        updatedAt: "2026-10-31T12:00:00Z",
        messageCount: 2,
      };
      const message = {
        id: "message-1",
        role: "ASSISTANT",
        content: "Respuesta fundamentada [S1].",
        keyPoints: null,
        sources: [],
        createdAt: "2026-10-31T12:00:00Z",
      };
      const fetchMock = vi.fn()
        .mockResolvedValueOnce(new Response(JSON.stringify([summary]), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf" }), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify(summary), { status: 201 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({ conversation: summary, messages: [message] }), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf" }), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify(summary), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf" }), { status: 200 }))
        .mockResolvedValueOnce(new Response(null, { status: 204 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf" }), { status: 200 }))
        .mockResolvedValueOnce(new Response(JSON.stringify({
          conversation: summary,
          userMessage: { ...message, id: "message-2", role: "USER", content: "Dame un ejemplo." },
          assistantMessage: message,
        }), { status: 201 }));
      vi.stubGlobal("fetch", fetchMock);

      expect(await listStudyConversations(auth)).toEqual([summary]);
      expect(await createStudyConversation(auth)).toEqual(summary);
      expect(await getStudyConversation(auth, summary.id)).toEqual({
        conversation: summary,
        messages: [message],
      });
      expect(await renameStudyConversation(auth, summary.id, "Nuevo título")).toEqual(summary);
      await deleteStudyConversation(auth, summary.id);
      expect((await sendStudyMessage(auth, summary.id, {
        content: "Dame un ejemplo.",
        learnerLevel: "Inicial",
      })).assistantMessage).toEqual(message);

      expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
        "/api/v1/study/conversations",
        "/api/v1/auth/csrf",
        "/api/v1/study/conversations",
        `/api/v1/study/conversations/${summary.id}`,
        "/api/v1/auth/csrf",
        `/api/v1/study/conversations/${summary.id}`,
        "/api/v1/auth/csrf",
        `/api/v1/study/conversations/${summary.id}`,
        "/api/v1/auth/csrf",
        `/api/v1/study/conversations/${summary.id}/messages`,
      ]);
      const sendRequest = fetchMock.mock.calls[9][1] as RequestInit;
      expect(sendRequest.method).toBe("POST");
      expect(JSON.parse(sendRequest.body as string)).toEqual({
        content: "Dame un ejemplo.",
        learnerLevel: "Inicial",
      });
      expect(JSON.parse(sendRequest.body as string)).not.toHaveProperty("sourceDocumentId");
    });

    it("posts a natural-language query to the explanations endpoint without a document filter by default", async () => {
      const fetchMock = vi.fn()
        .mockResolvedValueOnce(new Response(JSON.stringify({
          headerName: "X-CSRF-TOKEN",
          token: "csrf-test-token",
        }), { status: 200, headers: { "Content-Type": "application/json" } }))
        .mockResolvedValueOnce(new Response(JSON.stringify({
          explanation: "La gestión pública se explica en la fuente [S1].",
          keyPoints: "",
          sources: [],
        }), { status: 200, headers: { "Content-Type": "application/json" } }));
      vi.stubGlobal("fetch", fetchMock);
      const requestBody = {
        topicName: "Gestión pública y funcionamiento del Estado",
        learnerLevel: "Inicial",
      };

      const response = await explainTopic(auth, requestBody);

      expect(response.explanation).toContain("gestión pública");
      expect(fetchMock.mock.calls[1][0]).toBe("/api/v1/ai/explanations");
      const request = fetchMock.mock.calls[1][1] as RequestInit;
      expect(request.method).toBe("POST");
      expect(JSON.parse(request.body as string)).toEqual(requestBody);
      expect(JSON.parse(request.body as string)).not.toHaveProperty("sourceDocumentId");
      expect(new Headers(request.headers).get("X-CSRF-TOKEN")).toBe("csrf-test-token");
    });

    it("uses safe, distinct messages for provider failure, timeout, and internal server errors", () => {
      expect(getErrorMessage(new ApiError("http", 502))).toMatch(/servicio de IA no pudo completar/i);
      expect(getErrorMessage(new ApiError("http", 504))).toMatch(/tardó demasiado/i);
      expect(getErrorMessage(new ApiError("http", 500))).toMatch(/problema al procesar la consulta/i);
      expect(getErrorMessage(new ApiError("http", 500, "internal token /srv/path")))
        .not.toMatch(/internal token|srv\/path/i);
    });
  });

  it("preserves a safe PDF validation detail returned by the real upload endpoint", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        headerName: "X-CSRF-TOKEN",
        token: "csrf-test-token",
      }), { status: 200, headers: { "Content-Type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        detail: "El PDF no contiene texto extraíble; si es escaneado, requiere OCR",
      }), { status: 400, headers: { "Content-Type": "application/problem+json" } }));
    vi.stubGlobal("fetch", fetchMock);

    const file = new File(["%PDF-1.7"], "Ley_2094_de_2021.pdf", { type: "application/x-pdf" });
    const uploadError = await uploadDocument(auth, file).catch((error: unknown) => error);

    expect(uploadError).toBeInstanceOf(ApiError);
    expect(uploadError).toMatchObject({
      kind: "http",
      status: 400,
      detail: "El PDF no contiene texto extraíble; si es escaneado, requiere OCR",
    });
    expect(getErrorMessage(uploadError)).toMatch(/Si es un escaneo, necesita OCR/i);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const uploadRequest = fetchMock.mock.calls[1][1] as RequestInit;
    expect(uploadRequest.body).toBeInstanceOf(FormData);
    expect((uploadRequest.body as FormData).get("file")).toBe(file);
  });
});
