package com.rajat.hotelbooking.common.web;

import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    CONFLICT,
    CONCURRENT_UPDATE,
    INTERNAL_ERROR;

    static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> status.is4xxClientError() ? MALFORMED_REQUEST : INTERNAL_ERROR;
        };
    }
}
