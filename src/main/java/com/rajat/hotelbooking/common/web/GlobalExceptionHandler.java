package com.rajat.hotelbooking.common.web;

import com.rajat.hotelbooking.common.exception.ConflictException;
import com.rajat.hotelbooking.common.exception.DomainValidationException;
import com.rajat.hotelbooking.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_CODE = "errorCode";
    private static final String ERRORS = "errors";

    public record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ErrorCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ProblemDetail handleConcurrentUpdate(RuntimeException ex) {
        log.info("Concurrent update rejected: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_UPDATE,
                "The resource was changed by another request. Reload and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.info("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandler::toViolation)
                .toList();
        ProblemDetail body = validationProblem(violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldViolation> violations = ex.getParameterValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors errors
                        ? errors.getAllErrors().stream().map(GlobalExceptionHandler::toViolation)
                        : result.getResolvableErrors().stream().map(error -> new FieldViolation(
                                parameterName(result.getMethodParameter()), error.getDefaultMessage())))
                .toList();
        ProblemDetail body = validationProblem(violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail
                && (problemDetail.getProperties() == null || !problemDetail.getProperties().containsKey(ERROR_CODE))) {
            problemDetail.setProperty(ERROR_CODE, ErrorCode.fromStatus(statusCode));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail validationProblem(List<FieldViolation> violations) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request validation failed.");
        body.setProperty(ERRORS, violations);
        return body;
    }

    private static ProblemDetail problem(HttpStatus status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(ERROR_CODE, code);
        return problem;
    }

    private static FieldViolation toViolation(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            String message = fieldError.isBindingFailure()
                    ? "has an invalid value: " + fieldError.getRejectedValue()
                    : fieldError.getDefaultMessage();
            return new FieldViolation(fieldError.getField(), message);
        }
        return new FieldViolation(error.getObjectName(), error.getDefaultMessage());
    }

    private static String parameterName(MethodParameter parameter) {
        String name = parameter.getParameterName();
        return name != null ? name : "arg" + parameter.getParameterIndex();
    }
}
