import { ChangeEvent, useMemo, useState } from "react";
import {
  finishQuizAttempt,
  getErrorMessage,
  getQuizQuestions,
  recordFlashcardReview,
  startQuizAttempt,
  submitQuizAnswer,
} from "./api";
import { ResponseContent } from "./ResponseContent";
import { ESTIMATED_SESSION_MINUTES, getPlanDay, getStudyStatus, SYLLABUS } from "./syllabus";
import type {
  AuthenticatedUser,
  DocumentResponse,
  Flashcard,
  OverallProgress,
  Quiz,
  QuizAttempt,
  QuizQuestion,
  StudyModule,
  StudyTopic,
  TopicProgress,
} from "./types";

export type SectionId = "dashboard" | "plan" | "syllabus" | "materials" | "ai" | "simulators" | "reviews" | "progress";

export const NAV_ITEMS: { id: SectionId; label: string; icon: string }[] = [
  { id: "dashboard", label: "Dashboard", icon: "grid" },
  { id: "plan", label: "Mi plan de estudio", icon: "calendar" },
  { id: "syllabus", label: "Temario", icon: "book" },
  { id: "materials", label: "Mis materiales", icon: "spark" },
  { id: "ai", label: "Estudiar con IA", icon: "spark" },
  { id: "simulators", label: "Simulacros", icon: "check" },
  { id: "reviews", label: "Repasos", icon: "refresh" },
  { id: "progress", label: "Mi progreso", icon: "chart" },
];

export interface StudyData {
  progress: OverallProgress | null;
  reviews: Flashcard[];
  modules: StudyModule[];
  topics: StudyTopic[];
  topicProgress: Record<string, TopicProgress>;
  quizzes: Quiz[];
  failures: string[];
}

interface PlanState {
  completed: Record<string, string>;
  inProgressId: string | null;
}

interface CommonProps {
  plan: PlanState;
  onStartTopic: (title: string) => void;
}

