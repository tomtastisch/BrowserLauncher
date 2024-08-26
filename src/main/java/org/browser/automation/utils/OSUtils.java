package org.browser.automation.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.SystemUtils;

import java.util.Map;

/**
 * Utility class for OS-related operations.
 */
@UtilityClass
public class OSUtils {
    /**
     * Stores the key representing the current operating system.
     * <p>
     * The value is determined based on the operating system where this code is running and is used for
     * configuration lookup within the application. The supported operating systems include:
     * </p>
     * <ul>
     *     <li><b>Windows:</b> The key is "win" if the system is detected as Windows.</li>
     *     <li><b>MacOS:</b> The key is "mac" if the system is detected as MacOS.</li>
     *     <li><b>Linux/Unix:</b> The key is "nix" for other Unix-like systems, including Linux.</li>
     * </ul>
     * <p>
     * The value is computed only once during class loading and is stored in this static final variable.
     * This ensures the operating system is identified just once, improving performance by avoiding repeated checks.
     * </p>
     * <p>
     * This variable is particularly useful for selecting the appropriate command for determining the default browser
     * based on the detected operating system.
     * </p>
     * <p><b>Note:</b> If an unsupported operating system is detected, the key defaults to "nix".</p>
     */
    public static final String OS_KEY = determineOSKey();

    static final Map<String, String> COMMAND_MAP = Map.of(
            "win", "REG QUERY HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\http\\UserChoice /v ProgId",
            "mac", "sh -c defaultbrowser",
            "nix", "sh -c 'xdg-settings get default-web-browser'"
    );

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
     * <p><b>Important:</b> If the operating system is not supported, this method returns {@code null}.</p>
     *
     * <p><b>Installation Guide for MacOS:</b></p>
     * <ol>
     *     <li><b>Installing Homebrew:</b>
     *         <p>If Homebrew is not already installed on your Mac, open the Terminal and execute the following command:</p>
     *         <pre>/bin/bash -c "$(curl -fsSL <a href="https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh">https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh</a>)"</pre>
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
     * @return The system-specific command to find the default web browser, or {@code null} if the operating system is not recognized.
     */
    public String getDefaultBrowserCommand() {
        return COMMAND_MAP.getOrDefault(OS_KEY, null);
    }

    /**
     * Determines the OS key based on the current operating system.
     * This method is only called once and the result is stored in a static variable.
     *
     * @return the OS key for configuration lookup
     */
    private String determineOSKey() {
        return SystemUtils.IS_OS_WINDOWS ? "win"
                : SystemUtils.IS_OS_MAC ? "mac"
                : "nix";
    }
}