package com.yusdev.procuraduriaprep.ai.service;

import java.util.List;

public class AiOutputValidationException extends RuntimeException {

	private final List<String> violations;

	public AiOutputValidationException(List<String> violations) {
		super("La salida generada por el proveedor de IA no cumple el contrato esperado");
		this.violations = List.copyOf(violations);
	}

	public List<String> getViolations() {
		return violations;
	}
}
