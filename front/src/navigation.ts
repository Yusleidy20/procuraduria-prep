export type SectionId = "dashboard" | "plan" | "syllabus" | "materials" | "ai" | "simulators" | "reviews" | "progress";

export const NAV_ITEMS: { id: SectionId; label: string; icon: string }[] = [
  { id: "dashboard", label: "Dashboard", icon: "grid" },
  { id: "plan", label: "Mi plan de estudio", icon: "calendar" },
  { id: "syllabus", label: "Temario", icon: "book" },
  { id: "materials", label: "Mis documentos", icon: "spark" },
  { id: "ai", label: "Estudiar con IA", icon: "spark" },
  { id: "simulators", label: "Simulacros", icon: "check" },
  { id: "reviews", label: "Repasos", icon: "refresh" },
  { id: "progress", label: "Mi progreso", icon: "chart" },
];
