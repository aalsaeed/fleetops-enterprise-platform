package com.aalsaeed.fleetops.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FleetOpsJwtAuthoritiesConverterTest {

    private final FleetOpsJwtAuthoritiesConverter converter = new FleetOpsJwtAuthoritiesConverter();

    @Test
    void mapsSupportedTopLevelAndKeycloakRealmRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("security-user")
                .claim("roles", List.of("FLEETOPS_USER", "IGNORED_ROLE"))
                .claim("realm_access", Map.of(
                        "roles", List.of("fleetops_operator", "ROLE_FLEETOPS_ADMIN")))
                .build();

        assertEquals(
                Set.of(
                        FleetOpsAuthorities.USER,
                        FleetOpsAuthorities.OPERATOR,
                        FleetOpsAuthorities.ADMIN),
                authorities(converter.convert(jwt)));
    }

    @Test
    void ignoresUnrelatedIdentityProviderRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("security-user")
                .claim("roles", "offline_access account-management")
                .build();

        assertEquals(Set.of(), authorities(converter.convert(jwt)));
    }

    private static Set<String> authorities(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
