package org.ruscayn.vocarunner.common.adapter.in.web;

import java.time.LocalDateTime;
import org.ruscayn.vocarunner.common.ErrorCode;

public record ErrorResponse(String code, String message, String path, LocalDateTime timestamp) {

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode.getCode(), errorCode.getMessage(), path);
    }

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path, LocalDateTime.now());
    }

}
