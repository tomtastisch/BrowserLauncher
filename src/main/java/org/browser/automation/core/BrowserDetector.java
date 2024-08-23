package org.browser.automation.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.browser.automation.core.exception.PackageNotFoundException;
import org.browser.automation.core.exception.WebdriverNotFoundException;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@code BrowserDetector} class is responsible for managing browser configuration and retrieving
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
public class BrowserDetector {

    private final String osKey;
    private final Config config;

    public BrowserDetector() {
        this(new ConfigurationProvider());
    }

    /**
     * Constructs the {@code BrowserDetector} instance with the given configuration provider.
     *
     * @param configProvider the configuration provider that supplies the application configuration
     */
    public BrowserDetector(ConfigurationProvider configProvider) {
        this.osKey = OSUtils.getOSKey();
        this.config = configProvider.getConfig();
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
     * Retrieves the system's default browser information without a fallback option.
     * This method attempts to determine the default browser configured on the operating system
     * and returns its corresponding {@code BrowserInfo} wrapped in an {@code Optional}.
     * If the default browser cannot be determined, no fallback is provided, and the method
     * returns an empty {@code Optional}.
     *
     * <p>This method internally delegates to {@link #getDefaultBrowserInfo(boolean)} with
     * the {@code fallbackBrowser} parameter set to {@code false}, meaning no fallback
     * browser is used if the default browser cannot be identified.</p>
     *
     * @return An {@code Optional<BrowserInfo>} containing the default browser information,
     *         or an empty {@code Optional} if no match is found and no fallback is used.
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo() {
        return getDefaultBrowserInfo(false);
    }

    /**
     * Retrieves the system's default browser information with an optional fallback.
     * This method attempts to determine the default browser configured on the operating system
     * and returns the corresponding {@code BrowserInfo} wrapped in an {@code Optional}.
     * If the default browser cannot be determined, the method can optionally fall back to
     * returning the first available browser from the list of installed browsers.
     *
     * <p>The method follows these steps:</p>
     * <ol>
     *   <li>It fetches a list of all installed browsers on the system using {@code getInstalledBrowsers()}.</li>
     *   <li>If the {@code fallbackBrowser} parameter is set to {@code true}, the method initializes
     *       the result with the first available browser as a fallback option. Otherwise, it starts with an empty {@code Optional}.</li>
     *   <li>The method executes a system command (retrieved from {@code OSUtils.getDefaultBrowserCommand()})
     *       to determine the name of the default browser. The output of this command is captured as a UTF-8 string.</li>
     *   <li>If the command executes successfully (exit code 0), the output is processed line by line. Each line
     *       is trimmed to remove extra spaces, and the method checks if any of these lines match the name of an installed browser.
     *       The matching is done using a case-insensitive comparison.</li>
     *   <li>If multiple matches are found, the method returns the last match, as determined by the reduction operation.</li>
     *   <li>If any exception occurs (e.g., {@code IOException}, {@code InterruptedException}, or {@code NullPointerException}),
     *       an error is logged, and the method continues with the fallback logic.</li>
     *   <li>The method ultimately returns either the matching browser or the first available browser, depending on
     *       whether the fallback option was enabled and whether a match was found.</li>
     * </ol>
     *
     * @param fallbackBrowser A boolean flag indicating whether to fall back to the first available browser
     *                        if the default browser cannot be determined.
     * @return An {@code Optional<BrowserInfo>} containing the default browser information, or the first
     *         available browser if no match is found and the fallback is enabled. Otherwise, an empty {@code Optional}.
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo(boolean fallbackBrowser) {

        List<BrowserInfo> browsers = this.getInstalledBrowsers();
        Optional<BrowserInfo> browser = fallbackBrowser ? browsers.stream().findFirst() : Optional.empty();

        try {
            Process process = Runtime.getRuntime().exec(OSUtils.getDefaultBrowserCommand());
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

            if (process.waitFor() == 0) {
                browser = output.lines().map(String::trim)
                        .flatMap(line -> browsers.stream().filter(b -> line.equalsIgnoreCase(b.name)))
                        .reduce((first, second) -> second);
            }
        } catch (IOException | InterruptedException | NullPointerException e) {
            log.error("Error while executing OS {} command.", OSUtils.getDefaultBrowserCommand(), e);
        }

        // Return an empty Optional if no matching browser configuration is found
        return browser;
    }

    /**
     * Retrieves a list of all installed browsers based on the current operating system.
     *
     * <p>This method reads browser configurations from the application configuration and
     * determines which browsers are installed by checking the existence of the executable paths.
     * It returns a list of {@code BrowserInfo} objects for each detected browser.</p>
     *
     * @return A list of {@code BrowserInfo} objects representing installed browsers.
     * If no browsers are detected or if the configuration is missing, an empty list is returned.
     */
    public List<BrowserInfo> getInstalledBrowsers() {
        try {
            return config.getConfigList("osBrowserPaths." + osKey).stream()
                    .filter(browser -> Files.exists(Paths.get(browser.getString("path"))))
                    .map(BrowserDetector::getBrowserInfo)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (ConfigException.Missing e) {
            log.error("Configuration for OS {} is missing.", osKey, e);
            return List.of();
        }
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
    static Optional<BrowserInfo> getBrowserInfo(Config browser) {
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