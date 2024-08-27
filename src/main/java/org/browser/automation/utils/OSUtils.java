package org.browser.automation.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.browser.config.ConfigurationProvider;

import java.util.Objects;

/**
 * The {@code OSUtils} class provides utility methods related to operating system detection and
 * command retrieval based on the current operating system. It is designed to be a utility class
 * with static methods that do not require instantiation, leveraging the {@code @UtilityClass} annotation
 * from Lombok.
 *
 * <p>This class is primarily focused on determining the current operating system and retrieving
 * the appropriate command for fetching the default web browser configuration. It supports multiple
 * platforms, including Windows, macOS, and Linux/Unix.</p>
 *
 * <h2>Key Functionalities</h2>
 * <ul>
 *     <li>Determining the operating system and returning a corresponding key.</li>
 *     <li>Loading and caching commands for fetching the default browser setting.</li>
 *     <li>Providing detailed usage guidance for macOS, including setup instructions for necessary tools.</li>
 * </ul>
 *
 * <h2>Configuration Structure</h2>
 * The class expects configuration values to be stored in a structured format, typically in a file
 * named {@code commands.conf}, loaded via {@link ConfigurationProvider}. The structure should resemble the following:
 *
 * <pre>
 * commands {
 *     ...
 * }
 * </pre>
 * <p>
 * The configuration keys ("win", "mac", "nix") are derived based on the detected operating system.
 *
 * <p><b>Note:</b> If an unsupported operating system is detected, the key defaults to "nix".</p>
 */
@UtilityClass
public class OSUtils {

    /**
     * The name of the configuration file section where commands are stored.
     */
    final String CONFIG_NAME = "commands";

    /**
     * Stores the key representing the current operating system.
     * <p>
     * The value is determined based on the operating system where this code is running and is used for
     * configuration lookup within the application. The supported operating systems include:
     * </p>
     * <ul>
     *     <li><b>Windows:</b> The key is "win" if the system is detected as Windows.</li>
     *     <li><b>MacOS:</b> The key is "mac" if the system is detected as macOS.</li>
     *     <li><b>Linux/Unix:</b> The key is "nix" for other Unix-like systems, including Linux.</li>
     * </ul>
     * <p>
     * The value is computed only once during class loading and is stored in this static final variable.
     * This ensures the operating system is identified just once, improving performance by avoiding repeated checks.
     * </p>
     */
    public static final String OS_KEY = determineOSKey();

    /**
     * Caches the command retrieved from the configuration file to avoid redundant lookups.
     */
    private String cachedCommand;

    /**
     * Retrieves the command to determine the default web browser based on the operating system.
     * This method provides different commands for Windows, macOS, and Linux.
     *
     * <h3>Supported Operating Systems</h3>
     * <ul>
     *     <li><b>Windows:</b> Uses the Registry to determine the default browser.</li>
     *     <li><b>macOS:</b> Uses 'defaultbrowser' command, assuming it is installed.</li>
     *     <li><b>Linux:</b> Uses 'xdg-settings' to get the default web browser.</li>
     * </ul>
     *
     * <h3>MacOS Setup Instructions</h3>
     * <ol>
     *     <li>
     *         <b>Install Homebrew:</b>
     *         <p>If Homebrew is not already installed, run the following command in the Terminal:</p>
     *         <pre>/bin/bash -c "$(curl -fsSL <a href="https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh">https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh</a>)"</pre>
     *     </li>
     *     <li>
     *         <b>Install 'defaultbrowser':</b>
     *         <p>After installing Homebrew, run the following command:</p>
     *         <pre>brew install defaultbrowser</pre>
     *         <p>This command installs the 'defaultbrowser' tool used by this utility.</p>
     *     </li>
     * </ol>
     *
     * <h3>Usage</h3>
     * Execute the command returned by this method in a terminal or through a runtime process in your application
     * to retrieve the default web browser setting.
     *
     * <h3>Security and Compatibility Notes</h3>
     * <ul>
     *     <li><b>Security:</b> Be cautious when executing commands that involve system settings or installations.
     *     Always validate the commands if they are influenced by external input.</li>
     *     <li><b>Compatibility:</b> Ensure that the corresponding tools are available and supported on the operating
     *     system where this method is used.</li>
     * </ul>
     *
     * @return The system-specific command to find the default web browser, or {@code null} if the operating system is not recognized.
     */
    public String getDefaultBrowserCommand() {
        // Lazy initialization: only load the command once and cache it
        if (Objects.isNull(cachedCommand)) {
            final String os = determineOSKey();
            cachedCommand = ConfigurationProvider.getInstance(CONFIG_NAME)
                    .getConfig().getString(StringUtils.join(new String[]{CONFIG_NAME, os}, "."));
        }
        return cachedCommand;
    }

    /**
     * Determines the OS key based on the current operating system.
     * This method is only called once and the result is stored in a static variable.
     *
     * @return the OS key for configuration lookup ("win", "mac", "nix")
     */
    private String determineOSKey() {
        return SystemUtils.IS_OS_WINDOWS ? "win"
                : SystemUtils.IS_OS_MAC ? "mac"
                : "nix";
    }
}