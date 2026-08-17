package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.domain.ErpShipmentMessage;

public interface ErpShipmentPayloadDeserializer {

    ErpShipmentMessage deserialize(String payload);
}
