# Procuraduría Prep — Backend

API REST en Java 21 y Spring Boot para organizar el contenido de estudio. Incluye módulos/temas, documentos, tarjetas con historial de repasos, cuestionarios con intentos evaluables, progreso agregado, autenticación y generación de contenido con Groq o Gemini seleccionable por configuración.

## Arquitectura inicial

Los paquetes `study`, `documents`, `flashcards` y `quizzes` agrupan entidades, repositorios, DTOs, servicios transaccionales y controladores. `ai` define el contrato de tutoría y valida posibles salidas estructuradas de proveedores; `shared.exception` centraliza errores HTTP y `config` contiene la configuración de seguridad. Las entidades usan UUID, relaciones JPA explícitas y dificultad como enumeración; los controladores solo reciben/devuelven DTOs.

## Requisitos y ejecución

Se requiere Java 21, Maven (o el wrapper incluido) y PostgreSQL. Crea una base de datos local `procuraduria_prep`, configura las variables del archivo `.env.example` en el entorno de ejecución y arranca:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/procuraduria_prep"
$env:DB_USERNAME="procuraduria"
$env:DB_PASSWORD="tu-clave-local"
# Configura únicamente el proveedor seleccionado; no guardes claves en el repositorio.
$env:AI_PRIMARY_PROVIDER="groq"
$env:GROQ_API_KEY="tu-clave-de-Groq"
$env:GROQ_MODEL="id-de-modelo-gratuito-verificado-en-Groq"
# Alternativa: selecciona Gemini.
$env:AI_PRIMARY_PROVIDER="gemini"
$env:GEMINI_API_KEY="tu-clave-de-Gemini"
.\mvnw.cmd spring-boot:run
```

`DB_DDL_AUTO` usa `update` por defecto para crear las tablas faltantes sin borrar tablas existentes. Para entornos compartidos o productivos se recomienda migrar a Flyway/Liquibase y establecer `DB_DDL_AUTO=validate`. No se incluyen credenciales reales. Las pruebas usan H2 en memoria.
Spring Boot lee `GEMINI_API_KEY` del entorno del proceso; no carga automáticamente archivos `.env`. Si ejecutas desde IntelliJ IDEA, configura la variable en **Run/Debug Configurations → Environment variables**. En PowerShell, establece la variable y arranca la aplicación desde la misma ventana para que se herede.

## Endpoints implementados

Las rutas de estudio usan el prefijo `/api/v1/study`; documentos y tarjetas usan `/api/v1/documents` y `/api/v1/flashcards`. Las listas son paginadas con parámetros `page` (desde 0) y `size` (máximo configurable por Spring Data); las respuestas incluyen contenido y metadatos de paginación.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/study/modules` | Crear módulo |
| GET | `/api/v1/study/modules` | Listar módulos |
| GET | `/api/v1/study/modules/{moduleId}` | Consultar módulo |
| POST | `/api/v1/study/modules/{moduleId}/topics` | Crear tema en un módulo |
| GET | `/api/v1/study/modules/{moduleId}/topics` | Listar temas del módulo |
| GET | `/api/v1/study/topics/{topicId}` | Consultar tema |
| POST | `/api/v1/documents` (JSON) | Registrar solo metadatos y una referencia externa |
| POST | `/api/v1/documents` (multipart field `file`) | Subir y procesar un PDF, TXT o Markdown |
| GET | `/api/v1/documents` | Listar documentos activos |
| GET | `/api/v1/documents/{documentId}` | Consultar metadatos y estado |
| GET | `/api/v1/documents/{documentId}/status` | Consultar estado de procesamiento |
| GET | `/api/v1/documents/{documentId}/text` | Consultar el texto extraído de un documento procesado |
| DELETE | `/api/v1/documents/{documentId}` | Desactivar documento (borrado lógico) |
| POST | `/api/v1/flashcards?topicId={topicId}` | Crear tarjeta |
| GET | `/api/v1/flashcards?topicId={topicId}` | Listar tarjetas por tema |
| GET | `/api/v1/flashcards/{flashcardId}` | Consultar tarjeta |
| GET | `/api/v1/flashcards/due` | Listar tarjetas pendientes de repaso |
| POST | `/api/v1/flashcards/{flashcardId}/reviews` | Registrar resultado de repaso |
| POST | `/api/v1/quizzes?topicId={topicId}` | Crear cuestionario con preguntas y opciones |
| POST | `/api/v1/quizzes/mock-exams` | Crear simulacro aleatorio con preguntas de varios temas |
| GET | `/api/v1/quizzes` | Listar cuestionarios (`topicId` opcional) |
| GET | `/api/v1/quizzes/{quizId}` | Consultar cuestionario |
| GET | `/api/v1/quizzes/{quizId}/questions` | Listar preguntas y opciones sin revelar las respuestas correctas |
| POST | `/api/v1/quizzes/{quizId}/attempts` | Iniciar intento |
| POST | `/api/v1/quiz-attempts/{attemptId}/answers` | Registrar una respuesta |
| POST | `/api/v1/quiz-attempts/{attemptId}/finish` | Finalizar intento y obtener calificación y explicaciones |
| GET | `/api/v1/quiz-attempts/{attemptId}` | Consultar intento y, una vez finalizado, sus resultados |
| GET | `/api/v1/progress` | Consultar progreso agregado de esta instalación |
| GET | `/api/v1/progress/topics/{topicId}` | Consultar progreso de un tema |
| GET | `/api/v1/progress/errors` | Consultar preguntas con más respuestas incorrectas (`topicId` opcional) |
| GET | `/api/v1/progress/reviews/due` | Consultar tarjetas pendientes de repaso |
| POST | `/api/v1/auth/register` | Crear la cuenta inicial (solo se permite una cuenta) |
| GET | `/api/v1/auth/me` | Consultar el usuario autenticado |
| GET | `/api/v1/auth/csrf` | Obtener token CSRF para solicitudes de escritura |
| POST | `/api/v1/ai/explanations` | Explicar un tema a partir de material opcional |
| POST | `/api/v1/ai/flashcards` | Generar tarjetas estructuradas |
| POST | `/api/v1/ai/flashcards/save` | Guardar tarjetas generadas en un tema |
| POST | `/api/v1/ai/questions` | Generar preguntas de selección múltiple |
| POST | `/api/v1/ai/questions/save` | Guardar preguntas generadas como cuestionario |

