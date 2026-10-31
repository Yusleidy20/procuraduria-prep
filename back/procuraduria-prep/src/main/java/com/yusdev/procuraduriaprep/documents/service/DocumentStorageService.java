package com.yusdev.procuraduriaprep.documents.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.yusdev.procuraduriaprep.shared.exception.InvalidRequestException;

@Component
public class DocumentStorageService {

	private final Path storageDirectory;

	public DocumentStorageService(@Value("${app.documents.storage-dir:./data/documents}") String storageDirectory) {
		this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
	}

	public String store(UUID documentId, String extension, byte[] content) {
		Path destination = storageDirectory.resolve(documentId + extension).normalize();
		if (!destination.getParent().equals(storageDirectory)) {
			throw new InvalidRequestException("Referencia de almacenamiento inválida");
		}
		try {
			Files.createDirectories(storageDirectory);
			Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			return "local://documents/%s%s".formatted(documentId, extension);
		} catch (IOException exception) {
			throw new IllegalStateException("No fue posible almacenar el documento", exception);
		}
	}

	public void delete(String storageReference) {
		String prefix = "local://documents/";
		if (!storageReference.startsWith(prefix)) {
			throw new IllegalStateException("La referencia del documento no pertenece al almacenamiento local");
		}
		String fileName = storageReference.substring(prefix.length());
		if (!fileName.matches("[0-9a-fA-F-]{36}\\.(pdf|txt|md)")) {
			throw new IllegalStateException("La referencia del documento no es válida");
		}
		Path file = storageDirectory.resolve(fileName).normalize();
		if (!file.getParent().equals(storageDirectory)) {
			throw new IllegalStateException("La referencia del documento está fuera del almacenamiento configurado");
		}
		try {
			Files.deleteIfExists(file);
		} catch (IOException exception) {
			throw new IllegalStateException("No fue posible eliminar el archivo temporal", exception);
		}
	}
}
