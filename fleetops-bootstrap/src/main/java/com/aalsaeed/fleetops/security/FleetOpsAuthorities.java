package com.aalsaeed.fleetops.security;

import java.util.Set;

public final class FleetOpsAuthorities {

    public static final String USER = "FLEETOPS_USER";
    public static final String OPERATOR = "FLEETOPS_OPERATOR";
    public static final String ADMIN = "FLEETOPS_ADMIN";

    private static final Set<String> SUPPORTED = Set.of(USER, OPERATOR, ADMIN);

    private FleetOpsAuthorities() {
    }

    public static boolean isSupported(String authority) {
        return SUPPORTED.contains(authority);
    }
}
