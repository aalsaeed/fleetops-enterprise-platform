package com.aalsaeed.fleetops.audit.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class AuditWebConfiguration implements WebMvcConfigurer {

    private final AuditRouteRegistry routeRegistry;
    private final HttpAuditRecorder auditRecorder;

    AuditWebConfiguration(AuditRouteRegistry routeRegistry, HttpAuditRecorder auditRecorder) {
        this.routeRegistry = routeRegistry;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuditHttpCaptureInterceptor(routeRegistry, auditRecorder))
                .addPathPatterns("/api/v1/**");
    }
}
