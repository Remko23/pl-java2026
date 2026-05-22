package com.truthlens.search.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExternalSearchException extends RuntimeException {

    public ExternalSearchException(String message) {
        super(message);
    }

    public ExternalSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
