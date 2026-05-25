package com.miniuber.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("core-service", r -> r.path("/core/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://core-service"))
                .route("payment-service", r -> r.path("/payment/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://payment-service"))
                .route("trip-service", r -> r.path("/trip/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://trip-service"))
                .build();
    }

}
