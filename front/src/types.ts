export interface DocumentSourceReference {
  documentId: string;
  documentName: string;
  pageNumber: number | null;
  chunkIndex: number;
}

export interface TopicExplanationResponse {
  explanation: string;
  keyPoints: string | null;
  sources: DocumentSourceReference[];
}

export interface DocumentResponse {
  id: string;
  originalName: string;
  contentType: string;
  storageReference: string;
  status: "PROCESSING" | "COMPLETED" | "FAILED" | string;
  processingError: string | null;
  uploadedAt: string;
}

export interface DocumentPage {
  content: DocumentResponse[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface DocumentTextResponse {
  documentId: string;
  originalName: string;
  text: string;
}

export interface AuthenticatedUser {
  id: string;
  name: string;
  email: string;
}

export interface OverallProgress {
  topicCount: number;
  completedQuizAttempts: number;
  quizAnswers: number;
  correctQuizAnswers: number;
  flashcardCount: number;
  flashcardReviews: number;
  rememberedReviews: number;
  masteryPercentage: number | null;
}

export interface Flashcard {
  id: string;
  question: string;
  answer: string;
  explanation: string | null;
  topicId: string;
  sourceDocumentId: string | null;
  difficulty: string;
  reviewCount: number;
  correctReviewCount: number;
  nextReviewAt: string | null;
  createdAt: string;
}

export interface StudyModule {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  status: string;
  createdAt: string;
}

export interface StudyTopic {
  id: string;
  name: string;
  description: string | null;
  moduleId: string;
  moduleName: string;
  difficulty: string;
  createdAt: string;
}

export interface TopicProgress {
  topicId: string;
  topicName: string;
  completedQuizAttempts: number;
  quizAnswers: number;
  correctQuizAnswers: number;
  flashcardCount: number;
  flashcardReviews: number;
  rememberedReviews: number;
  masteryPercentage: number | null;
}

export interface Quiz {
  id: string;
  name: string;
  description: string | null;
  topicId: string | null;
  topicIds: string[];
  mockExam: boolean;
  difficulty: string;
  durationMinutes: number | null;
  questionCount: number;
  createdAt: string;
}

export interface QuizQuestion {
  id: string;
  quizId: string;
  statement: string;
  options: string[];
  explanationReference: string | null;
}

export interface QuizAnswerResult {
  questionId: string;
  selectedOptionIndex: number;
  correctOptionIndex: number;
  correct: boolean;
  explanation: string | null;
}

export interface QuizAttempt {
  id: string;
  quizId: string;
  startedAt: string;
  deadlineAt: string | null;
  finishedAt: string | null;
  totalQuestions: number;
  correctAnswers: number | null;
  incorrectAnswers: number | null;
  scorePercentage: number | null;
  answers: QuizAnswerResult[];
}
