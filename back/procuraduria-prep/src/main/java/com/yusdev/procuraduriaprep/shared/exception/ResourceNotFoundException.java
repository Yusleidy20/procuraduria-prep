package com.yusdev.procuraduriaprep.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, Object id) {
		super("%s con identificador %s no existe".formatted(resource, id));
	}
}
