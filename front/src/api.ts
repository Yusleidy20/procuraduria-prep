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
} from "./types";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");
const REQUEST_TIMEOUT_MS = 60_000;

export class ApiError extends Error {
  constructor(
    readonly kind: "network" | "timeout" | "http",
    readonly status?: number,
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
    throw new ApiError("http", response.status);
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
  requestBody: { topicName: string; learnerLevel: string; sourceDocumentId?: string },
) {
  return postJson<TopicExplanationResponse>("/api/v1/ai/explanations", auth, requestBody);
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
    case 422:
      return "No encontré información suficiente en los documentos para responder. Prueba con otro tema o documento.";
    case 400:
      return "El archivo no es válido. Usa un PDF, TXT o Markdown con un nombre y contenido válidos.";
    case 413:
      return "El archivo supera el límite permitido de 10 MiB.";
    case 415:
      return "El formato del archivo no es compatible. Usa PDF, TXT o Markdown.";
    case 502:
      return "El servicio de IA no pudo completar la consulta. Inténtalo más tarde.";
    case 503:
      return "El servicio de IA no está disponible en este momento. Inténtalo más tarde.";
    case 504:
      return "La consulta tardó demasiado. Comprueba tu conexión e inténtalo de nuevo.";
    default:
      return "No fue posible completar la solicitud. Inténtalo de nuevo.";
  }
}
