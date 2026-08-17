package com.aalsaeed.fleetops.integration.application.port.out;

import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryCommand;
import com.aalsaeed.fleetops.integration.application.port.in.AcceptErpShipmentDeliveryResult;

public interface IntegrationInboxStore {

    AcceptErpShipmentDeliveryResult accept(AcceptErpShipmentDeliveryCommand command);
}