El registro JSON de documentos conserva el flujo de metadatos y referencia externa. La carga multipart guarda el archivo en el almacenamiento local privado configurado, extrae el texto durante la solicitud y devuelve el documento en estado `COMPLETED`. Los PDF escaneados sin capa de texto no están soportados (aún no hay OCR). Las tarjetas pueden referenciar opcionalmente un documento. Un resultado `remembered=true` programa el siguiente repaso en 24 horas; `false`, en 10 minutos. Cada resultado se almacena como un evento de historial.

Ejemplo para crear un módulo:

```http
POST /api/v1/study/modules
Content-Type: application/json

{
  "name": "Derecho constitucional",
  "description": "Constitución y organización del Estado",
  "displayOrder": 1
}
```

La respuesta es `201 Created` con el módulo creado (`id`, `name`, `description`, `displayOrder`, `status`, `createdAt`):

```http
HTTP/1.1 201 Created
Location: /api/v1/study/modules/6d3558cc-2320-4bfc-a0e3-c775974b19f2
Content-Type: application/json

{
  "id": "6d3558cc-2320-4bfc-a0e3-c775974b19f2",
  "name": "Derecho constitucional",
  "description": "Constitución y organización del Estado",
  "displayOrder": 1,
  "status": "ACTIVE",
  "createdAt": "2026-10-31T17:00:00Z"
}
```

Crear un tema:

```http
POST /api/v1/study/modules/{moduleId}/topics
Content-Type: application/json

{
  "name": "Derechos fundamentales",
  "description": "Principios y mecanismos de protección",
  "difficulty": "INTERMEDIATE"
}
```

Registrar un documento:

```http
POST /api/v1/documents
Content-Type: application/json

{
  "originalName": "constitucion.pdf",
  "contentType": "application/pdf",
  "storageReference": "local://documents/constitucion.pdf"
}
```

Subir un documento real (en PowerShell 7, límite predeterminado: 10 MiB):

```powershell
$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/csrf" -SessionVariable session
$headers = @{
    Authorization = "Basic " + [Convert]::ToBase64String(
        [Text.Encoding]::ASCII.GetBytes("estudiante@example.com:tu-clave-local"))
}
$headers[$csrf.headerName] = $csrf.token
Invoke-WebRequest -Method Post -Uri "http://localhost:8080/api/v1/documents" `
    -WebSession $session -Headers $headers -Form @{ file = (Get-Item "C:\ruta\constitucion.pdf") }
```

Consulta el estado y el texto extraído con `GET /api/v1/documents/{documentId}/status` y `GET /api/v1/documents/{documentId}/text`. Se admiten `.pdf`, `.txt` y `.md`; texto codificado en UTF-8. El archivo se guarda con nombre aleatorio, no con el nombre suministrado por el cliente. `DOCUMENT_STORAGE_DIR`, `DOCUMENT_MAX_FILE_SIZE` y `DOCUMENT_MAX_FILE_SIZE_BYTES` se pueden configurar por entorno; evita servir directamente ese directorio desde un servidor web.

Crear una tarjeta (`sourceDocumentId` es opcional):

```http
POST /api/v1/flashcards?topicId={topicId}
Content-Type: application/json

