import type {
  AuthenticatedUser,
  DocumentPage,
  DocumentTextResponse,
  Flashcard,
  OverallProgress,
  Quiz,
  QuizAttempt,
  QuizQuestion,
  StudyModule,
  StudyTopic,
  TopicProgress,
  TopicExplanationResponse,
  StudyChatResponse,
  StudyConversationDetail,
  StudyConversationSummary,
} from "./types";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");
const REQUEST_TIMEOUT_MS = 60_000;

export class ApiError extends Error {
  constructor(
    readonly kind: "network" | "timeout" | "http",
    readonly status?: number,
    readonly detail?: string,
  ) {
    super(kind);
    this.name = "ApiError";
  }
}

function basicAuth(email: string, password: string): string {
  const bytes = new TextEncoder().encode(`${email}:${password}`);
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
  return `Basic ${btoa(binary)}`;
}

function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

async function request(
  path: string,
  auth: { email: string; password: string },
  init: RequestInit = {},
): Promise<Response> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const headers = new Headers(init.headers);
  headers.set("Authorization", basicAuth(auth.email, auth.password));

  try {
    return await fetch(apiUrl(path), {
      ...init,
      headers,
      credentials: "include",
      signal: controller.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiError("timeout");
    }
    throw new ApiError("network");
  } finally {
    window.clearTimeout(timeout);
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let detail: string | undefined;
    if (response.status === 400) {
      try {
        const body: unknown = await response.clone().json();
        if (body && typeof body === "object" && "detail" in body
          && typeof body.detail === "string" && body.detail.length <= 240) {
          detail = body.detail;
        }
      } catch {
        detail = undefined;
      }
    }
    throw new ApiError("http", response.status, detail);
  }

  try {
    return (await response.json()) as T;
  } catch {
    throw new ApiError("http", response.status);
  }
}

async function getCsrfToken(
  auth: { email: string; password: string },
): Promise<{ headerName: string; token: string }> {
  const response = await request("/api/v1/auth/csrf", auth);
  return readJson<{ headerName: string; token: string }>(response);
}

async function postJson<T>(
  path: string,
  auth: { email: string; password: string },
  body: unknown,
): Promise<T> {
  const csrf = await getCsrfToken(auth);
  const response = await request(path, auth, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  });
  if (response.status === 204) return undefined as T;
  return readJson<T>(response);
}

async function patchJson<T>(
  path: string,
  auth: { email: string; password: string },
  body: unknown,
): Promise<T> {
  const csrf = await getCsrfToken(auth);
  const response = await request(path, auth, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  });
  return readJson<T>(response);
}

async function readPage<T>(response: Response): Promise<T[]> {
  const page = await readJson<{ content: T[] }>(response);
  return page.content;
}

export async function getCurrentUser(auth: { email: string; password: string }) {
  const response = await request("/api/v1/auth/me", auth);
  return readJson<AuthenticatedUser>(response);
}

export async function getDocuments(auth: { email: string; password: string }) {
  const response = await request("/api/v1/documents?page=0&size=100&sort=uploadedAt,desc", auth);
  return readJson<DocumentPage>(response);
}

export async function uploadDocument(auth: { email: string; password: string }, file: File) {
  const csrf = await getCsrfToken(auth);
  const formData = new FormData();
  formData.append("file", file);
  const response = await request("/api/v1/documents", auth, {
    method: "POST",
    headers: { [csrf.headerName]: csrf.token },
    body: formData,
  });
  return readJson<import("./types").DocumentResponse>(response);
}

export async function deleteDocument(auth: { email: string; password: string }, documentId: string) {
  const csrf = await getCsrfToken(auth);
  const response = await request(`/api/v1/documents/${encodeURIComponent(documentId)}`, auth, {
    method: "DELETE",
    headers: { [csrf.headerName]: csrf.token },
  });
  if (!response.ok) throw new ApiError("http", response.status);
}

export async function getOverallProgress(auth: { email: string; password: string }) {
  const response = await request("/api/v1/progress", auth);
  return readJson<OverallProgress>(response);
}

export async function getDueReviews(auth: { email: string; password: string }) {
  const response = await request("/api/v1/progress/reviews/due?page=0&size=100", auth);
  return readPage<Flashcard>(response);
}

