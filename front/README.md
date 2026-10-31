# Procuraduría Prep — frontend de consulta documental

Interfaz React + TypeScript con dashboard personalizado, navegación por agenda, temario, tutor documental, simulacros, repasos y progreso.

## Desarrollo

```powershell
npm install
npm run dev
npm test
npm run build
```

En desarrollo, Vite reenvía `/api` a `http://localhost:8080`. Para cambiar el backend local, define `VITE_API_PROXY_TARGET`; para compilar una URL de API diferente, define `VITE_API_BASE_URL`. En producción se recomienda publicar el frontend y la API bajo el mismo origen mediante un proxy HTTPS.

La autenticación usa HTTP Basic y solicita un token CSRF antes de la consulta de escritura, conforme al backend. El correo y la contraseña solo permanecen en memoria mientras se usa la interfaz; no se guardan en almacenamiento local. La cuenta debe existir previamente en el backend.

## Contratos integrados

- `GET /api/v1/auth/me` valida las credenciales.
- `GET /api/v1/documents?page=0&size=100&sort=uploadedAt,desc` carga la selección opcional de documentos procesados.
- `POST /api/v1/ai/explanations` recibe `topicName`, `learnerLevel` y, opcionalmente, `sourceDocumentId`. La ausencia de ambos campos de fuente mantiene la búsqueda en todos los documentos activos.
- `GET /api/v1/auth/csrf` obtiene el encabezado/token para la solicitud de explicación.
- `GET /api/v1/documents/{documentId}/text` consulta el texto extraído del documento.
- `GET /api/v1/progress` y `GET /api/v1/progress/topics/{topicId}` aportan estadísticas de la plataforma.
- `GET /api/v1/progress/reviews/due` carga las tarjetas realmente pendientes; los resultados se registran con `POST /api/v1/flashcards/{flashcardId}/reviews`.
- Los simulacros usan los cuestionarios y preguntas reales de `GET /api/v1/quizzes` y `GET /api/v1/quizzes/{quizId}/questions`, y persisten intentos y resultados mediante los endpoints existentes de intentos.

Las referencias de explicación solo incluyen los metadatos definidos por el backend: documento, página nullable e índice de fragmento. El backend no devuelve el texto de cada fragmento ni una ruta para descargar el archivo original; por ello la interfaz no inventa extractos ni enlaces directos.

El plan y las sesiones marcadas como completadas son una agenda sugerida guardada localmente en este navegador; no se sincronizan ni representan tiempo medido. Las métricas de dominio, repasos, respuestas y cuestionarios provienen de la API. El temario utiliza la lista proporcionada para Técnico Investigador, código 4TI-15; el PDF `mi concurso(2).pdf` no estaba incluido en el workspace para comprobar la convocatoria original y no se añadieron subtemas jurídicos.