{
  "question": "¿Qué mecanismo protege los derechos fundamentales?",
  "answer": "La acción de tutela.",
  "explanation": "Está prevista en el artículo 86 de la Constitución.",
  "difficulty": "BASIC"
}
```

Registrar un repaso:

```http
POST /api/v1/flashcards/{flashcardId}/reviews
Content-Type: application/json

{ "remembered": false }
```

Crear un cuestionario con preguntas de selección múltiple (índice de opción base cero):

```http
POST /api/v1/quizzes?topicId={topicId}
Content-Type: application/json

{
  "name": "Derechos fundamentales",
  "description": "Evaluación inicial",
  "difficulty": "INTERMEDIATE",
  "durationMinutes": 60,
  "questions": [
    {
      "statement": "¿Qué mecanismo protege los derechos fundamentales?",
      "options": ["Acción de tutela", "Acción popular"],
      "correctOptionIndex": 0,
      "explanation": "La acción de tutela protege derechos fundamentales."
    }
  ]
}
```

Iniciar el intento, registrar cada selección y finalizarlo:

```http
POST /api/v1/quizzes/{quizId}/attempts
POST /api/v1/quiz-attempts/{attemptId}/answers
Content-Type: application/json

{
  "questionId": "{questionId}",
  "selectedOptionIndex": 0
}

POST /api/v1/quiz-attempts/{attemptId}/finish
```

El endpoint de preguntas no expone la opción correcta. Las respuestas y explicaciones solo se devuelven al finalizar el intento. Las preguntas sin responder cuentan como incorrectas al calcular el porcentaje. Cada pregunta admite entre 2 y 6 opciones, y un intento no permite responder dos veces la misma pregunta. Para crear un simulacro con tiempo límite, incluye `durationMinutes` entre 1 y 360 en la solicitud de creación del cuestionario; si se omite, el intento no tiene límite de tiempo. El intento devuelve `deadlineAt`, rechaza respuestas después del vencimiento y se finaliza automáticamente cuando se consulta su resultado tras expirar.

Para armar un simulacro aleatorio con preguntas existentes de varios temas, envía al menos dos `topicIds`, dificultad, cantidad y duración. Debe haber suficientes preguntas disponibles en esos temas y con la dificultad seleccionada. El backend guarda una copia inmutable de las preguntas elegidas para el simulacro; las respuestas se atribuyen al tema original de cada pregunta en las estadísticas.

```http
POST /api/v1/quizzes/mock-exams
Content-Type: application/json

{
  "name": "Simulacro general",
  "description": "Práctica de varias áreas",
  "topicIds": [
    "00000000-0000-0000-0000-000000000001",
    "00000000-0000-0000-0000-000000000002"
  ],
  "difficulty": "INTERMEDIATE",
  "questionCount": 40,
  "durationMinutes": 60
}
```

Consultar el progreso agregado y las preguntas con errores frecuentes:

```http
GET /api/v1/progress
GET /api/v1/progress/topics/{topicId}
GET /api/v1/progress/errors?topicId={topicId}&page=0&size=10
GET /api/v1/progress/reviews/due?page=0&size=20
```

`masteryPercentage` resume las respuestas de cuestionarios y los repasos de tarjetas recordados; es `null` cuando aún no hay actividad. La cuenta personal es única en esta versión. Los errores frecuentes cuentan respuestas incorrectas registradas y las tarjetas pendientes son aquellas cuya fecha de próximo repaso ya venció.

Los errores siguen el formato `ProblemDetail`: `400` para validación, `404` para recursos inexistentes y `409` para duplicados o conflictos de integridad.

## Recuperación documental (RAG)

Al subir PDF, TXT o Markdown, el backend extrae el texto y conserva los chunks indexados en PostgreSQL. Los PDF se dividen por página; los archivos de texto usan referencia de página nula. Cada fragmento conserva documento, página e índice, junto con un vector disperso local de frecuencia de términos. La recuperación compara esos vectores con el tema consultado, prioriza coincidencias literales y limita el contexto enviado a Groq a cinco fragmentos y 8500 caracteres. No se envía el documento completo en cada consulta. Los documentos antiguos se indexan al primer uso si no tienen chunks; como el texto histórico no conserva paginación, sus referencias no incluyen página. Al desactivar un documento, se eliminan sus chunks del índice.

Esta primera versión usa recuperación lexical (vectorización dispersa de términos), no embeddings semánticos densos: no agrega modelo externo, extensión de PostgreSQL ni costo de inferencia. Favorece búsqueda de artículos, nombres, fechas y frases exactas, pero puede no recuperar bien paráfrasis sin palabras compartidas. El registro limita cada instalación a una sola cuenta; los documentos no tienen propietario individual. Si se permite más de una cuenta en una misma instalación, se deberá añadir propiedad y filtrar el índice por usuario antes de habilitar ese escenario.

Las respuestas de IA agregan `sources` con ID/nombre de documento, página cuando existe e índice de fragmento. Explicaciones reportan cuando no hay evidencia suficiente; la generación de tarjetas o preguntas sin fragmentos relevantes responde `422`. El frontend en `front/` permite consultar explicaciones, revisar las fuentes asociadas y abrir el texto extraído del documento.

## Frontend

La interfaz React + TypeScript de consulta documental está en `front/`. Para ejecutarla localmente, inicia primero el backend y después:

```powershell
cd front
npm install
npm run dev
```

En desarrollo, Vite reenvía `/api` a `http://localhost:8080`. Usa `VITE_API_PROXY_TARGET` para cambiar el destino del proxy. Ejecuta las pruebas con `npm test` y el build con `npm run build`. La aplicación usa las credenciales de la cuenta del backend en memoria y solicita CSRF antes de enviar una consulta. Consulta `front/README.md` para los contratos integrados y detalles de configuración.

