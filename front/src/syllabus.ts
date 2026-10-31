export type SyllabusArea = "Conocimientos comunes" | "Conocimientos específicos";

export interface SyllabusTopic {
  id: string;
  title: string;
  area: SyllabusArea;
}

export const SYLLABUS: SyllabusTopic[] = [
  { id: "gestion-publica", title: "Gestión pública y funcionamiento del Estado", area: "Conocimientos comunes" },
  { id: "procuraduria", title: "Estructura, organización y funciones de la Procuraduría General de la Nación", area: "Conocimientos comunes" },
  { id: "archivo-correspondencia", title: "Procedimientos y reglamentación de archivo y correspondencia", area: "Conocimientos comunes" },
  { id: "ofimatica", title: "Herramientas ofimáticas", area: "Conocimientos comunes" },
  { id: "documentos-oficina", title: "Elaboración de documentos de oficina", area: "Conocimientos comunes" },
  { id: "atencion-usuario", title: "Atención al usuario", area: "Conocimientos comunes" },
  { id: "sistemas-gestion", title: "Sistemas de gestión", area: "Conocimientos comunes" },
  { id: "gestion-documental", title: "Gestión documental", area: "Conocimientos comunes" },
  { id: "constitucion", title: "Constitución Política", area: "Conocimientos específicos" },
  { id: "codigo-disciplinario", title: "Código General Disciplinario", area: "Conocimientos específicos" },
  { id: "derechos-humanos-dih", title: "Derechos Humanos y Derecho Internacional Humanitario", area: "Conocimientos específicos" },
  { id: "investigacion-criminal", title: "Procedimientos en investigación criminal", area: "Conocimientos específicos" },
  { id: "policia-judicial", title: "Marco normativo de Policía Judicial", area: "Conocimientos específicos" },
  { id: "area-disciplinar", title: "Conocimientos técnicos del área disciplinar", area: "Conocimientos específicos" },
];

export const PLAN_DAYS = [
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
  "Domingo",
];

export const ESTIMATED_SESSION_MINUTES = 25;

export function getPlanDay(topicIndex: number): string {
  return PLAN_DAYS[Math.floor(topicIndex / 2) % PLAN_DAYS.length];
}

export function getStudyStatus(
  topicId: string,
  completed: Record<string, string>,
  inProgressId: string | null,
): "Pendiente" | "En curso" | "Completado" {
  if (completed[topicId]) return "Completado";
  if (inProgressId === topicId) return "En curso";
  return "Pendiente";
}
