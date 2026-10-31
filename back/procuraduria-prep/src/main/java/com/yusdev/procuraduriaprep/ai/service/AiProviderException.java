package com.yusdev.procuraduriaprep.ai.service;

public class AiProviderException extends RuntimeException {
    private final String provider;
    private final boolean retryable;
    private final Integer httpStatus;

    public AiProviderException(String message) {
        this(message, null, false, null);
    }

    public AiProviderException(String message, String provider, boolean retryable, Integer httpStatus) {
        super(message);
        this.provider = provider;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