## Tutor de IA

`AiTutorService` valida las salidas estructuradas y conserva la generación común de explicaciones, tarjetas y preguntas. El proveedor principal predeterminado es Groq (`AI_PRIMARY_PROVIDER=groq`), por lo que Gemini no recibe solicitudes en la configuración normal. Puedes elegir `AI_PRIMARY_PROVIDER=gemini` para volver a usar Gemini; en ese modo, `AI_FALLBACK_ENABLED=true` permite fallback a Groq ante errores recuperables. Si Groq es el principal, sus errores no redirigen la solicitud a Gemini. Groq usa Chat Completions en `GROQ_BASE_URL` (por defecto `https://api.groq.com/openai/v1`) y requiere `GROQ_API_KEY` y `GROQ_MODEL`. Obtén una API key independiente en [Groq Console](https://console.groq.com/keys). `GROQ_MODEL` queda vacío por defecto para no escoger un modelo ni asumir su nivel de acceso; configúralo en IntelliJ (**Run/Debug Configurations → Environment variables**) con un ID vigente que tu consola indique disponible en el nivel gratuito y sin facturación. No quedó fijado un modelo por defecto porque no se verificó un ID gratuito en la cuenta. Si falta la clave o el modelo, el servicio responde con un error seguro de configuración. Groq usa por defecto timeout de conexión de 3 s, respuesta de 30 s y hasta 3000 tokens de salida (`GROQ_CONNECT_TIMEOUT`, `GROQ_RESPONSE_TIMEOUT`, `GROQ_MAX_COMPLETION_TOKENS` configurables). Gemini conserva sus timeouts configurables de conexión (3 s) y respuesta (6 s). No se exponen mensajes internos ni credenciales.

Ejemplos (requieren usuario autenticado, cookie de sesión y token CSRF):

```http
POST /api/v1/ai/explanations
Content-Type: application/json

{
  "topicName": "Acción de tutela",
  "sourceContent": "Texto de estudio opcional",
  "learnerLevel": "inicial"
}
```

```http
POST /api/v1/ai/questions
Content-Type: application/json

{
  "topicId": "00000000-0000-0000-0000-000000000001",
  "topicName": "Acción de tutela",
  "sourceDocumentId": "00000000-0000-0000-0000-000000000002",
  "difficulty": "INTERMEDIATE",
  "quantity": 5
}
```

Las solicitudes de explicación, tarjetas y preguntas pueden usar `sourceContent` o `sourceDocumentId`, pero no ambos. Si no se especifica ninguno, se buscan fragmentos relevantes entre los documentos activos; al proporcionar `sourceDocumentId`, la búsqueda queda restringida a ese documento. `sourceContent` conserva el flujo compatible de material en línea. Las respuestas incluyen `sources` con los fragmentos recuperados que se enviaron al modelo (no representan una verificación automática de cada afirmación individual). El backend no tiene navegación web; si se solicita información externa, no puede verificarla y debe indicarlo en lugar de presentarla como respaldada por los documentos. Configura las claves mediante variables de entorno/secret manager, no en el repositorio.

La generación devuelve contenido para revisión y no lo guarda automáticamente. Después de revisar el resultado, usa `POST /api/v1/ai/flashcards/save` para persistir tarjetas (`topicId`, `flashcards` y opcionalmente `sourceDocumentId`), o `POST /api/v1/ai/questions/save` para crear un cuestionario (`topicId`, `name`, `difficulty`, `questions` y opcionalmente `sourceDocumentId`). Los endpoints de guardado reciben las listas de objetos devueltas por los endpoints de generación; aceptan hasta 20 elementos por solicitud y conservan la asociación al tema y documento fuente.

## Seguridad y siguientes etapas

La API requiere HTTP Basic para todos los recursos excepto la obtención del token CSRF y el registro inicial. Para este último, primero solicita `GET /api/v1/auth/csrf`, conserva tanto la cookie `XSRF-TOKEN` como la cookie de sesión y envía el token en el encabezado devuelto (por defecto `X-XSRF-TOKEN`). Todas las operaciones que modifican datos requieren también ese token CSRF. Después del alta, usa el correo y la contraseña mediante HTTP Basic. El registro solo funciona cuando aún no existe ninguna cuenta; las contraseñas se guardan mediante BCrypt (factor de trabajo 12). La contraseña debe tener entre 12 y 72 caracteres y no exceder 72 bytes UTF-8. Protege Basic Auth siempre con HTTPS fuera del entorno local.

Ejemplo PowerShell para crear la primera cuenta (el cliente debe conservar las cookies de la sesión):

```powershell
$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/csrf" -SessionVariable session
$headers = @{}
$headers[$csrf.headerName] = $csrf.token
$body = @{
    name = "Estudiante"
    email = "estudiante@example.com"
    password = "usa-una-clave-local-segura"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/register" `
    -WebSession $session -Headers $headers -ContentType "application/json" -Body $body
```

Para el resto de solicitudes, añade credenciales HTTP Basic y, para métodos `POST`, `PUT`, `PATCH` o `DELETE`, el token CSRF de la sesión activa. `GET /api/v1/auth/me` devuelve el perfil autenticado sin la contraseña ni su hash.

Como el producto es personal y todavía no existe separación por propietario en los datos de estudio, solo se permite una cuenta por instalación. Para permitir varias cuentas se deberá migrar módulos de datos privados, tarjetas, documentos, cuestionarios, intentos y progreso con relaciones de propietario y aplicar filtros de autorización. Considera el tratamiento de datos al enviar material a un proveedor externo de IA.

Ejecuta las pruebas con `.\mvnw.cmd test`.

## Probar desde Postman

Importa `postman/Procuraduria-Prep.postman_collection.json` en Postman. En la colección, abre **Variables** y define `userPassword` con una contraseña local (12-72 caracteres; máximo 72 bytes UTF-8). Puedes cambiar también `studentName` y `userEmail`. No guardes ni exportes la colección con una contraseña real; no necesita la clave de Gemini.

Inicia el backend desde IntelliJ con `AI_PRIMARY_PROVIDER=groq`, `GROQ_API_KEY` y `GROQ_MODEL` en **Run/Debug Configurations → Environment variables**. Establece `GROQ_MODEL` con un ID vigente que tu consola marque incluido en el nivel gratuito y sin facturación. Para regresar a Gemini, cambia `AI_PRIMARY_PROVIDER=gemini` y proporciona `GEMINI_API_KEY`. No pongas claves en código ni las exportes en la colección Postman.

1. En **01 - Registro y temas**, ejecuta primero **Obtener token CSRF** y después **Registrar primera cuenta**. El registro solo se hace una vez por base de datos. Postman conservará la cookie CSRF.
2. Ejecuta las demás solicitudes del primer grupo para crear el módulo y dos temas de prueba.
3. En **02 - Tutor IA**, ejecuta las solicitudes en orden para probar explicación, generación de tarjetas/preguntas y guardado. Cada generación contacta el proveedor de `AI_PRIMARY_PROVIDER`; Groq es el predeterminado y no redirige sus fallos a Gemini. Si se selecciona Gemini, `AI_FALLBACK_ENABLED=true` permite fallback recuperable a Groq.
4. En **03 - Simulacro y resultado**, ejecuta las solicitudes en orden para crear el simulacro, iniciar el intento, responder una pregunta y consultar el resultado.

Si ya existe una cuenta, omite **Registrar primera cuenta**. Si Postman muestra `403`, vuelve a ejecutar **Obtener token CSRF** y repite la solicitud fallida. Si una solicitud de IA falla con `503`, verifica la API key y el modelo del proveedor seleccionado; reinicia la aplicación tras cambiar el entorno.