export async function recordFlashcardReview(
  auth: { email: string; password: string },
  flashcardId: string,
  remembered: boolean,
) {
  return postJson<Flashcard>(
    `/api/v1/flashcards/${encodeURIComponent(flashcardId)}/reviews`,
    auth,
    { remembered },
  );
}

export async function getStudyModules(auth: { email: string; password: string }) {
  const response = await request("/api/v1/study/modules?page=0&size=100&sort=displayOrder,asc", auth);
  return readPage<StudyModule>(response);
}

export async function getModuleTopics(auth: { email: string; password: string }, moduleId: string) {
  const response = await request(
    `/api/v1/study/modules/${encodeURIComponent(moduleId)}/topics?page=0&size=100&sort=name,asc`,
    auth,
  );
  return readPage<StudyTopic>(response);
}

export async function getTopicProgress(auth: { email: string; password: string }, topicId: string) {
  const response = await request(`/api/v1/progress/topics/${encodeURIComponent(topicId)}`, auth);
  return readJson<TopicProgress>(response);
}

export async function getQuizzes(auth: { email: string; password: string }) {
  const response = await request("/api/v1/quizzes?page=0&size=100&sort=createdAt,desc", auth);
  return readPage<Quiz>(response);
}

export async function getQuizQuestions(auth: { email: string; password: string }, quizId: string) {
  const response = await request(
    `/api/v1/quizzes/${encodeURIComponent(quizId)}/questions?page=0&size=100&sort=createdAt,asc`,
    auth,
  );
  return readPage<QuizQuestion>(response);
}

export async function startQuizAttempt(auth: { email: string; password: string }, quizId: string) {
  return postJson<QuizAttempt>(`/api/v1/quizzes/${encodeURIComponent(quizId)}/attempts`, auth, {});
}

export async function submitQuizAnswer(
  auth: { email: string; password: string },
  attemptId: string,
  questionId: string,
  selectedOptionIndex: number,
) {
  return postJson<void>(
    `/api/v1/quiz-attempts/${encodeURIComponent(attemptId)}/answers`,
    auth,
    { questionId, selectedOptionIndex },
  );
}

export async function finishQuizAttempt(auth: { email: string; password: string }, attemptId: string) {
  return postJson<QuizAttempt>(`/api/v1/quiz-attempts/${encodeURIComponent(attemptId)}/finish`, auth, {});
}

export async function explainTopic(
  auth: { email: string; password: string },
  requestBody: {
    topicName: string;
    learnerLevel: string;
    sourceDocumentId?: string;
    previousQuestions?: string[];
  },
) {
  return postJson<TopicExplanationResponse>("/api/v1/ai/explanations", auth, requestBody);
}

export async function listStudyConversations(auth: { email: string; password: string }) {
  const response = await request("/api/v1/study/conversations", auth);
  return readJson<StudyConversationSummary[]>(response);
}

export async function createStudyConversation(
  auth: { email: string; password: string },
  title?: string,
) {
  return postJson<StudyConversationSummary>("/api/v1/study/conversations", auth, { title });
}

export async function getStudyConversation(
  auth: { email: string; password: string },
  conversationId: string,
) {
  const response = await request(
    `/api/v1/study/conversations/${encodeURIComponent(conversationId)}`,
    auth,
  );
  return readJson<StudyConversationDetail>(response);
}

export async function renameStudyConversation(
  auth: { email: string; password: string },
  conversationId: string,
  title: string,
) {
  return patchJson<StudyConversationSummary>(
    `/api/v1/study/conversations/${encodeURIComponent(conversationId)}`,
    auth,
    { title },
  );
}

export async function deleteStudyConversation(
  auth: { email: string; password: string },
  conversationId: string,
) {
  const csrf = await getCsrfToken(auth);
  const response = await request(
    `/api/v1/study/conversations/${encodeURIComponent(conversationId)}`,
    auth,
    { method: "DELETE", headers: { [csrf.headerName]: csrf.token } },
  );
  if (!response.ok) throw new ApiError("http", response.status);
}

