package com.example.search_microservice.util;

import java.util.regex.Pattern;

/**
 * Utility class to sanitize user input and prevent NoSQL injection attacks
 */
public class InputSanitizer {

    // Regex special characters that need to be escaped
    private static final Pattern REGEX_SPECIAL_CHARS = Pattern.compile("[.^$*+?()\\[\\]{}|\\\\]");
    
    // Pattern to detect potential NoSQL injection attempts
    private static final Pattern NOSQL_INJECTION_PATTERN = Pattern.compile(".*[{}$\\[\\]].*");
    
    /**
     * Sanitize input for use in Elasticsearch queries
     * Escapes all regex special characters to prevent injection
     * 
     * @param input The user input to sanitize
     * @return Sanitized string safe for queries
     */
    public static String sanitizeForSearch(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Trim whitespace
        String sanitized = input.trim();
        
        // Limit length to prevent DoS attacks
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        // Escape all regex special characters
        sanitized = REGEX_SPECIAL_CHARS.matcher(sanitized).replaceAll("\\\\$0");
        
        return sanitized;
    }
    
    /**
     * Validate input to detect potential NoSQL injection attempts
     * 
     * @param input The user input to validate
     * @return true if input is safe, false if it contains suspicious patterns
     */
    public static boolean isValidInput(String input) {
        if (input == null) {
            return true;
        }
        
        // Check for NoSQL injection patterns
        if (NOSQL_INJECTION_PATTERN.matcher(input).matches()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Sanitize category input
     * Only allows alphanumeric characters, spaces, hyphens, and ampersands
     * 
     * @param category The category to sanitize
     * @return Sanitized category string
     */
    public static String sanitizeCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "";
        }
        
        String sanitized = category.trim();
        
        // Remove any characters that aren't alphanumeric, space, hyphen, or ampersand
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s&-]", "");
        
        // Limit length
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize product name input
     * Allows alphanumeric characters, spaces, and common punctuation
     * 
     * @param name The product name to sanitize
     * @return Sanitized name string
     */
    public static String sanitizeProductName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        String sanitized = name.trim();
        
        // Remove any characters that aren't alphanumeric, space, or common punctuation
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s'\".,!&()-]", "");
        
        // Limit length
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        return sanitized;
    }
}

