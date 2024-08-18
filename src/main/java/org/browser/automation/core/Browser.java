package org.browser.automation.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * The method looks up browser paths and driver classes from the configuration file.
     *
     * @return an {@code Optional} containing the {@code BrowserInfo} if a matching configuration is found,
     * otherwise an empty {@code Optional}
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo() {
        String osKey = OSUtils.getOSKey(); // Get the OS key using the utility class
        Config config = configProvider.getConfig();

        try {
            // Retrieve the list of browser configurations for the detected OS
            List<? extends Config> browserConfigs = config.getConfigList("osBrowserPaths." + osKey);

            // Iterate over the list of browser configurations
            for (Config browser : browserConfigs) {
                String path = browser.getString("path");
                String driverClassName = browser.getString("driverClass");

                // Check if the browser executable exists at the specified path
                if (Files.exists(Paths.get(path))) {
                    try {
                        // Get the WebDriver class using the driver class name
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

        // Return an empty Optional if no matching browser configuration is found
        return Optional.empty();
    }

    /**
     * Retrieves the WebDriver class based on its fully qualified class name.
     * The method uses the Reflections library to dynamically locate the class within the specified package.
     *
     * @param driverClassName the fully qualified name of the WebDriver class
     * @return the {@code Class} object corresponding to the WebDriver class
     * @throws ClassNotFoundException if the class cannot be found in the specified package
     */
    protected static Class<? extends WebDriver> getDriverClass(String driverClassName) throws ClassNotFoundException {
        // Perform a scan of all packages and sub-packages dynamically
        Reflections reflections = new Reflections(Scanners.SubTypes);

        // Dynamically locate the WebDriver class by its fully qualified name
        Set<Class<? extends WebDriver>> subTypes = reflections.getSubTypesOf(WebDriver.class);

        return subTypes.stream()
                .filter(clazz -> clazz.getName().equals(driverClassName))
                .findFirst()
                .orElseThrow(() -> new ClassNotFoundException("WebDriver class not found: " + driverClassName));
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