package com.populace.navigation.dto;

public record NavItemDto(
    String path,
    String label,
    int displayOrder
) {
    public static NavItemDto of(String path, String label, int displayOrder) {
        return new NavItemDto(path, label, displayOrder);
    }
}
