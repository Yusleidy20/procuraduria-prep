package com.yusdev.procuraduriaprep.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.yusdev.procuraduriaprep.ai.service.AiProviderException;

class GlobalExceptionHandlerTest {

    @Test
    void keepsProviderFailureAsSafeBadGatewayResponse() {
        String publicMessage = "No fue posible obtener una respuesta válida del proveedor de IA";
        var response = new GlobalExceptionHandler().handleAiProvider(new AiProviderException(publicMessage));

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
        assertEquals(publicMessage, response.getDetail());
    }

    @Test
    void mapsProviderTimeoutToGatewayTimeout() {
        var response = new GlobalExceptionHandler().handleAiProvider(
                new AiProviderException("provider internal detail", "Groq", true, null, true));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), response.getStatus());
        assertTrue(response.getDetail().contains("tardó demasiado"));
        org.junit.jupiter.api.Assertions.assertFalse(response.getDetail().contains("provider internal detail"));
    }

    @Test
    void returnsSafeInternalErrorDetailForUnexpectedFailures() {
        var response = new GlobalExceptionHandler().handleUnexpected(
                new IllegalStateException("internal host and token details"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertEquals("Ocurrió un problema interno al procesar la solicitud",
                response.getBody().getDetail());
        org.junit.jupiter.api.Assertions.assertFalse(
                response.getBody().getDetail().contains("token"));
    }
}
