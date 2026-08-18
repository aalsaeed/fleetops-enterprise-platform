package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class AuditHttpCaptureInterceptor implements HandlerInterceptor {

    private final AuditRouteRegistry routeRegistry;
    private final HttpAuditRecorder auditRecorder;

    AuditHttpCaptureInterceptor(AuditRouteRegistry routeRegistry, HttpAuditRecorder auditRecorder) {
        this.routeRegistry = routeRegistry;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {

        String routePattern = routePattern(request);
        routeRegistry.resolve(request.getMethod(), routePattern).ifPresent(route -> {
            AuditOutcome outcome = isSuccessful(response.getStatus())
                    ? AuditOutcome.SUCCESS
                    : AuditOutcome.FAILURE;

            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            metadata.put("httpMethod", request.getMethod());
            metadata.put("route", routePattern);
            metadata.put("status", Integer.toString(response.getStatus()));
            if (exception != null) {
                metadata.put("exceptionType", exception.getClass().getSimpleName());
            }

            auditRecorder.record(
                    route,
                    resolveResourceId(route, request, response),
                    outcome,
                    correlationId(request),
                    Map.copyOf(metadata));
        });
    }

    private static String routePattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getRequestURI() : pattern.toString();
    }

    private static String resolveResourceId(
            AuditRouteRegistry.AuditRoute route,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (route.resourceFromLocation()) {
            return resourceIdFromLocation(response.getHeader(HttpHeaders.LOCATION));
        }

        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> variableMap && route.resourcePathVariable() != null) {
            Object value = variableMap.get(route.resourcePathVariable());
            return value == null ? null : value.toString();
        }
        return null;
    }

    private static String resourceIdFromLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            String path = URI.create(location).getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] segments = path.split("/");
            for (int index = segments.length - 1; index >= 0; index--) {
                if (!segments[index].isBlank()) {
                    return segments[index];
                }
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private static String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(AuditCorrelationFilter.ATTRIBUTE_NAME);
        if (value != null && !value.toString().isBlank()) {
            return value.toString();
        }
        return UUID.randomUUID().toString();
    }

    private static boolean isSuccessful(int status) {
        return status >= 200 && status < 300;
    }
}
