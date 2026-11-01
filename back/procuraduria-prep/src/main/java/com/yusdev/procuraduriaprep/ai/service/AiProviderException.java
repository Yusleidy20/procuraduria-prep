package com.yusdev.procuraduriaprep.ai.service;

public class AiProviderException extends RuntimeException {
    private final String provider;
    private final boolean retryable;
    private final Integer httpStatus;
    private final boolean timeout;

    public AiProviderException(String message) {
        this(message, null, false, null, false);
    }

    public AiProviderException(String message, String provider, boolean retryable, Integer httpStatus) {
        this(message, provider, retryable, httpStatus, false);
    }

    public AiProviderException(String message, String provider, boolean retryable, Integer httpStatus, boolean timeout) {
        super(message);
        this.provider = provider;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
        this.timeout = timeout;
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

    public boolean isTimeout() {
        return timeout;
    }
}
