package com.aalsaeed.fleetops.vehicle.api.rest;

import com.aalsaeed.fleetops.common.concurrency.OptimisticConcurrencyConflictException;
import com.aalsaeed.fleetops.vehicle.application.exception.VehicleAlreadyExistsException;
import com.aalsaeed.fleetops.vehicle.application.exception.VehicleNotFoundException;
import com.aalsaeed.fleetops.vehicle.domain.InvalidVehicleStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = VehicleController.class)
public class VehicleRestExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    ProblemDetail handleNotFound(VehicleNotFoundException exception) {
        return createProblem(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(VehicleAlreadyExistsException.class)
    ProblemDetail handleAlreadyExists(VehicleAlreadyExistsException exception) {
        return createProblem(HttpStatus.CONFLICT, "VEHICLE_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(InvalidVehicleStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidVehicleStatusTransitionException exception) {
        return createProblem(HttpStatus.CONFLICT, "INVALID_VEHICLE_STATUS_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(OptimisticConcurrencyConflictException.class)
    ProblemDetail handleConcurrencyConflict(OptimisticConcurrencyConflictException exception) {
        ProblemDetail detail = createProblem(
                HttpStatus.CONFLICT,
                "OPTIMISTIC_CONCURRENCY_CONFLICT",
                exception.getMessage());
        detail.setProperty("resourceType", exception.resourceType());
        detail.setProperty("resourceId", exception.resourceId());
        return detail;
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
