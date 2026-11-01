package com.yusdev.procuraduriaprep.shared.exception;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yusdev.procuraduriaprep.ai.service.AiOutputValidationException;
import com.yusdev.procuraduriaprep.ai.service.AiProviderException;
import com.yusdev.procuraduriaprep.ai.service.AiProviderUnavailableException;
import com.yusdev.procuraduriaprep.ai.service.AiInsufficientContextException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(DuplicateResourceException.class)
	ProblemDetail handleDuplicate(DuplicateResourceException exception) {
		return problem(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	ProblemDetail handleInvalidRequest(InvalidRequestException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(AiOutputValidationException.class)
	ProblemDetail handleAiOutputValidation(AiOutputValidationException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_GATEWAY, exception.getMessage());
		problem.setProperty("violations", exception.getViolations());
		return problem;
	}

	@ExceptionHandler(AiProviderException.class)
	ProblemDetail handleAiProvider(AiProviderException exception) {
		if (exception.isTimeout()) {
			return problem(HttpStatus.GATEWAY_TIMEOUT, "El proveedor de IA tardó demasiado en responder");
		}
		return problem(HttpStatus.BAD_GATEWAY, exception.getMessage());
	}

	@ExceptionHandler(AiProviderUnavailableException.class)
	ProblemDetail handleAiProviderUnavailable(AiProviderUnavailableException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(AiInsufficientContextException.class)
	ProblemDetail handleAiInsufficientContext(AiInsufficientContextException exception) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos");
		Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						error -> error.getField(),
						error -> error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage(),
						(first, second) -> first));
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(problem(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes"));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
		logger.error("Unhandled API request failure; exceptionType={}", exception.getClass().getName(), exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
						"Ocurrió un problema interno al procesar la solicitud"));
	}

	private ProblemDetail problem(HttpStatus status, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("about:blank"));
		return problem;
	}
}
