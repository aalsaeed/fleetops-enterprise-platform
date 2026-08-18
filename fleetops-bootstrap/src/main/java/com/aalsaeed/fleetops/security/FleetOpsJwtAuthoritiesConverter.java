package com.aalsaeed.fleetops.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FleetOpsJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        collectRoles(roles, jwt.getClaim(ROLES_CLAIM));

        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            collectRoles(roles, realmAccessMap.get(ROLES_CLAIM));
        }

        return roles.stream()
                .map(FleetOpsJwtAuthoritiesConverter::normalizeRole)
                .filter(FleetOpsAuthorities::isSupported)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private static void collectRoles(Set<String> roles, Object claimValue) {
        if (claimValue instanceof Collection<?> values) {
            values.stream()
                    .filter(value -> value != null)
                    .map(Object::toString)
                    .forEach(roles::add);
            return;
        }

        if (claimValue instanceof String value) {
            for (String role : value.split("[,\\s]+")) {
                if (!role.isBlank()) {
                    roles.add(role);
                }
            }
        }
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }
}
