package com.aalsaeed.fleetops.driver.api.rest;

import com.aalsaeed.fleetops.driver.application.exception.DriverAlreadyExistsException;
import com.aalsaeed.fleetops.driver.application.exception.DriverNotFoundException;
import com.aalsaeed.fleetops.driver.domain.InvalidDriverStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class DriverRestExceptionHandler {

    @ExceptionHandler(DriverNotFoundException.class)
    ProblemDetail handleNotFound(DriverNotFoundException exception) {
        return createProblem(HttpStatus.NOT_FOUND, "DRIVER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DriverAlreadyExistsException.class)
    ProblemDetail handleAlreadyExists(DriverAlreadyExistsException exception) {
        return createProblem(HttpStatus.CONFLICT, "DRIVER_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(InvalidDriverStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidDriverStatusTransitionException exception) {
        return createProblem(HttpStatus.CONFLICT, "INVALID_DRIVER_STATUS_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = createProblem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed");

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return createProblem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    private static ProblemDetail createProblem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
