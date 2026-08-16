package com.aalsaeed.fleetops.trip.api.rest;

import com.aalsaeed.fleetops.trip.application.exception.InvalidTripResourceRoleException;
import com.aalsaeed.fleetops.trip.application.exception.TripAlreadyExistsException;
import com.aalsaeed.fleetops.trip.application.exception.TripNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceNotFoundException;
import com.aalsaeed.fleetops.trip.application.exception.TripResourceUnavailableException;
import com.aalsaeed.fleetops.trip.domain.InvalidTripAssignmentException;
import com.aalsaeed.fleetops.trip.domain.InvalidTripStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = TripController.class)
public class TripRestExceptionHandler {

    @ExceptionHandler(TripNotFoundException.class)
    ProblemDetail handleTripNotFound(TripNotFoundException exception) {
        return createProblem(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(TripAlreadyExistsException.class)
    ProblemDetail handleTripAlreadyExists(TripAlreadyExistsException exception) {
        return createProblem(HttpStatus.CONFLICT, "TRIP_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(TripResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(TripResourceNotFoundException exception) {
        return createProblem(HttpStatus.NOT_FOUND, "TRIP_RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(TripResourceUnavailableException.class)
    ProblemDetail handleResourceUnavailable(TripResourceUnavailableException exception) {
        return createProblem(HttpStatus.CONFLICT, "TRIP_RESOURCE_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(InvalidTripResourceRoleException.class)
    ProblemDetail handleInvalidResourceRole(InvalidTripResourceRoleException exception) {
        return createProblem(HttpStatus.CONFLICT, "INVALID_TRIP_RESOURCE_ROLE", exception.getMessage());
    }

    @ExceptionHandler(InvalidTripAssignmentException.class)
    ProblemDetail handleInvalidAssignment(InvalidTripAssignmentException exception) {
        return createProblem(HttpStatus.CONFLICT, "INVALID_TRIP_ASSIGNMENT", exception.getMessage());
    }

    @ExceptionHandler(InvalidTripStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidTripStatusTransitionException exception) {
        return createProblem(HttpStatus.CONFLICT, "INVALID_TRIP_STATUS_TRANSITION", exception.getMessage());
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
