package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentResult;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentIngestionStore;
import com.aalsaeed.fleetops.integration.application.port.out.ErpShipmentPayloadSerializer;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErpShipmentIngestionServiceTest {

    @Test
    void serializesCanonicalMessageBeforeStagingIt() {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "erp-demo",
                "MSG-1001",
                "SHIP-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T10:15:30Z"),
                "CORR-1001");

        AtomicReference<ErpShipmentMessage> storedMessage = new AtomicReference<>();
        AtomicReference<String> storedPayload = new AtomicReference<>();

        ErpShipmentPayloadSerializer serializer = ignored -> "{\"shipmentReference\":\"SHIP-1001\"}";
        ErpShipmentIngestionStore store = (candidate, payload) -> {
            storedMessage.set(candidate);
            storedPayload.set(payload);
            return new StageErpShipmentResult(
                    candidate.id(),
                    candidate.idempotencyKey(),
                    false,
                    Instant.parse("2026-08-17T10:16:00Z"));
        };

        ErpShipmentIngestionService service = new ErpShipmentIngestionService(serializer, store);
        StageErpShipmentResult result = service.stage(message);

        assertSame(message, storedMessage.get());
        assertEquals("{\"shipmentReference\":\"SHIP-1001\"}", storedPayload.get());
        assertEquals(message.id(), result.messageId());
        assertEquals(message.idempotencyKey(), result.idempotencyKey());
        assertFalse(result.duplicate());
    }

    @Test
    void rejectsBlankSerializedPayload() {
        ErpShipmentMessage message = ErpShipmentMessage.create(
                "ERP-DEMO",
                "MSG-1002",
                "SHIP-1002",
                ErpShipmentOperation.CANCEL,
                Instant.parse("2026-08-17T10:15:30Z"),
                null);

        ErpShipmentIngestionService service = new ErpShipmentIngestionService(
                ignored -> "   ",
                (candidate, payload) -> {
                    throw new AssertionError("Store must not be called for a blank payload");
                });

        assertThrows(IllegalStateException.class, () -> service.stage(message));
    }
}