export function DashboardView({
  user,
  data,
  plan,
  onStartTopic,
  onNavigate,
}: CommonProps & {
  user: AuthenticatedUser;
  data: StudyData;
  onNavigate: (section: SectionId) => void;
}) {
  const completedCount = Object.keys(plan.completed).filter((id) => SYLLABUS.some((topic) => topic.id === id)).length;
  const completionRate = Math.round((completedCount / SYLLABUS.length) * 100);
  const nextTopic = SYLLABUS.find((topic) => topic.id === plan.inProgressId && !plan.completed[topic.id])
    ?? SYLLABUS.find((topic) => !plan.completed[topic.id]);
  const latestActivity = Object.entries(plan.completed)
    .filter(([id]) => SYLLABUS.some((topic) => topic.id === id))
    .sort((a, b) => b[1].localeCompare(a[1]))[0];
  const latestTopic = latestActivity ? SYLLABUS.find((topic) => topic.id === latestActivity[0]) : null;

  return (
    <div className="view-stack">
      <header className="view-heading">
        <div>
          <p className="eyebrow">TU ESPACIO DE PREPARACIÓN</p>
          <h1>Hola, {user.name || user.email.split("@")[0]}</h1>
          <p>Un paso a la vez. Hoy puedes avanzar con una sesión enfocada.</p>
        </div>
        <div className="today-chip"><span aria-hidden="true">◷</span> {new Intl.DateTimeFormat("es-CO", { weekday: "long", day: "numeric", month: "long" }).format(new Date())}</div>
      </header>

      {data.failures.length > 0 && (
        <p className="notice notice-info" role="status">Algunos datos del backend no están disponibles en este momento; las cifras que faltan no se estiman.</p>
      )}

      <section className="dashboard-grid" aria-label="Resumen de estudio">
        <article className="metric-card metric-primary">
          <div className="metric-label">Temas estudiados · plan local <span aria-hidden="true">↗</span></div>
          <strong className="metric-value">{completedCount}<small>/{SYLLABUS.length}</small></strong>
          <div className="progress-track" role="progressbar" aria-label="Avance del plan personal" aria-valuemin={0} aria-valuemax={100} aria-valuenow={completionRate}><span style={{ width: `${completionRate}%` }} /></div>
          <p>{completionRate}% de sesiones sugeridas marcadas en este dispositivo</p>
        </article>
        <article className="metric-card">
          <div className="metric-label">Dominio registrado <span className="metric-icon">◎</span></div>
          <strong className="metric-value">{data.progress?.masteryPercentage != null ? `${data.progress.masteryPercentage}%` : "—"}</strong>
          <p>{data.progress?.masteryPercentage != null ? "Según respuestas y repasos registrados" : "Aún no hay datos suficientes"}</p>
        </article>
        <article className="metric-card">
          <div className="metric-label">Intentos completados <span className="metric-icon">✓</span></div>
          <strong className="metric-value">{data.progress?.completedQuizAttempts ?? "—"}</strong>
          <p>{data.progress ? "Registrados por la plataforma" : "Dato no disponible"}</p>
        </article>
      </section>

      <section className="dashboard-columns">
        <article className="panel continue-panel">
          <div className="panel-heading">
            <div><p className="eyebrow">SESIÓN RECOMENDADA</p><h2>{nextTopic ? nextTopic.title : "Plan completado"}</h2></div>
            <span className="panel-mark" aria-hidden="true">✦</span>
          </div>
          {nextTopic ? (
            <>
              <p className="muted-copy">{nextTopic.area} · Sesión sugerida de {ESTIMATED_SESSION_MINUTES} minutos</p>
              <button className="primary-button" onClick={() => onStartTopic(nextTopic.title)}>Continuar estudiando <span aria-hidden="true">→</span></button>
            </>
          ) : <button className="secondary-button" onClick={() => onNavigate("syllabus")}>Revisar el temario</button>}
        </article>
        <article className="panel activity-panel">
          <div className="panel-heading"><div><p className="eyebrow">ACTIVIDAD RECIENTE</p><h2>Tu actividad</h2></div></div>
          {latestTopic ? (
            <div className="activity-item"><span className="activity-check" aria-hidden="true">✓</span><div><strong>{latestTopic.title}</strong><p>Sesión completada localmente · {new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short" }).format(new Date(latestActivity![1]))}</p></div></div>
          ) : <p className="empty-copy">Aún no has marcado sesiones como completadas.</p>}
          <button className="text-link" onClick={() => onNavigate("plan")}>Ver mi agenda <span aria-hidden="true">→</span></button>
        </article>
      </section>

      <section className="panel area-panel">
        <div className="panel-heading"><div><p className="eyebrow">TU RUTA</p><h2>Áreas de estudio</h2></div><button className="text-link" onClick={() => onNavigate("syllabus")}>Ver temario completo →</button></div>
        <div className="area-cards">
          {(["Conocimientos comunes", "Conocimientos específicos"] as const).map((area) => {
            const areaTopics = SYLLABUS.filter((topic) => topic.area === area);
            const finished = areaTopics.filter((topic) => plan.completed[topic.id]).length;
            return <button className="area-card" key={area} onClick={() => onNavigate("syllabus")}>
              <span className="area-icon" aria-hidden="true">{area === "Conocimientos comunes" ? "01" : "02"}</span>
              <span className="area-name">{area}</span>
              <span className="area-meta">{areaTopics.length} temas · {finished} sesiones completadas</span>
              <span className="area-arrow" aria-hidden="true">↗</span>
            </button>;
          })}
        </div>
      </section>

      <section className="dashboard-bottom-grid">
        <article className="panel compact-panel">
          <div className="panel-heading"><div><p className="eyebrow">REPASO ESPACIADO</p><h2>Pendiente de repaso</h2></div></div>
          <strong className="compact-number">{data.reviews.length}</strong>
          <p>{data.reviews.length === 1 ? "tarjeta pendiente" : "tarjetas pendientes"} según el backend</p>
          <button className="text-link" onClick={() => onNavigate("reviews")}>Ir a repasos →</button>
        </article>
        <article className="panel compact-panel">
          <div className="panel-heading"><div><p className="eyebrow">PRACTICA LO APRENDIDO</p><h2>Simulacros disponibles</h2></div></div>
          <strong className="compact-number">{data.quizzes.length}</strong>
          <p>{data.quizzes.length === 1 ? "cuestionario disponible" : "cuestionarios disponibles"}</p>
          <button className="text-link" onClick={() => onNavigate("simulators")}>Ver simulacros →</button>
        </article>
      </section>
      <p className="data-footnote">El avance del plan se guarda solo en este navegador. Las cifras de dominio, repasos e intentos provienen del backend.</p>
    </div>
  );
}

export function StudyPlanView({ plan, onStartTopic, onToggleComplete, onBegin }: CommonProps & {
  onToggleComplete: (topicId: string) => void;
  onBegin: (topicId: string) => void;
}) {
  return (
    <div className="view-stack">
      <header className="view-heading"><div><p className="eyebrow">ORGANIZA TU RITMO</p><h1>Mi plan de estudio</h1><p>Una agenda sugerida para distribuir los temas. Puedes ajustarla a tu disponibilidad.</p></div></header>
      <div className="notice notice-info"><strong>Plan local sugerido</strong><span>Las sesiones y su estado se guardan en este navegador; no se sincronizan con el backend. Cada duración es una estimación, no tiempo medido.</span></div>
      <div className="plan-days">
        {Array.from({ length: 7 }, (_, dayIndex) => {
          const sessions = SYLLABUS.map((topic, index) => ({ topic, index })).filter(({ index }) => getPlanDay(index) === ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"][dayIndex]);
          if (!sessions.length) return null;
          return <section className="panel plan-day" key={dayIndex} aria-labelledby={`day-${dayIndex}`}>
            <div className="plan-day-heading"><div><p className="eyebrow">DÍA {dayIndex + 1}</p><h2 id={`day-${dayIndex}`}>{getPlanDay(sessions[0].index)}</h2></div><span className="day-count">{sessions.length} sesiones sugeridas</span></div>
            <div className="session-list">
              {sessions.map(({ topic, index }) => {
                const status = getStudyStatus(topic.id, plan.completed, plan.inProgressId);
                return <article className={`session-row status-${status.toLowerCase().replace(" ", "-")}`} key={topic.id}>
                  <span className="session-indicator" aria-hidden="true">{status === "Completado" ? "✓" : status === "En curso" ? "◷" : String(index + 1).padStart(2, "0")}</span>
                  <div className="session-content"><span className="session-area">{topic.area}</span><strong>{topic.title}</strong><span className={`status-label ${status.toLowerCase().replace(" ", "-")}`}>{status}</span></div>
                  <div className="session-actions"><span className="session-time">~{ESTIMATED_SESSION_MINUTES} min</span>
                    {status === "Completado" ? <button className="text-button" onClick={() => onToggleComplete(topic.id)}>Deshacer</button> : <>
                      <button className="secondary-button small-button" onClick={() => { onBegin(topic.id); onStartTopic(topic.title); }}>{status === "En curso" ? "Continuar" : "Iniciar sesión"}</button>
                      <button className="text-button" onClick={() => onToggleComplete(topic.id)}>Marcar completada</button>
                    </>}
                  </div>
                </article>;
              })}
            </div>
          </section>;
        })}
      </div>
    </div>
  );
}

export function SyllabusView({ plan, onStartTopic, onToggleComplete }: CommonProps & {
  onToggleComplete: (topicId: string) => void;
}) {
  return (
    <div className="view-stack">
      <header className="view-heading"><div><p className="eyebrow">EMPLEO TÉCNICO INVESTIGADOR · CÓDIGO 4TI-15</p><h1>Temario</h1><p>Ruta organizada con los conocimientos indicados para tu convocatoria.</p></div></header>
      <div className="notice notice-info"><strong>Contenido de la convocatoria</strong><span>Se muestran únicamente los temas proporcionados. El PDF “mi concurso(2).pdf” no está disponible en los archivos del proyecto para contrastarlos con la fuente original; verifica el texto oficial. No se añadieron subtemas jurídicos.</span></div>
      {(["Conocimientos comunes", "Conocimientos específicos"] as const).map((area) => {
        const areaTopics = SYLLABUS.filter((topic) => topic.area === area);
        return <section className="panel syllabus-panel" key={area}>
          <div className="panel-heading"><div><p className="eyebrow">{area === "Conocimientos comunes" ? "ÁREA 01" : "ÁREA 02"}</p><h2>{area}</h2></div><span className="area-total">{areaTopics.length} temas</span></div>
          <div className="syllabus-list">
            {areaTopics.map((topic) => {
              const status = getStudyStatus(topic.id, plan.completed, plan.inProgressId);
              return <article className="syllabus-item" key={topic.id}>
                <span className={`syllabus-check ${status === "Completado" ? "checked" : ""}`} aria-hidden="true">{status === "Completado" ? "✓" : ""}</span>
                <div className="syllabus-title"><strong>{topic.title}</strong><span>{status === "Completado" ? "Sesión completada en este navegador" : status}</span></div>
                <div className="syllabus-actions">
                  <button className="text-button" onClick={() => onStartTopic(topic.title)}>Agregar material</button>
                  {status === "Completado" ? <button className="text-button" onClick={() => onToggleComplete(topic.id)}>Deshacer</button> : <button className="secondary-button small-button" onClick={() => onStartTopic(topic.title)}>Estudiar tema</button>}
                </div>
              </article>;
            })}
          </div>
        </section>;
      })}
    </div>
  );
}

export function MaterialsView({ auth, documents, onUpload, onDelete }: {
  auth: { email: string; password: string };
  documents: DocumentResponse[];
  onUpload: (file: File) => Promise<void>;
  onDelete: (documentId: string) => Promise<void>;
}) {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [filter, setFilter] = useState("");

  const filteredDocuments = useMemo(() => {
    const query = filter.trim().toLowerCase();
    if (!query) return documents;
    return documents.filter((document) => document.originalName.toLowerCase().includes(query));
  }, [documents, filter]);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const nextFiles = Array.from(event.target.files ?? []);
    setSelectedFiles(nextFiles);
    setUploadError("");
    if (!nextFiles.length) return;
    setUploading(true);
    try {
      for (const file of nextFiles) {
        await onUpload(file);
      }
      setSelectedFiles([]);
      if (event.target) event.target.value = "";
    } catch (error) {
      setUploadError(getErrorMessage(error));
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(documentId: string) {
    setPendingDeleteId(documentId);
    try {
      await onDelete(documentId);
    } catch (error) {
      setUploadError(getErrorMessage(error));
    } finally {
      setPendingDeleteId(null);
    }
  }

  return (
    <div className="view-stack">
      <header className="view-heading"><div><p className="eyebrow">MATERIALES DE ESTUDIO</p><h1>Mis documentos</h1><p>Carga tus materiales y usa los que te ayuden a estudiar con IA o a consultar fuentes.</p></div></header>
      <section className="panel materials-panel">
        <div className="panel-heading"><div><p className="eyebrow">SUBIR ARCHIVOS</p><h2>Materiales del usuario</h2></div></div>
        <label className="upload-box" htmlFor="material-upload">
          <input id="material-upload" type="file" accept=".pdf,.txt,.md" multiple onChange={handleFileChange} aria-label="Seleccionar documentos para subir" />
          <span className="upload-title">Selecciona documentos</span>
          <span className="upload-copy">PDF, TXT o Markdown · hasta 10 MiB por archivo</span>
        </label>
        {uploading && <div className="loading-card" role="status" aria-live="polite"><span className="spinner spinner-dark" aria-hidden="true" /><div><strong>Subiendo documentos</strong><p>Procesando texto y preparando fuentes para la consulta.</p></div></div>}
        {uploadError && <p className="error-banner" role="alert">{uploadError}</p>}
        {selectedFiles.length > 0 && <div className="selected-files" aria-live="polite">{selectedFiles.map((file) => <span key={`${file.name}-${file.size}`}>{file.name} · {Math.max(1, Math.ceil(file.size / 1024))} KB</span>)}</div>}
      </section>

      <section className="panel materials-list-panel">
        <div className="panel-heading"><div><p className="eyebrow">DOCUMENTOS CARGADOS</p><h2>Biblioteca personal</h2></div><div className="search-field"><label htmlFor="material-search" className="sr-only">Buscar documentos</label><input id="material-search" type="search" value={filter} onChange={(event) => setFilter(event.target.value)} placeholder="Buscar por nombre" /></div></div>
        {!documents.length ? <div className="empty-panel"><span className="empty-illustration" aria-hidden="true">📄</span><h2>No tienes documentos cargados</h2><p>Sube tus materiales para consultarlos con la IA y conservar sus referencias.</p></div> : null}
        {documents.length > 0 && filteredDocuments.length === 0 ? <div className="empty-panel"><span className="empty-illustration" aria-hidden="true">🔎</span><h2>No hay documentos que coincidan</h2><p>Prueba con otro nombre o limpia el filtro.</p></div> : null}
        {filteredDocuments.length > 0 && (
          <div className="materials-grid">
            {filteredDocuments.map((document) => (
              <article className="material-card" key={document.id}>
                <div className="material-header">
                  <div className="material-icon" aria-hidden="true">{document.originalName.split(".").pop()?.toUpperCase() || "FILE"}</div>
                  <div className="material-info">
                    <strong>{document.originalName}</strong>
                    <span>{document.status === "COMPLETED" ? "Procesado" : document.status === "PROCESSING" ? "Procesando" : document.status === "FAILED" ? "Error" : document.status}</span>
                  </div>
                </div>
                <div className="material-meta">
                  <span>{document.contentType}</span>
                  <span>{new Date(document.uploadedAt).toLocaleDateString("es-CO", { day: "2-digit", month: "short", year: "numeric" })}</span>
                </div>
                <div className="material-actions">
                  <button type="button" className="secondary-button small-button" onClick={() => void onDelete(document.id)} disabled={pendingDeleteId === document.id}>{pendingDeleteId === document.id ? "Eliminando…" : "Eliminar"}</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function ProgressView({ plan, data }: { plan: PlanState; data: StudyData }) {
  const completedTopics = SYLLABUS.filter((topic) => plan.completed[topic.id]);
  const accuracy = data.progress && data.progress.quizAnswers > 0
    ? Math.round((data.progress.correctQuizAnswers / data.progress.quizAnswers) * 100)
    : null;
  return (
    <div className="view-stack">
      <header className="view-heading"><div><p className="eyebrow">AVANCE REAL, SIN ESTIMACIONES</p><h1>Mi progreso</h1><p>Consulta lo registrado por la plataforma y tu plan local por separado.</p></div></header>
      <section className="progress-metrics">
        <article className="metric-card"><span className="metric-label">Dominio general · backend</span><strong className="metric-value">{data.progress?.masteryPercentage != null ? `${data.progress.masteryPercentage}%` : "—"}</strong><p>{data.progress?.masteryPercentage != null ? "Calculado con repasos y respuestas" : "Aún no hay actividad registrada"}</p></article>
        <article className="metric-card"><span className="metric-label">Respuestas correctas · backend</span><strong className="metric-value">{accuracy != null ? `${accuracy}%` : "—"}</strong><p>{data.progress?.quizAnswers ? `${data.progress.correctQuizAnswers} de ${data.progress.quizAnswers} respuestas` : "Sin respuestas de cuestionarios"}</p></article>
        <article className="metric-card"><span className="metric-label">Sesiones del plan · este navegador</span><strong className="metric-value">{completedTopics.length}<small>/{SYLLABUS.length}</small></strong><p>Marcadas manualmente como completadas</p></article>
      </section>
      <section className="panel area-progress-panel">
        <div className="panel-heading"><div><p className="eyebrow">AVANCE POR ÁREA</p><h2>Ruta de preparación</h2></div></div>
        {(["Conocimientos comunes", "Conocimientos específicos"] as const).map((area) => {
          const topics = SYLLABUS.filter((topic) => topic.area === area);
          const count = topics.filter((topic) => plan.completed[topic.id]).length;
          const rate = Math.round((count / topics.length) * 100);
          return <div className="area-progress" key={area}><div className="area-progress-heading"><strong>{area}</strong><span>{count} de {topics.length} sesiones · {rate}%</span></div><div className="progress-track"><span style={{ width: `${rate}%` }} /></div></div>;
        })}
      </section>
      <section className="panel">
        <div className="panel-heading"><div><p className="eyebrow">ACTIVIDAD REGISTRADA</p><h2>Resultados y tiempo</h2></div></div>
        <div className="result-facts"><div><span>Intentos completados</span><strong>{data.progress?.completedQuizAttempts ?? "No disponible"}</strong></div><div><span>Tarjetas repasadas</span><strong>{data.progress?.flashcardReviews ?? "No disponible"}</strong></div><div><span>Tiempo de estudio</span><strong>No registrado por el backend</strong></div></div>
      </section>
      {data.topics.length > 0 && <section className="panel actual-topics-panel"><div className="panel-heading"><div><p className="eyebrow">TEMAS CONFIGURADOS EN TU CUENTA</p><h2>Avance por tema · backend</h2></div></div><div className="actual-topic-list">{data.topics.map((topic) => { const stats = data.topicProgress[topic.id]; return <div className="actual-topic-row" key={topic.id}><div><strong>{topic.name}</strong><span>{topic.moduleName}</span></div><div className="actual-topic-metric"><strong>{stats?.masteryPercentage != null ? `${stats.masteryPercentage}%` : "—"}</strong><span>{stats ? `${stats.correctQuizAnswers}/${stats.quizAnswers} respuestas correctas` : "Sin datos de avance"}</span></div></div>; })}</div></section>}
    </div>
  );
}

export function ReviewsView({ reviews, auth, onReviewed, topics, documents, onStudyTopic, onConsultDocument }: {
  reviews: Flashcard[];
  auth: { email: string; password: string };
  onReviewed: (flashcardId: string) => void;
  topics: StudyTopic[];
  documents: DocumentResponse[];
  onStudyTopic: (title: string) => void;
  onConsultDocument: (title: string, documentId: string) => void;
}) {
  const [revealedId, setRevealedId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState("");
  async function submitReview(card: Flashcard, remembered: boolean) {
    setBusyId(card.id);
    setError("");
    try {
      await recordFlashcardReview(auth, card.id, remembered);
      onReviewed(card.id);
      setRevealedId(null);
    } catch (reason) {
      setError(getErrorMessage(reason));
    } finally {
      setBusyId(null);
    }
  }
  return <div className="view-stack">
    <header className="view-heading"><div><p className="eyebrow">REPASO ESPACIADO</p><h1>Repasos</h1><p>Tarjetas que el backend indica que están pendientes de repaso.</p></div></header>
    {error && <p className="error-banner" role="alert">{error}</p>}
    {!reviews.length ? <section className="panel empty-panel"><span className="empty-illustration" aria-hidden="true">✓</span><h2>No tienes repasos pendientes</h2><p>Cuando haya tarjetas que debas revisar, aparecerán aquí.</p></section> : reviews.map((card) => {
      const topic = topics.find((item) => item.id === card.topicId);
      const document = card.sourceDocumentId ? documents.find((item) => item.id === card.sourceDocumentId) : null;
      const sourceDocumentId = card.sourceDocumentId;
      return <article className="panel review-card" key={card.id}>
      <p className="eyebrow">TARJETA DE ESTUDIO · {card.difficulty}</p><h2>{card.question}</h2>
      {(topic || sourceDocumentId) && <div className="review-related"><span>Relacionado:</span>{topic && <button className="text-link" onClick={() => onStudyTopic(topic.name)}>{topic.name}</button>}{sourceDocumentId && <button className="text-link" onClick={() => onConsultDocument(topic?.name ?? card.question, sourceDocumentId)}>{document?.originalName ?? "Consultar documento asociado"}</button>}</div>}
      {revealedId === card.id ? <div className="review-answer"><p className="eyebrow">RESPUESTA</p><ResponseContent text={card.answer} />{card.explanation && <p className="muted-copy">{card.explanation}</p>}<div className="review-actions"><button className="secondary-button" onClick={() => void submitReview(card, false)} disabled={busyId === card.id}>Necesito repasarla</button><button className="primary-button" onClick={() => void submitReview(card, true)} disabled={busyId === card.id}>{busyId === card.id ? "Guardando…" : "La recordé"}</button></div></div> : <button className="secondary-button" onClick={() => setRevealedId(card.id)}>Mostrar respuesta</button>}
    </article>;
    })}
  </div>;
}

export function SimulatorsView({ quizzes, auth, onAttemptFinished }: {
  quizzes: Quiz[];
  auth: { email: string; password: string };
  onAttemptFinished: () => void;
}) {
  const [activeQuiz, setActiveQuiz] = useState<Quiz | null>(null);
  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [position, setPosition] = useState(0);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [result, setResult] = useState<QuizAttempt | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function start(quiz: Quiz) {
    setBusy(true); setError("");
    try {
      const loadedQuestions = await getQuizQuestions(auth, quiz.id);
      if (loadedQuestions.length === 0) {
        setError("Este cuestionario todavía no tiene preguntas disponibles.");
        return;
      }
      const newAttempt = await startQuizAttempt(auth, quiz.id);
      setAttempt(newAttempt); setQuestions(loadedQuestions); setActiveQuiz(quiz); setPosition(0); setSelectedOption(null); setResult(null);
    } catch (reason) { setError(getErrorMessage(reason)); }
    finally { setBusy(false); }
  }

  async function submitAnswer() {
    if (!activeQuiz || !attempt || selectedOption == null) return;
    const question = questions[position];
    setBusy(true); setError("");
    try {
      await submitQuizAnswer(auth, attempt.id, question.id, selectedOption);
      if (position + 1 < questions.length) {
        setPosition(position + 1); setSelectedOption(null);
      } else {
        const finished = await finishQuizAttempt(auth, attempt.id);
        setResult(finished); setAttempt(null); onAttemptFinished();
      }
    } catch (reason) { setError(getErrorMessage(reason)); }
    finally { setBusy(false); }
  }

  function exitAttempt() {
    setActiveQuiz(null); setAttempt(null); setQuestions([]); setResult(null); setError("");
  }

  if (result) return <div className="view-stack">
    <header className="view-heading"><div><p className="eyebrow">RESULTADO REGISTRADO</p><h1>{activeQuiz?.name ?? "Simulacro"}</h1></div></header>
    <section className="panel result-panel"><strong className="result-score">{result.scorePercentage ?? 0}<small>%</small></strong><p>{result.correctAnswers ?? 0} respuestas correctas de {result.totalQuestions}</p><div className="result-facts"><div><span>Correctas</span><strong>{result.correctAnswers ?? 0}</strong></div><div><span>Incorrectas</span><strong>{result.incorrectAnswers ?? 0}</strong></div></div><div className="quiz-review-list">{result.answers.map((answer) => <article key={answer.questionId} className="quiz-review-item"><strong>{answer.correct ? "Respuesta correcta" : "Respuesta incorrecta"}</strong>{answer.explanation && <ResponseContent text={answer.explanation} />}</article>)}</div><button className="primary-button" onClick={exitAttempt}>Volver a simulacros</button></section>
  </div>;

  if (attempt && activeQuiz) {
    const question = questions[position];
    return <div className="view-stack">
      <header className="view-heading"><div><p className="eyebrow">{activeQuiz.mockExam ? "SIMULACRO" : "CUESTIONARIO"}</p><h1>{activeQuiz.name}</h1><p>Pregunta {position + 1} de {questions.length}</p></div><button className="text-button" onClick={exitAttempt}>Salir del intento</button></header>
      {error && <p className="error-banner" role="alert">{error}</p>}
      {question ? <section className="panel question-panel"><div className="question-progress"><span style={{ width: `${((position + 1) / questions.length) * 100}%` }} /></div><p className="eyebrow">PREGUNTA {position + 1}</p><h2>{question.statement}</h2>{question.explanationReference && <p className="muted-copy">Documento asociado: {question.explanationReference}</p>}<fieldset className="answer-options"><legend>Selecciona una respuesta</legend>{question.options.map((option, index) => <label className={`answer-option ${selectedOption === index ? "selected" : ""}`} key={index}><input type="radio" name={`question-${question.id}`} checked={selectedOption === index} onChange={() => setSelectedOption(index)} /><span>{String.fromCharCode(65 + index)}</span>{option}</label>)}</fieldset><button className="primary-button" onClick={() => void submitAnswer()} disabled={selectedOption == null || busy}>{busy ? "Guardando respuesta…" : position + 1 === questions.length ? "Finalizar y ver resultado" : "Guardar y continuar"}</button></section> : <p className="notice notice-info">No hay preguntas disponibles en este cuestionario.</p>}
    </div>;
  }

  return <div className="view-stack">
    <header className="view-heading"><div><p className="eyebrow">PRACTICA CON CUESTIONARIOS REALES</p><h1>Simulacros</h1><p>Responde cuestionarios guardados y consulta el resultado que registra el backend.</p></div></header>
    {error && <p className="error-banner" role="alert">{error}</p>}
    {busy && <p className="notice notice-info" role="status">Preparando cuestionario…</p>}
    {!quizzes.length ? <section className="panel empty-panel"><span className="empty-illustration" aria-hidden="true">⌑</span><h2>Aún no hay cuestionarios disponibles</h2><p>Cuando haya cuestionarios con preguntas guardados en la plataforma, aparecerán aquí. No se muestran preguntas de ejemplo.</p></section> : <div className="quiz-grid">{quizzes.map((quiz) => <article className="panel quiz-card" key={quiz.id}><span className="quiz-type">{quiz.mockExam ? "SIMULACRO" : "CUESTIONARIO"}</span><h2>{quiz.name}</h2><p>{quiz.description || "Cuestionario de estudio"}</p><div className="quiz-meta"><span>{quiz.questionCount} preguntas</span><span>{quiz.durationMinutes ? `${quiz.durationMinutes} min` : "Sin límite de tiempo"}</span><span>{quiz.difficulty}</span></div><button className="primary-button" onClick={() => void start(quiz)} disabled={busy || quiz.questionCount === 0}>{quiz.questionCount === 0 ? "Sin preguntas" : "Iniciar"} <span aria-hidden="true">→</span></button></article>)}</div>}
    <p className="data-footnote">Solo aparecen cuestionarios creados y guardados en la plataforma. Los resultados se calculan y persisten en el backend.</p>
  </div>;
}
