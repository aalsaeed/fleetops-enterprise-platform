package com.aalsaeed.fleetops.audit.application;

import com.aalsaeed.fleetops.audit.application.port.in.AuditSearchQuery;
import com.aalsaeed.fleetops.audit.domain.AuditOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditSearchQueryTest {

    @Test
    void normalizesOptionalFiltersAndAcceptsBoundedPagination() {
        AuditSearchQuery query = new AuditSearchQuery(
                Instant.parse("2026-08-18T10:00:00Z"),
                Instant.parse("2026-08-18T11:00:00Z"),
                "  subject-1  ",
                "  TRIP_CREATE  ",
                "  TRIP  ",
                "  trip-1  ",
                AuditOutcome.SUCCESS,
                "  corr-1  ",
                10,
                25);

        assertThat(query.actorSubject()).isEqualTo("subject-1");
        assertThat(query.action()).isEqualTo("TRIP_CREATE");
        assertThat(query.resourceType()).isEqualTo("TRIP");
        assertThat(query.resourceId()).isEqualTo("trip-1");
        assertThat(query.correlationId()).isEqualTo("corr-1");
        assertThat(query.offset()).isEqualTo(10);
        assertThat(query.limit()).isEqualTo(25);
    }

    @Test
    void rejectsInvalidRangeAndPagination() {
        Instant earlier = Instant.parse("2026-08-18T10:00:00Z");
        Instant later = Instant.parse("2026-08-18T11:00:00Z");

        assertThatThrownBy(() -> query(later, earlier, 0, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");

        assertThatThrownBy(() -> query(null, null, -1, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset");

        assertThatThrownBy(() -> query(null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");

        assertThatThrownBy(() -> query(null, null, 0, AuditSearchQuery.MAX_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    private static AuditSearchQuery query(Instant from, Instant to, int offset, int limit) {
        return new AuditSearchQuery(
                from,
                to,
                null,
                null,
                null,
                null,
                null,
                null,
                offset,
                limit);
    }
}
