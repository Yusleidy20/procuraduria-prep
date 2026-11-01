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
import type { SectionId } from "./navigation";
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
  documents,
  onStartTopic,
  onNavigate,
}: CommonProps & {
  user: AuthenticatedUser;
  data: StudyData;
  documents: DocumentResponse[];
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
  const todayName = new Intl.DateTimeFormat("es-CO", { weekday: "long" }).format(new Date());
  const todayPlanDay = todayName.charAt(0).toUpperCase() + todayName.slice(1);
  const agenda = SYLLABUS.map((topic, index) => ({ topic, index }))
    .filter(({ index }) => getPlanDay(index) === todayPlanDay)
    .slice(0, 3);
  const agendaItems = agenda.length ? agenda : SYLLABUS.filter((topic) => !plan.completed[topic.id]).slice(0, 3).map((topic) => ({
    topic,
    index: SYLLABUS.findIndex((item) => item.id === topic.id),
  }));
  const completedDocuments = documents.filter((document) => document.status === "COMPLETED").length;
  const documentsUnavailable = data.failures.includes("documentos");
  const reviewsUnavailable = data.failures.includes("repasos");

  return (
    <div className="view-stack dashboard-page">
      {data.failures.length > 0 && (
        <p className="notice notice-info" role="status">Algunos datos del backend no están disponibles en este momento; las cifras que faltan no se estiman.</p>
      )}

      <header className="dashboard-hero">
        <div className="hero-copy">
          <p className="eyebrow">TU ESPACIO DE PREPARACIÓN</p>
          <h1>Hola, {user.name || user.email.split("@")[0]}</h1>
          <p>Tu camino al concurso se construye sesión a sesión. Retoma tu plan y avanza con foco.</p>
          <div className="hero-actions">
            {nextTopic
              ? <button className="primary-button" onClick={() => onStartTopic(nextTopic.title)}>Continuar estudiando <span aria-hidden="true">→</span></button>
              : <button className="primary-button" onClick={() => onNavigate("syllabus")}>Revisar el temario <span aria-hidden="true">→</span></button>}
            <span className="hero-date"><span aria-hidden="true">◷</span>{new Intl.DateTimeFormat("es-CO", { weekday: "long", day: "numeric", month: "long" }).format(new Date())}</span>
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <span className="hero-art-sun" />
          <span className="hero-art-orbit hero-art-orbit-one" />
          <span className="hero-art-orbit hero-art-orbit-two" />
          <svg viewBox="0 0 310 220" role="presentation">
            <path d="M56 177c20-54 42-83 82-87 46-5 75 29 116 74l18 13H56Z" fill="#c6dcf1" />
            <path d="M72 170c12-37 33-58 63-58 33 0 48 20 73 58H72Z" fill="#a9c8e7" />
            <path d="M102 143c0-41 24-73 56-73s56 32 56 73v37H102v-37Z" fill="#356daa" />
            <path d="M158 70c-31 0-56 32-56 73v37h56V70Z" fill="#24496f" />
            <path d="M119 142c0-29 17-52 39-52s39 23 39 52v32h-78v-32Z" fill="#fff" />
            <path d="M158 90c-22 0-39 23-39 52v22h39V90Z" fill="#edf4fb" />
            <path d="M131 119h19m-19 10h19m16-10h18m-18 10h18" stroke="#8db8e8" strokeWidth="3" strokeLinecap="round" />
            <path d="M93 177h132" stroke="#24496f" strokeWidth="5" strokeLinecap="round" />
            <path d="m247 95 6 13 14 2-10 10 2 14-12-7-12 7 2-14-10-10 14-2 6-13Z" fill="#fff" />
            <circle cx="76" cy="111" r="7" fill="#fff" />
            <circle cx="245" cy="55" r="4" fill="#8db8e8" />
          </svg>
          <div className="hero-art-note"><span className="hero-art-note-icon">✦</span><span><strong>Un tema a la vez</strong><small>Avanza a tu ritmo</small></span></div>
        </div>
      </header>

      <section className="dashboard-stats" aria-label="Resumen de preparación">
        <article className="dashboard-stat stat-study">
          <span className="stat-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21zM4 5.5V21m4-13h8m-8 4h8" /></svg></span>
          <div><span className="stat-label">Temas estudiados</span><strong>{completedCount}<small> / {SYLLABUS.length}</small></strong><span className="stat-caption">Marcados en este dispositivo</span></div>
        </article>
        <article className="dashboard-stat stat-documents">
          <span className="stat-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M6 3h8l5 5v13H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm8 0v5h5m-9 5h5m-5 4h5" /></svg></span>
          <div><span className="stat-label">Documentos</span><strong>{documentsUnavailable ? "—" : documents.length}</strong><span className="stat-caption">{documentsUnavailable ? "Dato no disponible" : `${completedDocuments} listos para consulta`}</span></div>
        </article>
        <article className="dashboard-stat stat-reviews">
          <span className="stat-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M20 11a8 8 0 0 0-14.9-3M4 4v4h4m-4 5a8 8 0 0 0 14.9 3M20 20v-4h-4" /></svg></span>
          <div><span className="stat-label">Repasos pendientes</span><strong>{reviewsUnavailable ? "—" : data.reviews.length}</strong><span className="stat-caption">{reviewsUnavailable ? "Dato no disponible" : "Tarjetas indicadas por el backend"}</span></div>
        </article>
        <article className="dashboard-stat stat-attempts">
          <span className="stat-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M5 4h14v16H5zM8 8h8m-8 4h4m-4 4h3m5-4 1.5 1.5L20 10" /></svg></span>
          <div><span className="stat-label">Intentos completados</span><strong>{data.progress?.completedQuizAttempts ?? (data.failures.includes("progreso") ? "—" : 0)}</strong><span className="stat-caption">{data.failures.includes("progreso") ? "Dato no disponible" : "Registrados por la plataforma"}</span></div>
        </article>
      </section>

      <section className="dashboard-feature-grid" aria-label="Sesión y actividad">
        <article className="panel recommended-card">
          <div className="recommended-topline"><span className="recommended-kicker"><span aria-hidden="true">✦</span> PARA TU PRÓXIMA SESIÓN</span><span className="recommended-duration">{ESTIMATED_SESSION_MINUTES} min sugeridos</span></div>
          {nextTopic ? <>
            <span className="recommended-area">{nextTopic.area}</span>
            <h2>{nextTopic.title}</h2>
            <p>Retoma tu preparación con una sesión breve y enfocada en el temario.</p>
            <button className="primary-button" onClick={() => onStartTopic(nextTopic.title)}>Empezar sesión <span aria-hidden="true">→</span></button>
          </> : <>
            <h2>¡Completaste los temas de tu plan!</h2>
            <p>Puedes volver al temario y revisar cualquier área cuando quieras.</p>
            <button className="secondary-button" onClick={() => onNavigate("syllabus")}>Volver al temario</button>
          </>}
          <div className="recommended-watermark" aria-hidden="true">P</div>
        </article>

        <article className="panel recent-activity">
          <div className="section-title-row"><div><p className="eyebrow">TU RECORRIDO</p><h2>Actividad reciente</h2></div><button className="text-link" onClick={() => onNavigate("plan")}>Ver agenda →</button></div>
          {latestTopic ? <div className="recent-event">
            <span className="recent-event-icon" aria-hidden="true">✓</span>
            <div><strong>{latestTopic.title}</strong><p>Sesión completada localmente</p></div>
            <time dateTime={latestActivity![1]}>{new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short" }).format(new Date(latestActivity![1]))}</time>
          </div> : <div className="activity-empty"><span className="activity-empty-mark" aria-hidden="true">✦</span><p>Aún no hay sesiones completadas. Tu actividad aparecerá aquí cuando avances en el plan.</p></div>}
          <div className="activity-footer"><span>Actividad del plan personal</span><button className="text-link" onClick={() => onNavigate("plan")}>Abrir mi plan</button></div>
        </article>
      </section>

      <section className="dashboard-lower-grid" aria-label="Ruta de preparación">
        <article className="panel study-areas-panel">
          <div className="section-title-row"><div><p className="eyebrow">TU RUTA DE PREPARACIÓN</p><h2>Áreas de estudio</h2></div><button className="text-link" onClick={() => onNavigate("syllabus")}>Ver temario completo →</button></div>
          <div className="study-area-list">
            {(["Conocimientos comunes", "Conocimientos específicos"] as const).map((area, index) => {
              const areaTopics = SYLLABUS.filter((topic) => topic.area === area);
              const finished = areaTopics.filter((topic) => plan.completed[topic.id]).length;
              const rate = Math.round((finished / areaTopics.length) * 100);
              return <button className="study-area-row" key={area} onClick={() => onNavigate("syllabus")}>
                <span className={`study-area-symbol area-symbol-${index + 1}`} aria-hidden="true">{index === 0 ? "§" : "¶"}</span>
                <span className="study-area-content"><span className="study-area-heading"><strong>{area}</strong><small>{finished} de {areaTopics.length} temas</small></span><span className="progress-track" role="progressbar" aria-label={`Avance de ${area}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={rate}><span style={{ width: `${rate}%` }} /></span></span>
                <span className="study-area-arrow" aria-hidden="true">↗</span>
              </button>;
            })}
          </div>
        </article>

        <article className="panel agenda-panel">
          <div className="section-title-row"><div><p className="eyebrow">PLAN SUGERIDO</p><h2>Agenda de hoy</h2></div><span className="agenda-day">{todayPlanDay}</span></div>
          {agendaItems.length ? <div className="agenda-list">{agendaItems.map(({ topic, index }) => {
            const status = getStudyStatus(topic.id, plan.completed, plan.inProgressId);
            return <button type="button" className={`agenda-item agenda-${status.toLowerCase().replace(" ", "-")}`} key={topic.id} onClick={() => onStartTopic(topic.title)}>
              <span className="agenda-time">{String(index + 1).padStart(2, "0")}</span>
              <span className="agenda-item-copy"><strong>{topic.title}</strong><small>{status} · ~{ESTIMATED_SESSION_MINUTES} min</small></span>
              <span className="agenda-status" aria-label={status}>{status === "Completado" ? "✓" : "→"}</span>
            </button>;
          })}</div> : <p className="empty-copy">No hay sesiones pendientes en la ruta sugerida.</p>}
          <button className="agenda-all-link" onClick={() => onNavigate("plan")}>Ver mi plan de estudio <span aria-hidden="true">→</span></button>
        </article>
      </section>

      <section className="dashboard-last-grid" aria-label="Progreso y accesos rápidos">
        <article className="panel progress-overview">
          <div className="section-title-row"><div><p className="eyebrow">AVANCE PERSONAL</p><h2>Tu progreso</h2></div><button className="text-link" onClick={() => onNavigate("progress")}>Ver detalles →</button></div>
          <div className="progress-overview-content">
            <div className="progress-ring" role="progressbar" aria-label="Avance de temas del plan personal" aria-valuemin={0} aria-valuemax={100} aria-valuenow={completionRate} style={{ background: `conic-gradient(var(--blue) ${completionRate}%, #e6eef7 ${completionRate}% 100%)` }}>
              <span><strong>{completionRate}%</strong><small>del plan</small></span>
            </div>
            <div className="progress-overview-detail"><strong>{completedCount} de {SYLLABUS.length} temas</strong><p>{completionRate}% de sesiones sugeridas marcadas en este dispositivo</p><div className="backend-progress-line"><span>Dominio registrado en plataforma</span><strong>{data.progress?.masteryPercentage != null ? `${data.progress.masteryPercentage}%` : "Sin datos"}</strong></div></div>
          </div>
        </article>
        <article className="panel dashboard-shortcuts">
          <div className="section-title-row"><div><p className="eyebrow">A UN TOQUE</p><h2>Accesos rápidos</h2></div></div>
          <div className="shortcut-grid">
            <button onClick={() => onNavigate("materials")}><span className="shortcut-icon shortcut-docs" aria-hidden="true">▤</span><span><strong>Mis documentos</strong><small>Subir y organizar</small></span><span aria-hidden="true">↗</span></button>
            <button onClick={() => onNavigate("ai")}><span className="shortcut-icon shortcut-ai" aria-hidden="true">✦</span><span><strong>Estudiar con IA</strong><small>Consultar fuentes</small></span><span aria-hidden="true">↗</span></button>
            <button onClick={() => onNavigate("simulators")}><span className="shortcut-icon shortcut-quiz" aria-hidden="true">✓</span><span><strong>Simulacros</strong><small>Practicar preguntas</small></span><span aria-hidden="true">↗</span></button>
            <button onClick={() => onNavigate("reviews")}><span className="shortcut-icon shortcut-review" aria-hidden="true">↻</span><span><strong>Repasos</strong><small>Retomar tarjetas</small></span><span aria-hidden="true">↗</span></button>
          </div>
        </article>
      </section>
      <p className="data-footnote">El avance del plan y la agenda son locales y sugeridos. Dominio, repasos, documentos e intentos provienen del backend; el tiempo de estudio no se registra.</p>
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

export function SyllabusView({ plan, onStartTopic, onToggleComplete, onNavigate }: CommonProps & {
  onToggleComplete: (topicId: string) => void;
  onNavigate: (section: SectionId) => void;
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
                  <button className="text-button" onClick={() => onNavigate("materials")}>Ver materiales</button>
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

export function MaterialsView({ documents, onUpload, onDelete }: {
  documents: DocumentResponse[];
  onUpload: (file: File) => Promise<void>;
  onDelete: (documentId: string) => Promise<void>;
}) {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [filter, setFilter] = useState("");

  function getLocalFileError(file: File): string | null {
    if (file.size === 0) return `${file.name}: el archivo está vacío.`;
    if (file.size > 10 * 1024 * 1024) return `${file.name}: supera el límite de 10 MiB.`;
    const extension = file.name.slice(file.name.lastIndexOf(".")).toLowerCase();
    if (![".pdf", ".txt", ".md"].includes(extension)) {
      return `${file.name}: formato no compatible. Usa PDF, TXT o Markdown.`;
    }
    return null;
  }

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
    const fileErrors = nextFiles.map(getLocalFileError)
      .filter((error): error is string => error !== null);
    if (fileErrors.length) {
      setSelectedFiles([]);
      setUploadError(fileErrors.join(" "));
      event.target.value = "";
      return;
    }
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
                  <button type="button" className="secondary-button small-button" onClick={() => void handleDelete(document.id)} disabled={pendingDeleteId === document.id}>{pendingDeleteId === document.id ? "Eliminando…" : "Eliminar"}</button>
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
