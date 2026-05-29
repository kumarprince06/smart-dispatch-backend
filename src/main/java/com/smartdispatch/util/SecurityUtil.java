package com.smartdispatch.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.
        SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
        // Utility class — prevent instantiation
    }

    public static String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        return authentication.getName();
    }
}
