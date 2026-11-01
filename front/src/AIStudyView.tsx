import { FormEvent, useEffect, useRef, useState } from "react";
import {
  createStudyConversation,
  deleteStudyConversation,
  getErrorMessage,
  getStudyConversation,
  listStudyConversations,
  renameStudyConversation,
  sendStudyMessage,
} from "./api";
import { ResponseContent } from "./ResponseContent";
import { Sources } from "./Sources";
import type {
  DocumentResponse,
  StudyChatMessage,
  StudyConversationSummary,
} from "./types";

interface AIStudyViewProps {
  auth: { email: string; password: string };
  documents: DocumentResponse[];
  initialTopic: string;
  initialSourceDocumentId: string;
}

function isInsufficientContext(explanation: string) {
  return /no encontr[eé] informaci[oó]n suficiente|no encontr[eé] fragmentos relevantes|biblioteca a[uú]n no tiene documentos/i
    .test(explanation);
}

export function AIStudyView({ auth, documents, initialTopic, initialSourceDocumentId }: AIStudyViewProps) {
  const [conversations, setConversations] = useState<StudyConversationSummary[]>([]);
  const [activeConversationId, setActiveConversationId] = useState("");
  const [messages, setMessages] = useState<StudyChatMessage[]>([]);
  const [topicName, setTopicName] = useState(initialTopic);
  const [learnerLevel, setLearnerLevel] = useState("Inicial");
  const [sourceDocumentId, setSourceDocumentId] = useState(initialSourceDocumentId);
  const [isLoadingHistory, setIsLoadingHistory] = useState(true);
  const [isLoadingConversation, setIsLoadingConversation] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingQuestion, setPendingQuestion] = useState("");
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [error, setError] = useState("");
  const [editingConversationId, setEditingConversationId] = useState("");
  const [renameDraft, setRenameDraft] = useState("");
  const activeLoadRequest = useRef(0);
  const completedDocuments = documents.filter((document) => document.status === "COMPLETED");

  useEffect(() => {
    let cancelled = false;
    async function loadHistory() {
      setIsLoadingHistory(true);
      setError("");
      try {
        const history = await listStudyConversations(auth);
        if (cancelled) return;
        setConversations(history);
        if (!initialTopic.trim() && history.length > 0) {
          const requestId = ++activeLoadRequest.current;
          setActiveConversationId(history[0].id);
          setIsLoadingConversation(true);
          try {
            const detail = await getStudyConversation(auth, history[0].id);
            if (!cancelled && requestId === activeLoadRequest.current) setMessages(detail.messages);
          } catch (loadError) {
            if (!cancelled && requestId === activeLoadRequest.current) setError(getErrorMessage(loadError));
          } finally {
            if (!cancelled && requestId === activeLoadRequest.current) setIsLoadingConversation(false);
          }
        }
      } catch (loadError) {
        if (!cancelled) setError(getErrorMessage(loadError));
      } finally {
        if (!cancelled) setIsLoadingHistory(false);
      }
    }
    void loadHistory();
    return () => { cancelled = true; };
  }, [auth.email, auth.password, initialTopic]);

  async function openConversation(conversationId: string) {
    if (isSubmitting || conversationId === activeConversationId) {
      setIsHistoryOpen(false);
      return;
    }
    setError("");
    const requestId = ++activeLoadRequest.current;
    setIsLoadingConversation(true);
    setActiveConversationId(conversationId);
    setMessages([]);
    setIsHistoryOpen(false);
    try {
      const detail = await getStudyConversation(auth, conversationId);
      if (requestId !== activeLoadRequest.current) return;
      setMessages(detail.messages);
      setConversations((current) => current.map((item) =>
        item.id === detail.conversation.id ? detail.conversation : item));
    } catch (loadError) {
      if (requestId === activeLoadRequest.current) setError(getErrorMessage(loadError));
    } finally {
      if (requestId === activeLoadRequest.current) setIsLoadingConversation(false);
    }
  }

  async function startNewConversation() {
    if (isSubmitting) return;
    setError("");
    const requestId = ++activeLoadRequest.current;
    setIsLoadingConversation(true);
    try {
      const conversation = await createStudyConversation(auth);
      if (requestId !== activeLoadRequest.current) return;
      setConversations((current) => [conversation, ...current]);
      setActiveConversationId(conversation.id);
      setMessages([]);
      setTopicName("");
      setIsHistoryOpen(false);
    } catch (createError) {
      if (requestId === activeLoadRequest.current) setError(getErrorMessage(createError));
    } finally {
      if (requestId === activeLoadRequest.current) setIsLoadingConversation(false);
    }
  }

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const question = topicName.trim();
    if (isSubmitting || isLoadingHistory || !question) return;

    setError("");
    setTopicName("");
    setPendingQuestion(question);
    setIsSubmitting(true);
    ++activeLoadRequest.current;
    try {
      let conversationId = activeConversationId;
      if (!conversationId) {
        const created = await createStudyConversation(auth);
        conversationId = created.id;
        setActiveConversationId(created.id);
        setConversations((current) => [created, ...current]);
      }
      const response = await sendStudyMessage(auth, conversationId, {
        content: question,
        learnerLevel,
        ...(sourceDocumentId ? { sourceDocumentId } : {}),
      });
      setMessages((current) => [...current, response.userMessage, response.assistantMessage]);
      setConversations((current) => [
        response.conversation,
        ...current.filter((item) => item.id !== response.conversation.id),
      ]);
    } catch (sendError) {
      setTopicName(question);
      setError(getErrorMessage(sendError));
    } finally {
      setPendingQuestion("");
      setIsSubmitting(false);
    }
  }

  async function saveConversationTitle(event: FormEvent<HTMLFormElement>, conversationId: string) {
    event.preventDefault();
    const title = renameDraft.trim();
    if (!title || !editingConversationId) return;
    setError("");
    try {
      const renamed = await renameStudyConversation(auth, conversationId, title);
      setConversations((current) => current.map((item) => item.id === renamed.id ? renamed : item));
      setEditingConversationId("");
    } catch (renameError) {
      setError(getErrorMessage(renameError));
    }
  }

  async function removeConversation(conversationId: string) {
    if (isSubmitting) return;
    setError("");
    try {
      await deleteStudyConversation(auth, conversationId);
      const remaining = conversations.filter((item) => item.id !== conversationId);
      setConversations(remaining);
      if (activeConversationId === conversationId) {
        ++activeLoadRequest.current;
        setActiveConversationId("");
        setMessages([]);
        if (remaining[0]) await openConversation(remaining[0].id);
      }
    } catch (deleteError) {
      setError(getErrorMessage(deleteError));
    }
  }

  return <div className="view-stack ai-view">
    <header className="view-heading">
      <div>
        <p className="eyebrow">TUTOR DOCUMENTAL · BIBLIOTECA PERSONAL</p>
        <h1>Estudiar con IA</h1>
        <p>Conversa para estudiar. Recuperaré evidencia de tus documentos en cada respuesta.</p>
      </div>
      <button type="button" className="secondary-button ai-history-toggle" onClick={() => setIsHistoryOpen(true)}>
        Historial
      </button>
    </header>

    <div className="study-chat-layout">
      {isHistoryOpen && <button
        type="button"
        className="ai-history-backdrop"
        aria-label="Cerrar historial"
        onClick={() => setIsHistoryOpen(false)}
      />}
      <aside className={`ai-history-panel${isHistoryOpen ? " open" : ""}`} aria-label="Historial de conversaciones">
        <div className="ai-history-heading">
          <div><p className="eyebrow">TUS SESIONES</p><h2>Conversaciones</h2></div>
          <button type="button" className="ai-history-close" aria-label="Cerrar historial" onClick={() => setIsHistoryOpen(false)}>×</button>
        </div>
        <button type="button" className="primary-button ai-new-conversation" onClick={startNewConversation} disabled={isLoadingHistory || isLoadingConversation || isSubmitting}>
          <span aria-hidden="true">＋</span> Nueva conversación
        </button>
        {isLoadingHistory && <p className="ai-history-message" role="status">Cargando historial…</p>}
        {!isLoadingHistory && conversations.length === 0 && <p className="ai-history-message">Tus conversaciones guardadas aparecerán aquí.</p>}
        <nav className="ai-conversation-list" aria-label="Conversaciones guardadas">
          {conversations.map((conversation) => <div
            className={`ai-conversation-item${conversation.id === activeConversationId ? " active" : ""}`}
            key={conversation.id}
          >
            {editingConversationId === conversation.id
              ? <form className="ai-rename-form" onSubmit={(event) => void saveConversationTitle(event, conversation.id)}>
                <label className="sr-only" htmlFor={`rename-${conversation.id}`}>Nuevo título</label>
                <input id={`rename-${conversation.id}`} autoFocus maxLength={120} value={renameDraft} onChange={(event) => setRenameDraft(event.target.value)} />
                <button type="submit" aria-label="Guardar título">✓</button>
              </form>
              : <>
                <button
                  type="button"
                  className="ai-conversation-select"
                  aria-current={conversation.id === activeConversationId ? "page" : undefined}
                  onClick={() => void openConversation(conversation.id)}
                >
                  <strong>{conversation.title}</strong>
                  <span>{conversation.messageCount} {conversation.messageCount === 1 ? "mensaje" : "mensajes"}</span>
                </button>
                <div className="ai-conversation-actions">
                  <button type="button" aria-label={`Renombrar conversación: ${conversation.title}`} onClick={() => {
                    setEditingConversationId(conversation.id);
                    setRenameDraft(conversation.title);
                  }}>Renombrar</button>
                  <button type="button" aria-label={`Eliminar conversación: ${conversation.title}`} onClick={() => void removeConversation(conversation.id)}>Eliminar</button>
                </div>
              </>}
          </div>)}
        </nav>
      </aside>

      <section className="ai-chat-main" aria-label="Conversación de estudio">
        <div className="ai-chat-messages" aria-live="polite">
          {messages.length === 0 && !pendingQuestion && !isLoadingConversation && <div className="ai-welcome">
            <span className="ai-welcome-mark" aria-hidden="true">✦</span>
            <h2>¿Qué quieres estudiar hoy?</h2>
            <p>Pregunta con tus propias palabras, pide ejemplos o profundiza en un concepto. La biblioteca se consulta automáticamente.</p>
            <div className="ai-suggestions" aria-label="Preguntas sugeridas">
              {["Explícame la organización del Estado colombiano", "¿Qué es una falta disciplinaria?", "Hazme preguntas tipo examen"].map((suggestion) =>
                <button type="button" key={suggestion} onClick={() => setTopicName(suggestion)}>{suggestion}</button>)}
            </div>
          </div>}
          {isLoadingConversation && <div className="ai-history-message" role="status">Cargando conversación…</div>}
          {messages.map((message) => message.role === "USER"
            ? <div className="ai-question" key={message.id}><span className="ai-user-mark" aria-hidden="true">Tú</span><p>{message.content}</p></div>
            : <article className={`panel answer-card${isInsufficientContext(message.content) ? " insufficient" : ""}`} key={message.id}>
              <div className="answer-heading">
                <div><p className="eyebrow">{isInsufficientContext(message.content) ? "EVIDENCIA INSUFICIENTE" : "TUTOR DOCUMENTAL"}</p><h2>Respuesta</h2></div>
                <span className="evidence-badge"><span aria-hidden="true">{message.sources.length ? "✓" : "!"}</span>{message.sources.length ? "Con fuentes" : "Sin fuentes asociadas"}</span>
              </div>
              {isInsufficientContext(message.content) && <div className="context-notice" role="status"><strong>No encontré información suficiente</strong><p>La biblioteca no aporta evidencia suficiente para responder con seguridad. Agrega material pertinente o formula la pregunta con más contexto.</p></div>}
              <ResponseContent text={message.content} />
              {message.keyPoints && <section className="key-points"><h3>Puntos clave</h3><ResponseContent text={message.keyPoints} /></section>}
              <Sources sources={message.sources} auth={auth} />
            </article>)}
          {pendingQuestion && <div className="ai-pending-turn">
            <div className="ai-question"><span className="ai-user-mark" aria-hidden="true">Tú</span><p>{pendingQuestion}</p></div>
            <div className="ai-answer-loading" role="status" aria-live="polite">
              <span className="spinner spinner-dark" aria-hidden="true" />
              <div><strong>Buscando en tu biblioteca</strong><p>Recuperando fragmentos relevantes y preparando una respuesta.</p></div>
            </div>
          </div>}
        </div>
        {error && <p className="error-banner ai-chat-error" role="alert">{error}</p>}
        <section className="panel ai-composer" aria-label="Escribir pregunta">
          <form onSubmit={submitQuestion}>
            <label className="sr-only" htmlFor="topic">Tu pregunta</label>
            <textarea id="topic" required maxLength={2000} rows={3} placeholder="Pregunta o pide una explicación para continuar estudiando…" value={topicName} onChange={(event) => setTopicName(event.target.value)} />
            <div className="ai-composer-options">
              <label htmlFor="learner-level">Nivel</label>
              <select id="learner-level" value={learnerLevel} onChange={(event) => setLearnerLevel(event.target.value)}><option>Inicial</option><option>Intermedio</option><option>Avanzado</option></select>
              <details className="ai-document-filter">
                <summary>{sourceDocumentId ? "Documento específico" : "Toda mi biblioteca"}</summary>
                <label htmlFor="source-document">Limitar búsqueda a un documento (opcional)</label>
                <select id="source-document" value={sourceDocumentId} onChange={(event) => setSourceDocumentId(event.target.value)}>
                  <option value="">Buscar en toda mi biblioteca</option>
                  {completedDocuments.map((document) => <option key={document.id} value={document.id}>{document.originalName}</option>)}
                </select>
              </details>
              <span className="ai-composer-spacer" />
              <button type="submit" className="primary-button" disabled={isSubmitting || isLoadingHistory || !topicName.trim()}>
                {isSubmitting ? <><span className="spinner" aria-hidden="true" /> Buscando…</> : <>Enviar <span aria-hidden="true">↗</span></>}
              </button>
            </div>
            <p className="privacy-note">Las respuestas se fundamentan en fragmentos recuperados. No se envía la biblioteca completa al modelo.</p>
          </form>
        </section>
      </section>
    </div>
  </div>;
}
