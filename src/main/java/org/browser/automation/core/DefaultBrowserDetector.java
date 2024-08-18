package org.browser.automation.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.utils.OSUtils;
import org.browser.config.BrowserConfig;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * The {@code DefaultBrowserDetector} class is responsible for detecting the default browser
 * based on the operating system and configuration file.
 *
 * <p>This class uses the Typesafe Config library to read browser configurations from an external
 * configuration file. It identifies the browser paths and WebDriver classes based on the detected
 * operating system (e.g., Windows, macOS, Linux).
 *
 * <p>The class is designed as a Singleton to ensure only one instance is used throughout the application.
 *
 * <p>Example usage:
 * <pre>
 *     DefaultBrowserDetector detector = DefaultBrowserDetector.getInstance();
 *     Optional&lt;Browser.BrowserInfo&gt; browserInfo = detector.getDefaultBrowserInfo();
 *     if (browserInfo.isPresent()) {
 *         // Use browserInfo for further processing
 *     }
 * </pre>
 */
@Slf4j
@NoArgsConstructor
public class DefaultBrowserDetector {

    private static final Config CONFIG = BrowserConfig.getConfig();

    /**
     * Inner static class responsible for holding the Singleton instance of {@code DefaultBrowserDetector}.
     * The instance is lazily loaded when the class is first accessed.
     */
    private static class SingletonHelper {
        private static final DefaultBrowserDetector INSTANCE = new DefaultBrowserDetector();
    }

    /**
     * Returns the singleton instance of {@code DefaultBrowserDetector}.
     *
     * @return the singleton instance of {@code DefaultBrowserDetector}
     */
    public static DefaultBrowserDetector getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Retrieves the default browser information based on the operating system.
     *
     * <p>This method fetches the list of available browsers for the detected OS from the configuration file.
     * It then checks if the browser executable exists on the file system, and if it does, it returns the
     * browser information, including its name, path, and WebDriver class.
     *
     * @return an {@code Optional} containing the {@code Browser.BrowserInfo} if a matching browser is found,
     * otherwise an empty {@code Optional}.
     */
    public Optional<Browser.BrowserInfo> getDefaultBrowserInfo() {
        String osKey = OSUtils.getOSKey(); // Detect the operating system key (e.g., "win", "mac", "nix")
        try {
            // Retrieve the list of browser configurations for the detected OS
            List<? extends Config> browserConfigs = CONFIG.getConfigList("osBrowserPaths." + osKey);

            // Iterate over the list of browser configurations
            for (Config browser : browserConfigs) {
                String path = browser.getString("path");
                if (Files.exists(Paths.get(path))) { // Check if the browser executable exists
                    try {
                        String driverClassName = browser.getString("driverClass");
                        // Get the WebDriver class using the driver class name
                        Class<? extends WebDriver> driverClass = Browser.getDriverClass(driverClassName);
                        return Optional.of(new Browser.BrowserInfo(
                                browser.getString("name"),
                                path,
                                driverClass
                        ));
                    } catch (Exception e) {
                        log.error("Driver class not found: {}", browser.getString("driverClass"), e);
                    }
                }
            }
        } catch (ConfigException.Missing e) {
            log.error("Configuration for OS {} is missing.", osKey, e);
        }

        // Return an empty Optional if no matching browser configuration is found
        return Optional.empty();
    }
}