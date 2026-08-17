package com.aalsaeed.fleetops.integration.application.service;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryCommand;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryResult;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryUseCase;
import com.aalsaeed.fleetops.integration.application.port.out.IntegrationInboxStore;

import java.util.Objects;

public final class ErpShipmentInboxService implements AcceptErpShipmentDeliveryUseCase {

    private final IntegrationInboxStore inboxStore;

    public ErpShipmentInboxService(IntegrationInboxStore inboxStore) {
        this.inboxStore = Objects.requireNonNull(inboxStore, "Inbox store cannot be null");
    }

    @Override
    public AcceptErpShipmentDeliveryResult accept(AcceptErpShipmentDeliveryCommand command) {
        Objects.requireNonNull(command, "ERP shipment delivery command cannot be null");
        return inboxStore.accept(command);
    }
}
