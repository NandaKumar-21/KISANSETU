package com.kisansetu.security;

public enum Role {
    FARMER,
    MERCHANT,
    CUSTOMER,
    LOGISTICS;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase()) {
            case "farmer" -> FARMER;
            case "merchant" -> MERCHANT;
            case "customer" -> CUSTOMER;
            case "logistics" -> LOGISTICS;
            default -> null;
        };
    }
}