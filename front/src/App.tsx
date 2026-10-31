import { FormEvent, useEffect, useState } from "react";
import {
  deleteDocument,
  getCurrentUser,
  getDocuments,
  getDueReviews,
  getErrorMessage,
  getModuleTopics,
  getOverallProgress,
  getQuizzes,
  getStudyModules,
  getTopicProgress,
  uploadDocument,
} from "./api";
import { AIStudyView } from "./AIStudyView";
import {
  DashboardView,
  MaterialsView,
  NAV_ITEMS,
  ProgressView,
  ReviewsView,
  SectionId,
  SimulatorsView,
  StudyData,
  StudyPlanView,
  SyllabusView,
} from "./StudyViews";
import { SYLLABUS } from "./syllabus";
import type {
  AuthenticatedUser,
  DocumentResponse,
  StudyTopic,
  TopicProgress,
} from "./types";

interface Credentials {
  email: string;
  password: string;
}

interface PlanState {
  completed: Record<string, string>;
  inProgressId: string | null;
}

const PLAN_STORAGE_KEY = "procuraduria-prep.study-plan.v1";
const EMPTY_STUDY_DATA: StudyData = {
  progress: null,
  reviews: [],
  modules: [],
  topics: [],
  topicProgress: {},
  quizzes: [],
  failures: [],
};

function readPlan(): PlanState {
  try {
    const stored = window.localStorage.getItem(PLAN_STORAGE_KEY);
    if (!stored) return { completed: {}, inProgressId: null };
    const parsed: unknown = JSON.parse(stored);
    if (!parsed || typeof parsed !== "object") return { completed: {}, inProgressId: null };
    const record = parsed as { completed?: unknown; inProgressId?: unknown };
    const completed = record.completed && typeof record.completed === "object" && !Array.isArray(record.completed)
      ? Object.fromEntries(Object.entries(record.completed).filter(([id, date]) =>
        SYLLABUS.some((topic) => topic.id === id) && typeof date === "string"))
      : {};
    const inProgressId = typeof record.inProgressId === "string" && SYLLABUS.some((topic) => topic.id === record.inProgressId)
      ? record.inProgressId
      : null;
    return { completed, inProgressId };
  } catch {
    return { completed: {}, inProgressId: null };
  }
}

async function loadStudyData(auth: Credentials): Promise<{ data: StudyData; documents: DocumentResponse[]; documentsError: boolean }> {
  const [progressResult, reviewsResult, modulesResult, quizzesResult, documentsResult] = await Promise.allSettled([
    getOverallProgress(auth),
    getDueReviews(auth),
    getStudyModules(auth),
    getQuizzes(auth),
    getDocuments(auth),
  ]);
  const failures: string[] = [];
  if (progressResult.status === "rejected") failures.push("progreso");
  if (reviewsResult.status === "rejected") failures.push("repasos");
  if (modulesResult.status === "rejected") failures.push("temas");
  if (quizzesResult.status === "rejected") failures.push("cuestionarios");
  const progress = progressResult.status === "fulfilled" ? progressResult.value : null;
  const reviews = reviewsResult.status === "fulfilled" ? reviewsResult.value : [];
  const modules = modulesResult.status === "fulfilled" ? modulesResult.value : [];
  const quizzes = quizzesResult.status === "fulfilled" ? quizzesResult.value : [];
  const documents = documentsResult.status === "fulfilled" ? documentsResult.value.content : [];
  if (documentsResult.status === "rejected") failures.push("documentos");

  const topicResults = await Promise.allSettled(modules.map((module) => getModuleTopics(auth, module.id)));
  const topics: StudyTopic[] = [];
  topicResults.forEach((result) => {
    if (result.status === "fulfilled") topics.push(...result.value);
    else failures.push("temas");
  });
  const topicProgressResults = await Promise.allSettled(topics.map((topic) => getTopicProgress(auth, topic.id)));
  const topicProgress: Record<string, TopicProgress> = {};
  topicProgressResults.forEach((result) => {
    if (result.status === "fulfilled") topicProgress[result.value.topicId] = result.value;
    else failures.push("avance por tema");
  });

  return {
    data: { progress, reviews, modules, topics, topicProgress, quizzes, failures: [...new Set(failures)] },
    documents,
    documentsError: documentsResult.status === "rejected",
  };
}

