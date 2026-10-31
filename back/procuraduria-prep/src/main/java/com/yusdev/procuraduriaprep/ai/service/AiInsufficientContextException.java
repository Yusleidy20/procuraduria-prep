package com.yusdev.procuraduriaprep.ai.service;

public class AiInsufficientContextException extends RuntimeException {

    public AiInsufficientContextException() {
        super("No se encontró información suficiente en los documentos para generar contenido fundamentado");
    }
}
