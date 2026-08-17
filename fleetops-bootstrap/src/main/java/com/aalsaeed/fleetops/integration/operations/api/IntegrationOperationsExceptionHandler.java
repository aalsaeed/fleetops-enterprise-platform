package com.aalsaeed.fleetops.integration.operations.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IntegrationOperationsController.class)
final class IntegrationOperationsExceptionHandler {

    @ExceptionHandler(IntegrationRecoveryNotAvailableException.class)
    ProblemDetail handleRecoveryNotAvailable(IntegrationRecoveryNotAvailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Integration recovery is not available");
        problem.setProperty("code", "INTEGRATION_RECOVERY_NOT_AVAILABLE");
        return problem;
    }
}
