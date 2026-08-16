package com.aalsaeed.fleetops.vehicle.infrastructure.config;

import com.aalsaeed.fleetops.vehicle.application.port.out.VehicleRepository;
import com.aalsaeed.fleetops.vehicle.application.service.VehicleApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VehicleApplicationConfiguration {

    @Bean
    VehicleApplicationService vehicleApplicationService(VehicleRepository vehicleRepository) {
        return new VehicleApplicationService(vehicleRepository);
    }
}
