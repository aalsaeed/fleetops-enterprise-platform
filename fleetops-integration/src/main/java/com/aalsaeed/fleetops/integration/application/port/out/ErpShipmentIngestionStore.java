package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.application.port.in.StageErpShipmentResult;
import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

public interface ErpShipmentIngestionStore {

    StageErpShipmentResult stage(ErpShipmentMessage message, String payload);
}
