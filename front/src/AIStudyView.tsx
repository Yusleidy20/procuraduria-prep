import { FormEvent, useState } from "react";
import { explainTopic, getErrorMessage } from "./api";
import { ResponseContent } from "./ResponseContent";
import { Sources } from "./Sources";
import type { DocumentResponse, TopicExplanationResponse } from "./types";

interface AIStudyViewProps {
  auth: { email: string; password: string };
  documents: DocumentResponse[];
  initialTopic: string;
  initialSourceDocumentId: string;
}

function isInsufficientContext(explanation: string) {
  return /no encontr[eé] informaci[oó]n suficiente/i.test(explanation);
}

export function AIStudyView({ auth, documents, initialTopic, initialSourceDocumentId }: AIStudyViewProps) {
  const [topicName, setTopicName] = useState(initialTopic);
  const [learnerLevel, setLearnerLevel] = useState("Inicial");
  const [sourceDocumentId, setSourceDocumentId] = useState(initialSourceDocumentId);
  const [response, setResponse] = useState<TopicExplanationResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const completedDocuments = documents.filter((document) => document.status === "COMPLETED");

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;
    setIsSubmitting(true);
    setErrorMessage("");
    setResponse(null);
    try {
      const result = await explainTopic(auth, {
        topicName: topicName.trim(),
        learnerLevel,
        ...(sourceDocumentId ? { sourceDocumentId } : {}),
      });
      setResponse(result);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  const hasInsufficientContext = response ? isInsufficientContext(response.explanation) : false;

  return <div className="view-stack ai-view">
    <header className="view-heading"><div><p className="eyebrow">TUTOR DOCUMENTAL · RESPUESTAS CON REFERENCIAS</p><h1>Estudiar con IA</h1><p>Pregunta sobre tus documentos; revisa la explicación y las fuentes recuperadas.</p></div></header>
    <section className="panel query-card" aria-labelledby="query-title">
      <div className="panel-heading"><div><p className="eyebrow">CONSULTA DOCUMENTAL</p><h2 id="query-title">¿Qué quieres entender?</h2></div><span className="panel-mark" aria-hidden="true">✦</span></div>
      <form onSubmit={submitQuestion}>
        <label htmlFor="topic">Tema o pregunta</label>
        <textarea id="topic" required maxLength={160} rows={3} placeholder="Escribe un tema del temario o una pregunta concreta" value={topicName} onChange={(event) => setTopicName(event.target.value)} />
        <div className="form-grid">
          <div><label htmlFor="learner-level">Nivel de estudio</label><select id="learner-level" value={learnerLevel} onChange={(event) => setLearnerLevel(event.target.value)}><option>Inicial</option><option>Intermedio</option><option>Avanzado</option></select></div>
          <div><label htmlFor="source-document">Documento</label><select id="source-document" value={sourceDocumentId} onChange={(event) => setSourceDocumentId(event.target.value)}><option value="">Buscar en todos los documentos</option>{completedDocuments.map((document) => <option key={document.id} value={document.id}>{document.originalName}</option>)}</select></div>
        </div>
        {errorMessage && <p className="error-banner" role="alert">{errorMessage}</p>}
        <div className="submit-row"><p className="privacy-note">La respuesta y sus referencias provienen del backend.</p><button type="submit" className="primary-button" disabled={isSubmitting || !topicName.trim()}>{isSubmitting ? <><span className="spinner" aria-hidden="true" /> Consultando documentos…</> : <>Consultar <span aria-hidden="true">↗</span></>}</button></div>
      </form>
    </section>
    {isSubmitting && <div className="loading-card" role="status" aria-live="polite"><span className="spinner spinner-dark" aria-hidden="true" /><div><strong>Buscando evidencia en tus documentos</strong><p>La consulta puede tardar unos segundos.</p></div></div>}
    {response && !isSubmitting && <article className={`panel answer-card${hasInsufficientContext ? " insufficient" : ""}`} aria-labelledby="answer-title">
      <div className="answer-heading"><div><p className="eyebrow">{hasInsufficientContext ? "EVIDENCIA INSUFICIENTE" : "EXPLICACIÓN"}</p><h2 id="answer-title">{topicName.trim()}</h2></div><span className="evidence-badge"><span aria-hidden="true">{hasInsufficientContext ? "!" : "✓"}</span>{hasInsufficientContext ? "Sin evidencia suficiente" : response.sources.length ? "Con fuentes" : "Sin fuentes asociadas"}</span></div>
      {hasInsufficientContext && <div className="context-notice" role="status"><strong>No encontré información suficiente</strong><p>No encontré información suficiente en los documentos consultados para responder esta pregunta. Puedes revisar el documento o formular una pregunta más específica.</p></div>}
      <ResponseContent text={response.explanation} />
      {response.keyPoints && <section className="key-points" aria-labelledby="key-points-title"><h3 id="key-points-title">Puntos clave</h3><ResponseContent text={response.keyPoints} /></section>}
      <Sources sources={response.sources ?? []} auth={auth} />
    </article>}
  </div>;
}