export async function sendStudyMessage(
  auth: { email: string; password: string },
  conversationId: string,
  requestBody: {
    content: string;
    learnerLevel: string;
    sourceDocumentId?: string;
  },
) {
  return postJson<StudyChatResponse>(
    `/api/v1/study/conversations/${encodeURIComponent(conversationId)}/messages`,
    auth,
    requestBody,
  );
}

export async function getDocumentText(
  auth: { email: string; password: string },
  documentId: string,
) {
  const response = await request(
    `/api/v1/documents/${encodeURIComponent(documentId)}/text`,
    auth,
  );
  return readJson<DocumentTextResponse>(response);
}

export function getErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "Ocurrió un problema inesperado. Inténtalo de nuevo.";
  }
  if (error.kind === "timeout") {
    return "La consulta tardó demasiado. Comprueba tu conexión e inténtalo de nuevo.";
  }
  if (error.kind === "network") {
    return "No fue posible conectar con el servidor. Comprueba que el backend esté disponible.";
  }

  switch (error.status) {
    case 401:
      return "No se pudo autenticar. Verifica tu correo y contraseña.";
    case 403:
      return "La sesión de seguridad expiró. Vuelve a conectar e inténtalo de nuevo.";
    case 404:
      return "La conversación ya no está disponible. Actualiza el historial e inténtalo de nuevo.";
    case 422:
      return "No encontré información suficiente en los documentos para responder. Prueba con otro tema o documento.";
    case 408:
    case 504:
      return "La consulta tardó demasiado. Comprueba tu conexión e inténtalo de nuevo.";
    case 429:
      return "El servicio de IA está atendiendo demasiadas consultas. Inténtalo más tarde.";
    case 400:
      if (error.detail) {
        const detail = error.detail.toLocaleLowerCase("es");
        if (detail.includes("requiere ocr")) {
          return "Este PDF no contiene texto seleccionable. Si es un escaneo, necesita OCR, que aún no está disponible.";
        }
        if (detail.includes("no contiene texto extraíble")) {
          return "El PDF no contiene texto extraíble. Si es un documento escaneado, necesita OCR.";
        }
        if (detail.includes("no contiene páginas legibles")) {
          return "El PDF no contiene páginas legibles.";
        }
        if (detail.includes("contenido no es un pdf válido")) {
          return "El archivo tiene extensión PDF, pero su contenido no es un PDF válido.";
        }
        if (detail.includes("leer el pdf") || detail.includes("leer el archivo pdf")) {
          return "No fue posible leer el PDF. Comprueba que no esté dañado o protegido con contraseña.";
        }
        if (detail.includes("archivo no vacío")) {
          return "El archivo está vacío. Selecciona un documento que tenga contenido.";
        }
        if (detail.includes("no contiene texto")) {
          return "El documento de texto está vacío.";
        }
        if (detail.includes("codificados en utf-8")) {
          return "El archivo TXT o Markdown debe estar codificado en UTF-8.";
        }
        if (detail.includes("extensión y el tipo de contenido")) {
          return "La extensión no coincide con el tipo de archivo. Selecciona el archivo original sin cambiar su extensión.";
        }
        if (detail.includes("cabecera pdf válida")) {
          return "El archivo tiene extensión PDF, pero su contenido no es un PDF válido.";
        }
        if (detail.includes("solo se admiten archivos pdf, txt o markdown")) {
          return "El formato no es compatible. Usa PDF, TXT o Markdown.";
        }
      }
      return "El archivo no es válido. Usa un PDF, TXT o Markdown con un nombre y contenido válidos.";
    case 413:
      return "El archivo supera el límite permitido de 10 MiB.";
    case 415:
      return "El formato del archivo no es compatible. Usa PDF, TXT o Markdown.";
    case 502:
      return "El servicio de IA no pudo completar la consulta. Inténtalo más tarde.";
    case 503:
      return "El servicio de IA no está disponible en este momento. Inténtalo más tarde.";
    case 500:
      return "El servidor encontró un problema al procesar la consulta. Inténtalo de nuevo; si continúa, contacta con soporte.";
    default:
      return "No fue posible completar la solicitud. Inténtalo de nuevo.";
  }
}
