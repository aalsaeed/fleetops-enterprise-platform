package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryCommand;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryResult;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationInboxStore;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentOperation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpShipmentInboxServiceTest {

    @Test
    void acceptsFirstDeliveryAndIdentifiesDuplicateDelivery() {
        InMemoryInboxStore store = new InMemoryInboxStore();
        ErpShipmentInboxService service = new ErpShipmentInboxService(store);
        ErpShipmentMessage message = message();
        AcceptErpShipmentDeliveryCommand command = command(message);

        AcceptErpShipmentDeliveryResult first = service.accept(command);
        AcceptErpShipmentDeliveryResult duplicate = service.accept(command);

        assertFalse(first.duplicate());
        assertTrue(duplicate.duplicate());
    }

    @Test
    void rejectsNullCommand() {
        ErpShipmentInboxService service = new ErpShipmentInboxService(new InMemoryInboxStore());

        assertThrows(NullPointerException.class, () -> service.accept(null));
    }

    private static ErpShipmentMessage message() {
        return ErpShipmentMessage.create(
                "ERP-DEMO",
                "INBOX-UNIT-1001",
                "SHIP-INBOX-UNIT-1001",
                ErpShipmentOperation.UPSERT,
                Instant.parse("2026-08-17T20:20:00Z"),
                "CORR-INBOX-UNIT-1001");
    }

    private static AcceptErpShipmentDeliveryCommand command(ErpShipmentMessage message) {
        return new AcceptErpShipmentDeliveryCommand(
                message,
                "{\"messageId\":\"" + message.id().value() + "\"}",
                "ERP_SHIPMENT",
                "SHIPMENT",
                message.shipmentReference(),
                Instant.parse("2026-08-17T20:21:00Z"));
    }

    private static final class InMemoryInboxStore implements IntegrationInboxStore {
        private final Map<String, AcceptErpShipmentDeliveryResult> accepted = new HashMap<>();

        @Override
        public AcceptErpShipmentDeliveryResult accept(AcceptErpShipmentDeliveryCommand command) {
            String key = command.message().idempotencyKey().value();
            AcceptErpShipmentDeliveryResult existing = accepted.get(key);
            if (existing != null) {
                return new AcceptErpShipmentDeliveryResult(
                        existing.messageId(),
                        existing.idempotencyKey(),
                        true);
            }
            AcceptErpShipmentDeliveryResult created = new AcceptErpShipmentDeliveryResult(
                    command.message().id(),
                    command.message().idempotencyKey(),
                    false);
            accepted.put(key, created);
            return created;
        }
    }
}
