package com.ecommerce.product_services.util;

import java.util.regex.Pattern;


public class InputSanitizer {
    private static final Pattern REGEX_SPECIAL_CHARS = Pattern.compile("[.^$*+?()\\[\\]{}|\\\\]");

    private static final Pattern NOSQL_INJECTION_PATTERN = Pattern.compile(".*[{}$\\[\\]].*");

    public static String sanitizeForRegex(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String sanitized = input.trim();

        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        sanitized = REGEX_SPECIAL_CHARS.matcher(sanitized).replaceAll("\\\\$0");
        
        return sanitized;
    }

    public static boolean isValidInput(String input) {
        if (input == null) {
            return true;
        }

        if (NOSQL_INJECTION_PATTERN.matcher(input).matches()) {
            return false;
        }
        
        return true;
    }

    public static String sanitizeCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "";
        }
        
        String sanitized = category.trim();
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s&-]", "");

        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }

    public static String sanitizeProductName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        String sanitized = name.trim();

        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s'\".,!&()-]", "");

        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        return sanitized;
    }
}

