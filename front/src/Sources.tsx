import { useState } from "react";
import { getDocumentText, getErrorMessage } from "./api";
import type { DocumentSourceReference, DocumentTextResponse } from "./types";

interface SourcesProps {
  sources: DocumentSourceReference[];
  auth: { email: string; password: string };
}

function SourceCard({
  source,
  auth,
}: {
  source: DocumentSourceReference;
  auth: { email: string; password: string };
}) {
  const [documentText, setDocumentText] = useState<DocumentTextResponse | null>(null);
  const [loadingText, setLoadingText] = useState(false);
  const [textError, setTextError] = useState("");

  async function toggleDocumentText() {
    if (documentText) {
      setDocumentText(null);
      return;
    }
    setTextError("");
    setLoadingText(true);
    try {
      setDocumentText(await getDocumentText(auth, source.documentId));
    } catch (error) {
      setTextError(getErrorMessage(error));
    } finally {
      setLoadingText(false);
    }
  }

  return (
    <details className="source-card">
      <summary>
        <span className="source-name">{source.documentName || "Documento sin nombre"}</span>
        <span className="source-reference">
          {source.pageNumber != null ? `Página ${source.pageNumber}` : "Sin número de página"}
          <span aria-hidden="true"> · </span>
          Fragmento {source.chunkIndex}
        </span>
      </summary>
      <div className="source-detail">
        <p>
          Referencia: {source.pageNumber != null ? `página ${source.pageNumber}` : "página no disponible"}{" "}
          · Fragmento {source.chunkIndex}
        </p>
        <button
          type="button"
          className="text-button"
          onClick={toggleDocumentText}
          disabled={loadingText}
          aria-expanded={documentText !== null}
        >
          {loadingText
            ? "Cargando texto…"
            : documentText
              ? "Ocultar texto del documento"
              : "Consultar texto del documento"}
        </button>
        {textError && (
          <p className="inline-error" role="alert">
            {textError}
          </p>
        )}
        {documentText && (
          <div className="document-text" aria-label={`Texto extraído de ${documentText.originalName}`}>
            <p className="document-text-label">
              Texto extraído completo; el backend no proporciona el extracto exacto de este fragmento.
            </p>
            <pre>{documentText.text}</pre>
          </div>
        )}
      </div>
    </details>
  );
}

export function Sources({ sources, auth }: SourcesProps) {
  if (sources.length === 0) {
    return (
      <p className="no-sources">
        Esta respuesta no incluye referencias documentales. No se encontraron fuentes para asociar.
      </p>
    );
  }

  return (
    <details className="sources-panel">
      <summary>
        <span>Fuentes consultadas</span>
        <span className="source-count">
          {sources.length} {sources.length === 1 ? "referencia" : "referencias"}
        </span>
      </summary>
      <div className="source-list">
        {sources.map((source, index) => (
          <SourceCard key={`${source.documentId}-${source.chunkIndex}-${index}`} source={source} auth={auth} />
        ))}
      </div>
    </details>
  );
}