function NavIcon({ name }: { name: string }) {
  const paths: Record<string, string> = {
    grid: "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z",
    calendar: "M7 2v4 M17 2v4 M3 9h18 M5 4h14a2 2 0 0 1 2 2v13H3V6a2 2 0 0 1 2-2z M8 13h3 M8 17h3",
    book: "M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17H6.5A2.5 2.5 0 0 0 4 21z M4 4.5v16.5 M8 6h8 M8 10h8",
    spark: "m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3z M19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16z",
    check: "M4 4h16v16H4z M8 12l2.5 2.5L16 9",
    refresh: "M20 7v5h-5 M4 17v-5h5 M5.6 9A7 7 0 0 1 18 6l2 2 M4 16l2 2a7 7 0 0 0 12.4-3",
    chart: "M3 3v18h18 M8 16v-5 M13 16V6 M18 16v-8",
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24" className="nav-icon" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"><path d={paths[name] ?? paths.grid} /></svg>;
}

function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [studyData, setStudyData] = useState<StudyData>(EMPTY_STUDY_DATA);
  const [selectedSection, setSelectedSection] = useState<SectionId>("dashboard");
  const [aiInitialTopic, setAiInitialTopic] = useState("");
  const [aiInitialSourceDocumentId, setAiInitialSourceDocumentId] = useState("");
  const [plan, setPlan] = useState<PlanState>(readPlan);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isLoadingData, setIsLoadingData] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [documentsWarning, setDocumentsWarning] = useState("");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  useEffect(() => {
    if (!credentials) return;
    try {
      window.localStorage.setItem(PLAN_STORAGE_KEY, JSON.stringify(plan));
    } catch {
      setDocumentsWarning("El avance del plan solo se conservará durante esta sesión del navegador.");
    }
  }, [plan, credentials]);

  function updatePlan(updater: (current: PlanState) => PlanState) {
    setPlan(updater);
  }

  async function refreshStudyData(auth: Credentials) {
    const loaded = await loadStudyData(auth);
    setStudyData(loaded.data);
    setDocuments(loaded.documents.filter((document) => document.status === "COMPLETED"));
    setDocumentsWarning(loaded.documentsError ? "No se pudo cargar la lista de documentos; puedes consultar todos los documentos." : "");
  }

  async function connect(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isConnecting) return;
    const nextCredentials = { email: email.trim(), password };
    setIsConnecting(true);
    setErrorMessage("");
    try {
      const currentUser = await getCurrentUser(nextCredentials);
      setCredentials(nextCredentials);
      setUser(currentUser);
      setPassword("");
      setSelectedSection("dashboard");
      setIsLoadingData(true);
      await refreshStudyData(nextCredentials);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsLoadingData(false);
      setIsConnecting(false);
    }
  }

  async function handleUploadDocument(file: File) {
    if (!credentials) return;
    await uploadDocument(credentials, file);
    await refreshStudyData(credentials);
  }

  async function handleDeleteDocument(documentId: string) {
    if (!credentials) return;
    await deleteDocument(credentials, documentId);
    await refreshStudyData(credentials);
  }

  function disconnect() {
    setCredentials(null);
    setUser(null);
    setDocuments([]);
    setStudyData(EMPTY_STUDY_DATA);
    setSelectedSection("dashboard");
    setErrorMessage("");
    setDocumentsWarning("");
    setPassword("");
  }

  function navigate(section: SectionId) {
    setSelectedSection(section);
    setMobileNavOpen(false);
    setErrorMessage("");
  }

  function startTopic(topicTitle: string, sourceDocumentId = "") {
    const topic = SYLLABUS.find((item) => item.title === topicTitle);
    if (topic) beginTopic(topic.id);
    setAiInitialTopic(topicTitle);
    setAiInitialSourceDocumentId(sourceDocumentId);
    navigate("ai");
  }

  function toggleComplete(topicId: string) {
    updatePlan((current) => {
      if (current.completed[topicId]) {
        const completed = { ...current.completed };
        delete completed[topicId];
        return { ...current, completed };
      }
      return {
        completed: { ...current.completed, [topicId]: new Date().toISOString() },
        inProgressId: current.inProgressId === topicId ? null : current.inProgressId,
      };
    });
  }

  function beginTopic(topicId: string) {
    updatePlan((current) => ({ ...current, inProgressId: topicId }));
  }

  function renderSection() {
    if (!credentials || !user) return null;
    switch (selectedSection) {
      case "dashboard":
        return <DashboardView user={user} data={studyData} plan={plan} onStartTopic={startTopic} onNavigate={navigate} />;
      case "plan":
        return <StudyPlanView plan={plan} onStartTopic={startTopic} onBegin={beginTopic} onToggleComplete={toggleComplete} />;
      case "syllabus":
        return <SyllabusView plan={plan} onStartTopic={startTopic} onToggleComplete={toggleComplete} />;
      case "materials":
        return <MaterialsView auth={credentials} documents={documents} onUpload={handleUploadDocument} onDelete={handleDeleteDocument} />;
      case "ai":
        return <AIStudyView key={`${aiInitialTopic}:${aiInitialSourceDocumentId}`} auth={credentials} documents={documents} initialTopic={aiInitialTopic} initialSourceDocumentId={aiInitialSourceDocumentId} />;
      case "simulators":
        return <SimulatorsView quizzes={studyData.quizzes} auth={credentials} onAttemptFinished={() => void refreshStudyData(credentials)} />;
      case "reviews":
        return <ReviewsView reviews={studyData.reviews} auth={credentials} topics={studyData.topics} documents={documents} onStudyTopic={startTopic} onConsultDocument={startTopic} onReviewed={(id) => setStudyData((current) => ({ ...current, reviews: current.reviews.filter((card) => card.id !== id) }))} />;
      case "progress":
        return <ProgressView plan={plan} data={studyData} />;
      default:
        return null;
    }
  }

  if (!credentials) {
    return <main className="login-screen">
      <a className="brand login-brand" href="/" aria-label="Procuraduría Prep, inicio"><span className="brand-mark">P</span><span>Procuraduría <strong>Prep</strong></span></a>
      <section className="panel connection-card" aria-labelledby="connection-title">
        <div><p className="eyebrow">PREPARACIÓN PERSONALIZADA</p><h1 id="connection-title">Tu próximo logro empieza aquí</h1><p className="muted-copy">Organiza tu estudio, aprende con tus documentos y mide tu avance.</p></div>
        <form onSubmit={connect} className="connection-form">
          <label htmlFor="email">Correo electrónico</label><input id="email" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} />
          <label htmlFor="password">Contraseña</label><input id="password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
          {errorMessage && <p className="error-banner" role="alert">{errorMessage}</p>}
          <button type="submit" className="primary-button" disabled={isConnecting}>{isConnecting ? <><span className="spinner" aria-hidden="true" /> Conectando…</> : "Entrar a mi espacio"}</button>
          <p className="security-note">Tus credenciales se usan solo en esta sesión. Usa HTTPS fuera del entorno local.</p>
        </form>
      </section>
      <p className="login-caption">Una ruta clara para llegar preparado.</p>
    </main>;
  }

  return <div className="workspace">
    {mobileNavOpen && <button className="sidebar-backdrop" aria-label="Cerrar navegación" onClick={() => setMobileNavOpen(false)} />}
    <aside className={`sidebar ${mobileNavOpen ? "sidebar-open" : ""}`} aria-label="Navegación principal">
      <button className="brand sidebar-brand brand-button" onClick={() => navigate("dashboard")}><span className="brand-mark">P</span><span>Procuraduría <strong>Prep</strong></span></button>
      <div className="sidebar-caption">TU PREPARACIÓN</div>
      <nav aria-label="Secciones de estudio" className="main-nav">
        {NAV_ITEMS.map((item) => <button key={item.id} className={`nav-item ${selectedSection === item.id ? "active" : ""}`} onClick={() => navigate(item.id)} aria-current={selectedSection === item.id ? "page" : undefined}><NavIcon name={item.icon} /><span>{item.label}</span></button>)}
      </nav>
      <div className="sidebar-spacer" />
      <div className="sidebar-exam"><span className="eyebrow">TU CONVOCATORIA</span><strong>Técnico Investigador</strong><span>Código 4TI-15</span></div>
      <div className="sidebar-user"><span className="avatar" aria-hidden="true">{(user?.name || user?.email || "E").slice(0, 1).toUpperCase()}</span><div className="user-data"><strong>{user?.name || "Estudiante"}</strong><span>{user?.email}</span></div><button className="logout-button" onClick={disconnect} aria-label="Cerrar sesión" title="Cerrar sesión">↗</button></div>
    </aside>
    <div className="main-column">
      <header className="workspace-header"><button className="mobile-menu-button" aria-label="Abrir navegación" aria-expanded={mobileNavOpen} onClick={() => setMobileNavOpen((open) => !open)}><span /><span /><span /></button><div className="breadcrumb"><span>Mi espacio</span><span aria-hidden="true">/</span><strong>{NAV_ITEMS.find((item) => item.id === selectedSection)?.label}</strong></div><div className="header-right"><span className="connection-indicator"><span aria-hidden="true" />Conectado</span><span className="header-user">{user?.name || user?.email}</span></div></header>
      <main className="content-area">
        {isLoadingData && <div className="loading-banner" role="status"><span className="spinner spinner-dark" aria-hidden="true" /> Cargando tu espacio de estudio…</div>}
        {documentsWarning && <p className="notice notice-info" role="status">{documentsWarning}</p>}
        {renderSection()}
      </main>
      <footer className="workspace-footer"><span>Procuraduría Prep</span><span>Tu preparación, a tu ritmo.</span></footer>
    </div>
  </div>;
}

export default App;
