package org.ruscayn.vocarunner.common.adapter.in.web;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.ruscayn.vocarunner.common.ErrorCode;

@Getter
@Builder
public class ErrorResponse {

    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
