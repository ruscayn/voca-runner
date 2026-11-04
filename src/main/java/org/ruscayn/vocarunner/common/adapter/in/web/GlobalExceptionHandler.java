package org.ruscayn.vocarunner.common.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 공통 오류 응답.
 * <p>API(/api/**)는 JSON, 화면 요청은 한국어 안내 화면(error.html)으로 응답한다.
 * 화면에서 주소를 잘못 입력해도 JSON 글자나 500 이 보이지 않게 한다 (B4·U3)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 업무 오류 (없는·끝난 시험, 마감 초과, 다른 기기의 요청 등). 요청을 거절한 것이라 스택 없이 WARN 한 줄로 남긴다.
     * 오류 코드와 문구로 원인을 찾을 수 있다
     */
    @ExceptionHandler(CustomException.class)
    public Object handleCustomException(CustomException e, HttpServletRequest request) {
        log.warn("Business exception: {} {} {}", request.getRequestURI(), e.getErrorCode().getCode(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage(), request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, e.getMessage());
    }

    /**
     * 입력값 검사 실패 (보기 번호 0·5, 고른 보기 목록(answers) null 등 — Command 가 스스로 검증한 것 포함).
     * 사용자 입력 문제라 스택 없이 남긴다
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Object handleValidationException(Exception e, HttpServletRequest request) {
        log.warn("Validation failed: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, null);
    }

    /**
     * 주소·파라미터·본문 형식 오류 (UUID 아닌 시험 id, 없는 레벨, 레벨 누락, GET /test/start,
     * JSON 이 아니거나 칸 타입이 틀린 제출 본문 등). 사용자 입력 문제라 스택 없이 남긴다 (U44)
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public Object handleBadRequestParameter(Exception e, HttpServletRequest request) {
        log.warn("Bad request parameter: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, null);
    }

    /** 본문 형식(Content-Type)을 받지 않는 요청 (예: 제출에 text/plain). 사용자 입력 문제라 스택 없이 남긴다 (U44) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Media type not supported: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request.getRequestURI());
        return respond(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, errorResponse, null);
    }

    /** 허용하지 않는 메서드 (GET 으로 제출 등). 사용자 입력 문제라 스택 없이 남긴다 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI());
        return respond(request, HttpStatus.METHOD_NOT_ALLOWED, errorResponse, null);
    }

    /** 없는 경로·정적 파일 (favicon.ico 등). 흔한 요청이라 스택 없이 DEBUG 로만 남긴다 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(
            NoResourceFoundException e, HttpServletRequest request) {
        log.debug("Resource not found: {}", e.getResourcePath());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, request.getRequestURI());
        return respond(request, HttpStatus.NOT_FOUND, errorResponse, null);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception occurred: {}", e.getMessage(), e);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return respond(request, HttpStatus.INTERNAL_SERVER_ERROR, errorResponse, null);
    }

    /**
     * API 는 JSON(ResponseEntity), 화면은 error.html(ModelAndView).
     * 반환 처리기는 실제 반환 객체의 타입으로 고르므로 반환형이 Object 여도 각각 처리된다
     *
     * @param detail 화면에 보여줄 안내 문구. null 이면 상태 코드별 기본 문구
     */
    private Object respond(HttpServletRequest request, HttpStatus status, ErrorResponse errorResponse, String detail) {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(status).body(errorResponse);
        }
        ModelAndView mav = new ModelAndView("error", status);
        mav.addObject("status", status.value());
        mav.addObject("detail", detail);
        return mav;
    }

}
