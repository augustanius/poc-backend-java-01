package com.poc.common.exception;

import com.poc.common.context.CorrelationConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.validation.FieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handle(NotFoundException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handle(BadRequestException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handle(ConflictException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handle(UnauthorizedException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handle(ForbiddenException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.FORBIDDEN, ex, request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handle(ExternalServiceException ex, HttpServletRequest request) {
        logError(ex, request);
        return buildProblemDetail(HttpStatus.BAD_GATEWAY, ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                           HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid"),
                        (existing, replacement) -> existing
                ));

        log.error("[{}] {} {} | validationErrors={} | {}",
                MDC.get(CorrelationConstants.MDC_KEY),
                request.getMethod(), request.getRequestURI(),
                fieldErrors, ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Bad Request");
        pd.setProperty("errors", fieldErrors);
        pd.setProperty("correlationId", MDC.get(CorrelationConstants.MDC_KEY));
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                             HttpServletRequest request) {
        String detail = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());

        log.error("[{}] {} {} | {}",
                MDC.get(CorrelationConstants.MDC_KEY),
                request.getMethod(), request.getRequestURI(), detail);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Bad Request");
        pd.setProperty("correlationId", MDC.get(CorrelationConstants.MDC_KEY));
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handle(Exception ex, HttpServletRequest request) {
        log.error("[{}] {} {} | {}",
                MDC.get(CorrelationConstants.MDC_KEY),
                request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error");
        pd.setTitle("Internal Server Error");
        pd.setProperty("correlationId", MDC.get(CorrelationConstants.MDC_KEY));
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    private void logError(DomainException ex, HttpServletRequest request) {
        log.error("[{}] {} {} | context={} | {}",
                MDC.get(CorrelationConstants.MDC_KEY),
                request.getMethod(), request.getRequestURI(),
                ex.getLoggingContext(), ex.getMessage(), ex);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, DomainException ex,
                                              HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("correlationId", MDC.get(CorrelationConstants.MDC_KEY));
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
