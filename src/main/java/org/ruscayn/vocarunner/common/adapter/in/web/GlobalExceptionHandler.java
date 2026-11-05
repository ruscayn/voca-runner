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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public Object handleCustomException(CustomException e, HttpServletRequest request) {
        log.warn("Business exception: {} {} {}", request.getRequestURI(), e.getErrorCode().getCode(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage(),
                request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, e.getMessage());
    }

    /**
     * 반환형이 Object 여도 반환 처리기가 실제 객체 타입(ResponseEntity · ModelAndView)으로 골라
     * API 는 JSON, 화면은 error.html 로 응답한다
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

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Object handleValidationException(Exception e, HttpServletRequest request) {
        log.warn("Validation failed: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public Object handleBadRequestParameter(Exception e, HttpServletRequest request) {
        log.warn("Bad request parameter: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return respond(request, HttpStatus.BAD_REQUEST, errorResponse, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Media type not supported: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request.getRequestURI());
        return respond(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, errorResponse, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported: {} {}", request.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI());
        return respond(request, HttpStatus.METHOD_NOT_ALLOWED, errorResponse, null);
    }

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

}
