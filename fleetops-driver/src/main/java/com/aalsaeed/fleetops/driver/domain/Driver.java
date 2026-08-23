package com.aalsaeed.fleetops.driver.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Driver {

    private static final Pattern E164_PHONE = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final DriverId id;
    private final String externalReference;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private DriverStatus status;
    private final Long revision;

    private Driver(
            DriverId id,
            String externalReference,
            String firstName,
            String lastName,
            String phoneNumber,
            DriverStatus status,
            Long revision) {
        this.id = Objects.requireNonNull(id, "Driver ID cannot be null");
        this.externalReference = requireText(externalReference, "External reference");
        this.firstName = requireText(firstName, "First name");
        this.lastName = requireText(lastName, "Last name");
        this.phoneNumber = requirePhone(phoneNumber);
        this.status = Objects.requireNonNull(status, "Driver status cannot be null");
        this.revision = requireRevision(revision);
    }

    public static Driver create(
            String externalReference,
            String firstName,
            String lastName,
            String phoneNumber) {
        return new Driver(
                DriverId.newId(),
                externalReference,
                firstName,
                lastName,
                phoneNumber,
                DriverStatus.ACTIVE,
                null);
    }

    public static Driver restore(
            DriverId id,
            String externalReference,
            String firstName,
            String lastName,
            String phoneNumber,
            DriverStatus status) {
        return restore(id, externalReference, firstName, lastName, phoneNumber, status, null);
    }

    public static Driver restore(
            DriverId id,
            String externalReference,
            String firstName,
            String lastName,
            String phoneNumber,
            DriverStatus status,
            Long revision) {
        return new Driver(id, externalReference, firstName, lastName, phoneNumber, status, revision);
    }

    public void changeStatus(DriverStatus target) {
        Objects.requireNonNull(target, "Target driver status cannot be null");
        if (!status.canTransitionTo(target)) {
            throw new InvalidDriverStatusTransitionException(status, target);
        }
        status = target;
    }

    public DriverId id() {
        return id;
    }

    public String externalReference() {
        return externalReference;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public DriverStatus status() {
        return status;
    }

    public Long revision() {
        return revision;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String requirePhone(String value) {
        String phone = requireText(value, "Phone number");
        if (!E164_PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number must use E.164 format");
        }
        return phone;
    }

    private static Long requireRevision(Long revision) {
        if (revision != null && revision < 0) {
            throw new IllegalArgumentException("Driver revision cannot be negative");
        }
        return revision;
    }
}
