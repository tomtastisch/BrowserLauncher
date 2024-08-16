package org.browser.automation.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.utils.OSUtils;
import org.browser.config.BrowserConfig;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code Browser} class encapsulates the configuration and driver information for various browsers.
 * It retrieves browser paths and WebDriver classes based on the current operating system.
 *
 * <p>This class is responsible for managing supported browsers and providing default browser configurations.
 */
@Slf4j
public class Browser {

    /**
     * A record that holds browser-related information such as name, executable path, and driver class.
     */
    @Builder
    public record BrowserInfo(String name, String path, Class<? extends WebDriver> driverClass) {
    }

    private static final Config CONFIG = BrowserConfig.getConfig();

    /**
     * Retrieves the default browser info based on the current operating system.
     * The method looks up browser paths and driver classes from the configuration file.
     *
     * @return an {@code Optional} containing the {@code BrowserInfo} if a match is found, otherwise an empty {@code Optional}
     */
    public static Optional<BrowserInfo> getDefaultBrowserInfo() {
        String osKey = OSUtils.getOSKey(); // Get the OS key using the utility class

        try {
            List<? extends Config> browserConfigs = CONFIG.getConfigList("osBrowserPaths." + osKey);

            for (Config browser : browserConfigs) {
                String path = browser.getString("path");
                String driverClassName = browser.getString("driverClass");

                if (Files.exists(Paths.get(path))) {
                    try {
                        Class<? extends WebDriver> driverClass = getDriverClass(driverClassName);
                        return Optional.of(new BrowserInfo(
                                browser.getString("name"),
                                path,
                                driverClass
                        ));
                    } catch (ClassNotFoundException e) {
                        log.error("Driver class not found: {}", driverClassName, e);
                    }
                }
            }
        } catch (ConfigException.Missing e) {
            log.error("Configuration for OS {} is missing.", osKey, e);
        }

        return Optional.empty();
    }

    /**
     * Retrieves the {@code WebDriver} class based on its fully qualified class name.
     * The method scans all subclasses of {@code WebDriver} within the "org.openqa.selenium" package.
     *
     * @param driverClassName the fully qualified name of the driver class
     * @return the {@code WebDriver} class
     * @throws ClassNotFoundException if the class cannot be found in the specified package
     */
    protected static Class<? extends WebDriver> getDriverClass(String driverClassName) throws ClassNotFoundException {
        Reflections reflections = new Reflections("org.openqa.selenium");
        Set<Class<? extends WebDriver>> subTypes = reflections.getSubTypesOf(WebDriver.class);

        for (Class<? extends WebDriver> subType : subTypes) {
            if (subType.getName().equals(driverClassName)) {
                return subType;
            }
        }

        throw new ClassNotFoundException("WebDriver class not found: " + driverClassName);
    }
}