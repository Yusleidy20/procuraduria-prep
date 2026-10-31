package com.yusdev.procuraduriaprep.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
