package com.aalsaeed.fleetops.driver.infrastructure.config;

import com.aalsaeed.fleetops.driver.application.port.out.DriverRepository;
import com.aalsaeed.fleetops.driver.application.service.DriverApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DriverApplicationConfiguration {

    @Bean
    DriverApplicationService driverApplicationService(DriverRepository driverRepository) {
        return new DriverApplicationService(driverRepository);
    }
}
