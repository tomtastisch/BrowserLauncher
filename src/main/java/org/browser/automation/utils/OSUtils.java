package org.browser.automation.utils;

import lombok.experimental.UtilityClass;

/**
 * Utility class for OS-related operations.
 */
@UtilityClass
public class OSUtils {

    /**
     * Retrieves the command to determine the default web browser based on the operating system.
     * This method provides different commands for Windows, MacOS, and Linux.
     *
     * <p><b>Supported Operating Systems:</b></p>
     * <ul>
     *     <li><b>Windows:</b> Uses the Registry to determine the default browser.</li>
     *     <li><b>MacOS:</b> Uses 'defaultbrowser' command, assuming it is installed.</li>
     *     <li><b>Linux:</b> Uses 'xdg-settings' to get the default web browser.</li>
     * </ul>
     *
     * <p><b>Installation Guide for MacOS:</b></p>
     * <ol>
     *     <li><b>Installing Homebrew:</b>
     *         <p>If Homebrew is not already installed on your Mac, open the Terminal and execute the following command:</p>
     *         <pre>/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"</pre>
     *         <p>This script will install Homebrew. Follow the on-screen instructions to complete the installation.</p>
     *     </li>
     *     <li><b>Installing 'defaultbrowser':</b>
     *         <p>Once Homebrew is installed, you can install 'defaultbrowser' by running:</p>
     *         <pre>brew install defaultbrowser</pre>
     *         <p>This command downloads and installs the 'defaultbrowser' tool on your Mac.</p>
     *     </li>
     * </ol>
     *
     * <p><b>Usage:</b></p>
     * <p>Execute the command returned by this method in a terminal or through a runtime process in your application to retrieve the default web browser setting.</p>
     *
     * <p><b>Notes on Security and Compatibility:</b></p>
     * <ul>
     *     <li><b>Security:</b> Be cautious when executing commands that involve system settings or installations. Always validate the commands if they are influenced by external input.</li>
     *     <li><b>Compatibility:</b> Ensure that the corresponding tools are available and supported on the operating system where this method is used.</li>
     * </ul>
     *
     * @return The system-specific command to find the default web browser, or null if the operating system is not recognized.
     */
    public String getDefaultBrowserCommand() {
        return switch (getOSKey()) {
            case "win" -> "REG QUERY HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\http\\UserChoice /v ProgId";
            case "mac" -> "sh -c defaultbrowser";
            case "nix" -> "sh -c 'xdg-settings get default-web-browser'";
            default -> null;
        };
    }

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