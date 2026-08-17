package com.aalsaeed.fleetops.integration.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErpShipmentMessageTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-16T18:00:00Z");

    @Test
    void createsCanonicalMessageAndNormalizesBoundaryValues() {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "  erp-demo  ",
                " MSG-1001 ",
                " SHP-9001 ",
                ErpShipmentOperation.UPSERT,
                OCCURRED_AT,
                " CORR-42 ");

        assertEquals(1, message.schemaVersion());
        assertEquals("ERP-DEMO", message.sourceSystem());
        assertEquals("MSG-1001", message.sourceMessageId());
        assertEquals("SHP-9001", message.shipmentReference());
        assertEquals(ErpShipmentOperation.UPSERT, message.operation());
        assertEquals(OCCURRED_AT, message.occurredAt());
        assertEquals("CORR-42", message.correlationId());
        assertEquals("ERP-DEMO:MSG-1001", message.idempotencyKey().value());
    }

    @Test
    void duplicateSourceDeliveryProducesSameIdempotencyKey() {
        ErpShipmentMessage first = ErpShipmentMessage.create(
                "erp-demo", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null);
        ErpShipmentMessage duplicate = ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT.plusSeconds(5), null);

        assertNotEquals(first.id(), duplicate.id());
        assertEquals(first.idempotencyKey(), duplicate.idempotencyKey());
    }

    @Test
    void differentSourceMessagesProduceDifferentIdempotencyKeys() {
        ErpShipmentMessage first = ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null);
        ErpShipmentMessage second = ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1002", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null);

        assertNotEquals(first.idempotencyKey(), second.idempotencyKey());
    }

    @Test
    void blankRequiredBoundaryValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ErpShipmentMessage.create(
                " ", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class, () -> ErpShipmentMessage.create(
                "ERP-DEMO", " ", "SHP-9001",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class, () -> ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", " ",
                ErpShipmentOperation.UPSERT, OCCURRED_AT, null));
    }

    @Test
    void operationAndOccurredAtAreRequired() {
        assertThrows(NullPointerException.class, () -> ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", "SHP-9001",
                null, OCCURRED_AT, null));
        assertThrows(NullPointerException.class, () -> ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.UPSERT, null, null));
    }

    @Test
    void blankCorrelationIdIsNormalizedToNull() {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "ERP-DEMO", "MSG-1001", "SHP-9001",
                ErpShipmentOperation.CANCEL, OCCURRED_AT, "   ");

        assertNull(message.correlationId());
    }

    @Test
    void restorePreservesMessageIdentityAndSchemaVersion() {
        IntegrationMessageId id = IntegrationMessageId.of(UUID.randomUUID());

        ErpShipmentMessage restored = ErpShipmentMessage.restore(
                id,
                2,
                "ERP-DEMO",
                "MSG-2001",
                "SHP-9100",
                ErpShipmentOperation.UPSERT,
                OCCURRED_AT,
                null);

        assertEquals(id, restored.id());
        assertEquals(2, restored.schemaVersion());
        assertEquals("ERP-DEMO:MSG-2001", restored.idempotencyKey().value());
    }

    @Test
    void invalidSchemaVersionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ErpShipmentMessage.restore(
                IntegrationMessageId.newId(),
                0,
                "ERP-DEMO",
                "MSG-1001",
                "SHP-9001",
                ErpShipmentOperation.UPSERT,
                OCCURRED_AT,
                null));
    }
}
