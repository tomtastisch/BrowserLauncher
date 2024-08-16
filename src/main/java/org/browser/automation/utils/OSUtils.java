package org.browser.automation.utils;

import lombok.experimental.UtilityClass;

/**
 * Utility class for OS-related operations.
 */
@UtilityClass
public class OSUtils {

    /**
     * Gets the OS key based on the current operating system.
     *
     * @return the OS key for configuration lookup
     */
    public String getOSKey() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "win" :
                os.contains("mac") ? "mac" : "nix";
    }
}