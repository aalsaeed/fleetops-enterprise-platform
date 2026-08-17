package com.aalsaeed.fleetops.integration.application.port.in;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

public interface StageErpShipmentUseCase {

    StageErpShipmentResult stage(ErpShipmentMessage message);
}
