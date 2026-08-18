package com.aalsaeed.fleetops.audit.web;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchPage;

import java.util.List;

public record AuditSearchResponse(
        List<AuditEventResponse> items,
        long total,
        int offset,
        int limit,
        boolean hasMore) {

    static AuditSearchResponse from(AuditSearchPage page) {
        return new AuditSearchResponse(
                page.items().stream().map(AuditEventResponse::from).toList(),
                page.total(),
                page.offset(),
                page.limit(),
                page.hasMore());
    }
}
