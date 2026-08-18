package com.aalsaeed.fleetops.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "fleetops.security.test-permit-all",
        havingValue = "false",
        matchIfMissing = true)
public class SecurityConfiguration {

    @Bean
    FleetOpsJwtAuthoritiesConverter fleetOpsJwtAuthoritiesConverter() {
        return new FleetOpsJwtAuthoritiesConverter();
    }

    @Bean
    JwtAuthenticationConverter fleetOpsJwtAuthenticationConverter(
            FleetOpsJwtAuthoritiesConverter authoritiesConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    SecurityFilterChain fleetOpsSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            JsonMapper jsonMapper) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/integration/operations/**")
                        .hasAuthority(FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/integration/operations")
                        .hasAnyAuthority(FleetOpsAuthorities.OPERATOR, FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**")
                        .hasAnyAuthority(
                                FleetOpsAuthorities.USER,
                                FleetOpsAuthorities.OPERATOR,
                                FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/**")
                        .hasAnyAuthority(FleetOpsAuthorities.OPERATOR, FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                        .hasAnyAuthority(FleetOpsAuthorities.OPERATOR, FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                        .hasAnyAuthority(FleetOpsAuthorities.OPERATOR, FleetOpsAuthorities.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                        .hasAuthority(FleetOpsAuthorities.ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            writeProblem(
                                    response,
                                    jsonMapper,
                                    HttpStatus.UNAUTHORIZED,
                                    "Authentication required",
                                    "A valid bearer token is required to access this resource.",
                                    "AUTHENTICATION_REQUIRED");
                        }))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            writeProblem(
                                    response,
                                    jsonMapper,
                                    HttpStatus.UNAUTHORIZED,
                                    "Authentication required",
                                    "A valid bearer token is required to access this resource.",
                                    "AUTHENTICATION_REQUIRED");
                        })
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                response,
                                jsonMapper,
                                HttpStatus.FORBIDDEN,
                                "Access denied",
                                "The authenticated principal does not have sufficient authority for this operation.",
                                "ACCESS_DENIED")))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults());

        return http.build();
    }

    private static void writeProblem(
            HttpServletResponse response,
            JsonMapper jsonMapper,
            HttpStatus status,
            String title,
            String detail,
            String code) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
