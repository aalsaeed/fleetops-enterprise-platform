package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

public interface ErpShipmentPayloadSerializer {

    String serialize(ErpShipmentMessage message);
}
