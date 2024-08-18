package org.browser.automation.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

import com.sun.tools.javac.Main;
import org.browser.automation.core.exception.PackageNotFoundException;
import org.browser.automation.core.exception.WebdriverNotFoundException;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@code Browser} class is responsible for managing browser configuration and retrieving
 * the necessary WebDriver information based on the current operating system.
 *
 * <p>This class dynamically identifies the browser configurations from the application configuration
 * and retrieves the corresponding WebDriver class to automate the browser.
 *
 * <p>The primary use cases of this class include:
 * <ul>
 *   <li>Retrieving the default browser configuration based on the operating system.</li>
 *   <li>Dynamically instantiating the appropriate WebDriver based on the configuration.</li>
 * </ul>
 *
 * <p>The class leverages Jackson annotations to allow serialization and deserialization of the
 * {@code BrowserInfo} record, making it compatible with JSON formats for further processing or storage.
 */
@Slf4j
public class Browser {

    private final ConfigurationProvider configProvider;

    public Browser() {
        this(new ConfigurationProvider());
    }

    /**
     * Constructs the {@code Browser} instance with the given configuration provider.
     *
     * @param configProvider the configuration provider that supplies the application configuration
     */
    public Browser(ConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    /**
     * A record that holds browser-related information such as the browser name, executable path,
     * and the associated WebDriver class. This record is used to encapsulate the essential browser
     * details required for automation.
     */
    @Builder
    public record BrowserInfo(
            @JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("driverClass") Class<? extends WebDriver> driverClass
    ) {
        /**
         * Custom constructor for the {@code BrowserInfo} record that enables JSON deserialization using Jackson.
         *
         * @param name        the name of the browser (e.g., "Chrome", "Firefox")
         * @param path        the path to the browser executable
         * @param driverClass the WebDriver class associated with the browser
         */
        @JsonCreator
        public BrowserInfo {
            // The @JsonCreator and @JsonProperty annotations enable Jackson to correctly deserialize the record
        }
    }

    /**
     * Retrieves the default browser information based on the current operating system.
     * The method reads browser configurations from the application configuration and
     * determines the most appropriate browser based on the available settings and the OS.
     *
     * <p>The method filters browser configurations based on the existence of the browser executable,
     * then attempts to resolve the WebDriver class dynamically. If a valid configuration is found,
     * it returns an {@code Optional} containing the corresponding {@code BrowserInfo}.</p>
     *
     * @return An {@code Optional} containing the {@code BrowserInfo} if a matching browser configuration is found,
     * or an empty {@code Optional} if no suitable configuration is detected.
     * @throws ConfigException.Missing If the configuration for the detected OS is missing.
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo() {
        String osKey = OSUtils.getOSKey(); // Get the OS key using the utility class
        Config config = configProvider.getConfig();

        try {
            // Retrieve the list of browser configurations for the detected OS
            List<? extends Config> browserConfigs = config.getConfigList("osBrowserPaths." + osKey);

            return browserConfigs.stream()
                    .filter(browser -> Files.exists(Paths.get(browser.getString("path"))))
                    .flatMap(browser -> getBrowserInfo(browser).stream()) // Directly flatten Optional<BrowserInfo>
                    .findFirst();
        } catch (ConfigException.Missing e) {
            log.error("Configuration for OS {} is missing.", osKey, e);
        }

        // Return an empty Optional if no matching browser configuration is found
        return Optional.empty();
    }

    /**
     * Resolves the {@code BrowserInfo} based on the provided browser configuration.
     * This method dynamically loads the WebDriver class specified in the configuration and
     * creates a {@code BrowserInfo} object containing the browser's name, path, and WebDriver class.
     *
     * <p>The method uses {@code @SneakyThrows} to avoid handling {@code ClassNotFoundException}
     * explicitly, as this exception is not expected to occur during regular use.</p>
     *
     * @param browser The {@code Config} object representing the browser configuration.
     * @return An {@code Optional} containing the {@code BrowserInfo} if the WebDriver class can be resolved.
     */
    @SneakyThrows
    private Optional<BrowserInfo> getBrowserInfo(Config browser) {
        String path = browser.getString("path");
        String driverClassName = browser.getString("driverClass");
        Class<? extends WebDriver> driverClass = getDriverClass(driverClassName);

        return Optional.of(new BrowserInfo(
                browser.getString("name"),
                path,
                driverClass
        ));
    }

    /**
     * Resolves and retrieves a WebDriver class based on the provided class name.
     * The method handles both fully qualified class names (e.g., "org.openqa.selenium.safari.SafariDriver")
     * and package paths (e.g., "org.openqa.selenium.safari"). If the input is a fully qualified class name
     * and no matching class is found, the method throws a {@link WebdriverNotFoundException}. If the input
     * is a package path and no WebDriver classes are found within that package, a {@link PackageNotFoundException} is thrown.
     *
     * <p>The method uses logging to provide detailed information about the processing steps,
     * making it easier to track decisions and potential issues during execution.</p>
     *
     * @param driverClassName The class name or package path for the WebDriver class to be resolved.
     * @return The resolved WebDriver class if a match is found.
     */
    @SneakyThrows
    protected static Class<? extends WebDriver> getDriverClass(String driverClassName) {
        log.info("Resolving WebDriver class for: {}", driverClassName);

        String packagePath = Arrays.stream(driverClassName.split("\\."))
                .reduce((acc, part) -> {
                    String currentPath = acc.isEmpty() ? part : acc + "." + part;
                    return !new Reflections(currentPath, Scanners.SubTypes).getSubTypesOf(WebDriver.class).isEmpty() ? currentPath : acc;
                }).orElse(null);

        Set<Class<? extends WebDriver>> subTypes = new Reflections(packagePath, Scanners.SubTypes).getSubTypesOf(WebDriver.class);

        return subTypes.stream()
                .filter(clazz -> clazz.getName().equals(driverClassName))
                .findFirst()
                .or(() -> subTypes.stream().findFirst())
                .orElseThrow(() -> driverClassName.contains(".")
                        ? new WebdriverNotFoundException("The class '" + driverClassName + "' does not match any available WebDriver class. Please check the class path and ensure the class exists.")
                        : new PackageNotFoundException("No WebDriver class found in the package path: '" + packagePath + "'."));
    }

    /**
     * Instantiates the WebDriver based on the provided driver class.
     * The method dynamically creates an instance of the WebDriver using reflection.
     *
     * @param driverClass the {@code Class} of the WebDriver to instantiate
     * @return an instance of the specified WebDriver
     * @throws ReflectiveOperationException if there is an error during instantiation (e.g., missing default constructor)
     */
    protected WebDriver instantiateDriver(Class<? extends WebDriver> driverClass) throws ReflectiveOperationException {
        return driverClass.getDeclaredConstructor().newInstance();
    }
}