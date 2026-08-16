package com.aalsaeed.fleetops.trip.infrastructure.config;

import com.aalsaeed.fleetops.trip.application.port.out.DriverResourcePort;
import com.aalsaeed.fleetops.trip.application.port.out.TripRepository;
import com.aalsaeed.fleetops.trip.application.port.out.VehicleResourcePort;
import com.aalsaeed.fleetops.trip.application.service.TripApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TripApplicationConfiguration {

    @Bean
    TripApplicationService tripApplicationService(
            TripRepository tripRepository,
            DriverResourcePort driverResourcePort,
            VehicleResourcePort vehicleResourcePort) {
        return new TripApplicationService(tripRepository, driverResourcePort, vehicleResourcePort);
    }
}
